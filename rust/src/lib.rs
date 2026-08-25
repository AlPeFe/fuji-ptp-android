//! JNI bridge for the Android application.
//!
//! This crate is a thin transport layer. It contains NO protocol logic and
//! NO hand-written DTOs: the whole PTP/Fujifilm protocol and the JSON
//! serialization of the recipe domain model live in `fuji-ptp-core` (feature
//! `serde`). This crate only:
//!
//! 1. Implements `fuji_ptp_core::transport::Transport` on top of a Kotlin
//!    `UsbIo` object (bulk IN/OUT callbacks through JNI).
//! 2. Exposes a small, high-level JNI facade to Kotlin that calls
//!    `FujiPtp` and passes JSON strings across the boundary.
//!
//! ```text
//! Kotlin UsbIoBridge (owns UsbDeviceConnection)
//!         │  JNI callbacks: send(byte[]) / receive(int)
//!         ▼
//! AndroidTransport (this crate)  implements fuji_ptp_core::transport::Transport
//!         ▼
//! fuji-ptp-core::FujiPtp  (protocol + serde DTOs, feature "serde")
//! ```

use std::ffi::c_void;
use std::sync::{Mutex, OnceLock};

use fuji_ptp_core::ptp::FujiPtp;
use fuji_ptp_core::recipe::Profile;
use fuji_ptp_core::transport::{Transport, TransportError};
use jni::objects::{GlobalRef, JByteArray, JObject, JString, JValue, JValueOwned};
use jni::sys::{JNI_VERSION_1_6, jint, jstring};
use jni::{JNIEnv, JavaVM};

// ---------------------------------------------------------------------------
// Global state
// ---------------------------------------------------------------------------

/// The `JavaVM`, captured at `JNI_OnLoad`. `JavaVM` is `Send + Sync` (a thin
/// wrapper over the VM pointer, which outlives the library), so a `&'static`
/// reference can be handed to the transport.
static VM: OnceLock<JavaVM> = OnceLock::new();

/// The opaque controller holding the live `FujiPtp` session, if connected.
static CONTROLLER: Mutex<Option<Controller>> = Mutex::new(None);

struct Controller {
    fuji: FujiPtp<AndroidTransport>,
}

/// Replaces the current controller, returning the previous one (dropped
/// after this function returns). Used to reset the session state cleanly
/// without holding the lock during teardown.
fn replace_controller(new: Controller) -> Option<Controller> {
    let mut guard = lock_controller();
    std::mem::replace(&mut *guard, Some(new))
}

fn lock_controller() -> std::sync::MutexGuard<'static, Option<Controller>> {
    CONTROLLER
        .lock()
        .unwrap_or_else(|poisoned| poisoned.into_inner())
}

// ---------------------------------------------------------------------------
// Android transport: calls back into Kotlin for raw USB bulk I/O
// ---------------------------------------------------------------------------

/// `Transport` implementation that performs bulk I/O through a Kotlin
/// `UsbIoBridge` global reference. The receive loop mirrors the proven
/// desktop client: read the 12-byte container header, then read the declared
/// payload length in one or more chunks.
struct AndroidTransport {
    bridge: GlobalRef,
    vm: &'static JavaVM,
}

impl AndroidTransport {
    fn call_receive(&self, size: i32) -> Result<Vec<u8>, TransportError> {
        let mut env = self
            .vm
            .attach_current_thread()
            .map_err(|_| TransportError::ReceiveError)?;
        let result: JValueOwned<'_> = env
            .call_method(&self.bridge, "receive", "(I)[B", &[JValue::Int(size)])
            .map_err(|_| TransportError::ReceiveError)?;
        let object = result.l().map_err(|_| TransportError::ReceiveError)?;
        let array = JByteArray::from(object);
        env.convert_byte_array(&array)
            .map_err(|_| TransportError::ReceiveError)
    }

    fn call_send(&self, data: &[u8]) -> Result<(), TransportError> {
        let mut env = self
            .vm
            .attach_current_thread()
            .map_err(|_| TransportError::SendError)?;
        let array: JByteArray<'_> = env
            .byte_array_from_slice(data)
            .map_err(|_| TransportError::SendError)?;
        let object = JObject::from(array);
        let result = env
            .call_method(&self.bridge, "send", "([B)I", &[JValue::Object(&object)])
            .map_err(|_| TransportError::SendError)?;
        match result.i() {
            Ok(written) if written >= 0 => Ok(()),
            _ => Err(TransportError::SendError),
        }
    }
}

impl Transport for AndroidTransport {
    fn send(&mut self, data: &[u8]) -> Result<(), TransportError> {
        self.call_send(data)
    }

    fn receive(&mut self) -> Result<Vec<u8>, TransportError> {
        // Read the first bulk packet (up to the USB max packet size, 512).
        // Android's bulkTransfer drops any bytes beyond the requested
        // length, so we always request a full packet and let the parser
        // consume the container header from whatever came back.
        const MAX_BULK_PACKET: usize = 512;
        let first = self.call_receive(MAX_BULK_PACKET as i32)?;
        if first.len() < 12 {
            return Err(TransportError::ReceiveError);
        }
        let length = u32::from_le_bytes(first[0..4].try_into().unwrap()) as usize;
        if !(12..=1024 * 1024).contains(&length) {
            return Err(TransportError::ReceiveError);
        }
        let mut packet = first;
        let first_len = packet.len();
        packet.resize(length, 0);
        let mut offset = first_len;
        while offset < length {
            let chunk = self.call_receive(MAX_BULK_PACKET as i32)?;
            if chunk.is_empty() {
                return Err(TransportError::ReceiveError);
            }
            packet[offset..offset + chunk.len()].copy_from_slice(&chunk);
            offset += chunk.len();
        }
        Ok(packet)
    }
}

// ---------------------------------------------------------------------------
// JNI helpers
// ---------------------------------------------------------------------------

fn ok_json() -> String {
    "{\"ok\":true}".to_string()
}

fn err_json(message: impl AsRef<str>) -> String {
    format!(
        "{{\"ok\":false,\"error\":{}}}",
        serde_json::to_string(message.as_ref()).unwrap_or_else(|_| "\"unknown\"".into())
    )
}

/// Clear any pending Java exception left by a transport callback, so the
/// exception never surfaces in Kotlin as an unexpected throw.
fn clear_pending_exception(env: &JNIEnv<'_>) {
    let _ = env.exception_clear();
}

unsafe fn json_param<'local>(env: &mut JNIEnv<'local>, raw: jstring) -> Result<String, String> {
    // Safety: `raw` is a valid local reference passed by the JVM.
    let string = unsafe { JString::from_raw(raw) };
    let value = env.get_string(&string).map_err(|e| e.to_string())?;
    Ok(value.into())
}

// ---------------------------------------------------------------------------
// Controller operations (all protocol work happens inside fuji-ptp-core)
// ---------------------------------------------------------------------------

fn connect(bridge: GlobalRef) -> Result<(), String> {
    let vm: &'static JavaVM = VM.get().ok_or("JNI_OnLoad was not called")?;
    let transport = AndroidTransport { bridge, vm };
    let fuji = FujiPtp::new(transport);
    // If a previous controller exists (stale session), drop it: a fresh
    // connection must start from a clean session state.
    let prev = replace_controller(Controller { fuji });
    drop(prev);
    Ok(())
}

fn open_session(session_id: u32) -> Result<(), String> {
    let mut guard = lock_controller();
    let controller = guard.as_mut().ok_or("not connected")?;
    controller
        .fuji
        .open_session(session_id)
        .map_err(|e| format!("open session failed: {e:?}"))
}

fn close_session() -> Result<(), String> {
    let mut guard = lock_controller();
    let controller = guard.as_mut().ok_or("not connected")?;
    // Best effort: the camera may already be gone or the transport broken.
    // If the close itself fails we still want the session dropped, so the
    // next connect starts clean. The error is only informational.
    match controller.fuji.close_session() {
        Ok(()) => Ok(()),
        Err(e) => Err(format!("close session failed: {e:?}")),
    }
}

fn read_recipes_json() -> Result<String, String> {
    let mut guard = lock_controller();
    let controller = guard.as_mut().ok_or("not connected")?;
    let profile: Profile = controller
        .fuji
        .read_recipes()
        .map_err(|e| format!("read recipes failed: {e:?}"))?;
    serde_json::to_string(&profile).map_err(|e| format!("serialize failed: {e}"))
}

fn write_recipe(slot: u8, recipe_json: &str) -> Result<(), String> {
    let recipe = serde_json::from_str(recipe_json).map_err(|e| format!("bad recipe JSON: {e}"))?;
    let mut guard = lock_controller();
    let controller = guard.as_mut().ok_or("not connected")?;
    controller
        .fuji
        .write_recipe(slot, &recipe)
        .map_err(|e| format!("write recipe C{slot} failed: {e:?}"))
}

/// Like [write_recipe] but skips the slot name: the camera keeps its own
/// name. Used by the Android app when pushing recipes so names are never
/// garbled (renames are explicit via write_recipe_names).
fn write_recipe_settings(slot: u8, recipe_json: &str) -> Result<(), String> {
    let recipe = serde_json::from_str(recipe_json).map_err(|e| format!("bad recipe JSON: {e}"))?;
    let mut guard = lock_controller();
    let controller = guard.as_mut().ok_or("not connected")?;
    controller
        .fuji
        .write_recipe_settings(slot, &recipe)
        .map_err(|e| format!("write recipe settings C{slot} failed: {e:?}"))
}

/// Writes only the names of the 7 slots, preserving every other camera value.
fn write_recipe_names(names_json: &str) -> Result<(), String> {
    let names: Vec<String> =
        serde_json::from_str(names_json).map_err(|e| format!("bad names JSON: {e}"))?;
    if names.len() != 7 {
        return Err(format!("expected 7 names, got {}", names.len()));
    }
    let mut guard = lock_controller();
    let controller = guard.as_mut().ok_or("not connected")?;
    let recipes: [fuji_ptp_core::recipe::Recipe; 7] =
        std::array::from_fn(|i| fuji_ptp_core::recipe::Recipe::new(names[i].clone()));
    let profile = Profile::new("Names only".into(), recipes);
    controller
        .fuji
        .write_recipe_names(&profile)
        .map_err(|e| format!("write names failed: {e:?}"))
}

// ---------------------------------------------------------------------------
// Exported JNI functions (Kotlin class: com.alpefe.fujiptp.FujiNative)
// ---------------------------------------------------------------------------

/// "0.1.0"
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_alpefe_fujiptp_FujiNative_nativeVersion(
    env: JNIEnv<'_>,
    _this: JObject<'_>,
) -> jstring {
    let value = env
        .new_string(concat!("fuji-ptp-android/", env!("CARGO_PKG_VERSION")))
        .expect("JNI string");
    value.into_raw()
}

/// nativeConnect(bridge: UsbIo, sessionId: Int): String  -> JSON {ok} / {ok:false,error}
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_alpefe_fujiptp_FujiNative_nativeConnect(
    env: JNIEnv<'_>,
    _this: JObject<'_>,
    bridge: JObject<'_>,
    session_id: jint,
) -> jstring {
    clear_pending_exception(&env);
    let _ = session_id; // session id is passed to nativeOpenSession
    let result = env
        .new_global_ref(&bridge)
        .map_err(|e| e.to_string())
        .and_then(connect);
    let json = match result {
        Ok(()) => ok_json(),
        Err(e) => err_json(e),
    };
    env.new_string(json).expect("JNI string").into_raw()
}

/// nativeOpenSession(sessionId: Int): String
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_alpefe_fujiptp_FujiNative_nativeOpenSession(
    env: JNIEnv<'_>,
    _this: JObject<'_>,
    session_id: jint,
) -> jstring {
    clear_pending_exception(&env);
    let json = match open_session(session_id.max(0) as u32) {
        Ok(()) => ok_json(),
        Err(e) => err_json(e),
    };
    env.new_string(json).expect("JNI string").into_raw()
}

/// nativeCloseSession(): String
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_alpefe_fujiptp_FujiNative_nativeCloseSession(
    env: JNIEnv<'_>,
    _this: JObject<'_>,
) -> jstring {
    clear_pending_exception(&env);
    let json = match close_session() {
        Ok(()) => ok_json(),
        Err(e) => err_json(e),
    };
    env.new_string(json).expect("JNI string").into_raw()
}

/// nativeReadRecipes(): String -> {"ok":true,"recipes":[...]} | {"ok":false,"error":...}
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_alpefe_fujiptp_FujiNative_nativeReadRecipes(
    env: JNIEnv<'_>,
    _this: JObject<'_>,
) -> jstring {
    clear_pending_exception(&env);
    let json = match read_recipes_json() {
        Ok(profile_json) => format!("{{\"ok\":true,\"profile\":{profile_json}}}"),
        Err(e) => err_json(e),
    };
    env.new_string(json).expect("JNI string").into_raw()
}

/// nativeWriteRecipe(slot: Int, recipeJson: String): String
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_alpefe_fujiptp_FujiNative_nativeWriteRecipe(
    mut env: JNIEnv<'_>,
    _this: JObject<'_>,
    slot: jint,
    recipe_json: jstring,
) -> jstring {
    clear_pending_exception(&env);
    let result = unsafe { json_param(&mut env, recipe_json) }
        .and_then(|json| write_recipe(slot.max(1) as u8, &json));
    let json = match result {
        Ok(()) => ok_json(),
        Err(e) => err_json(e),
    };
    env.new_string(json).expect("JNI string").into_raw()
}

/// nativeWriteRecipeSettings(slot: Int, recipeJson: String): String
/// Writes recipe settings but NOT the slot name (camera keeps its name).
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_alpefe_fujiptp_FujiNative_nativeWriteRecipeSettings(
    mut env: JNIEnv<'_>,
    _this: JObject<'_>,
    slot: jint,
    recipe_json: jstring,
) -> jstring {
    clear_pending_exception(&env);
    let result = unsafe { json_param(&mut env, recipe_json) }
        .and_then(|json| write_recipe_settings(slot.max(1) as u8, &json));
    let json = match result {
        Ok(()) => ok_json(),
        Err(e) => err_json(e),
    };
    env.new_string(json).expect("JNI string").into_raw()
}

/// nativeWriteRecipeNames(namesJson: String): String  -> JSON array of 7 names
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_alpefe_fujiptp_FujiNative_nativeWriteRecipeNames(
    mut env: JNIEnv<'_>,
    _this: JObject<'_>,
    names_json: jstring,
) -> jstring {
    clear_pending_exception(&env);
    let result =
        unsafe { json_param(&mut env, names_json) }.and_then(|json| write_recipe_names(&json));
    let json = match result {
        Ok(()) => ok_json(),
        Err(e) => err_json(e),
    };
    env.new_string(json).expect("JNI string").into_raw()
}

/// nativeClose(): String  — drops the controller entirely (best effort).
/// After this, a fresh connect() must be performed; no stale session state
/// survives.
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_alpefe_fujiptp_FujiNative_nativeClose(
    env: JNIEnv<'_>,
    _this: JObject<'_>,
) -> jstring {
    clear_pending_exception(&env);
    // If a session is open, try to close it first (ignoring errors), then
    // drop the controller so the transport/state is fully released.
    if let Some(mut controller) = lock_controller().take() {
        let _ = controller.fuji.close_session();
        drop(controller);
    }
    env.new_string(ok_json()).expect("JNI string").into_raw()
}

#[unsafe(no_mangle)]
pub extern "system" fn JNI_OnLoad(vm: JavaVM, _reserved: *mut c_void) -> jint {
    let _ = VM.set(vm);
    JNI_VERSION_1_6
}

// ---------------------------------------------------------------------------
// Host-side tests: validate the JSON contract against a scripted fake
// camera. No JVM involved: the transport is exercised directly.
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;
    use fuji_ptp_core::transport::MockTransport;

    // --- PTP packet builders mirroring src/ptp/session.rs -------------------

    fn response_ok(tx: u32) -> Vec<u8> {
        let mut p = vec![12, 0, 0, 0, 3, 0, 0x01, 0x20];
        p.extend_from_slice(&tx.to_le_bytes());
        p
    }

    fn data_container(op: u16, tx: u32, payload: &[u8]) -> Vec<u8> {
        let mut p = vec![0, 0, 0, 0, 2, 0];
        p.extend_from_slice(&op.to_le_bytes());
        p.extend_from_slice(&tx.to_le_bytes());
        p.extend_from_slice(payload);
        let length = p.len() as u32;
        p[0..4].copy_from_slice(&length.to_le_bytes());
        p
    }

    /// Scripts a fake camera answering open_session + read_recipes() with one
    /// recipe replicated over the seven slots.
    fn queue_read_script(transport: &mut MockTransport, name: &str, film_wire: u16) {
        let mut tx = 1u32;
        transport.queue_received(response_ok(tx)); // open_session
        for _ in 0..7u8 {
            tx += 1;
            transport.queue_received(response_ok(tx)); // select_slot (SET, wait)
            let name_units: Vec<u16> = name.encode_utf16().collect();
            let mut name_bytes = vec![(name_units.len() + 1) as u8];
            for u in name_units {
                name_bytes.extend_from_slice(&u.to_le_bytes());
            }
            name_bytes.extend_from_slice(&[0, 0]);
            tx += 1;
            transport.queue_received(data_container(0x1015, tx, &name_bytes));
            transport.queue_received(response_ok(tx));
            // 19 property values (PROPS order in src/ptp/fuji.rs).
            // Wire values: highlight=+1.0, shadow=-1.0, color=+2.0,
            // sharpness=+1.0, NR=0, clarity=+4.0.
            let props: Vec<u16> = vec![
                100,
                0,
                film_wire,
                0,
                0,
                1,
                1,
                1,
                1,
                2,
                0,
                0,
                0,
                10,
                (-10i16) as u16,
                20,
                10,
                8192,
                40,
            ];
            for value in props {
                tx += 1;
                transport.queue_received(data_container(0x1015, tx, &value.to_le_bytes()));
                transport.queue_received(response_ok(tx));
            }
        }
    }

    #[test]
    fn read_recipes_through_fake_camera_produces_core_json() {
        let mut transport = MockTransport::new();
        queue_read_script(&mut transport, "CINEMA GOLD", 11 /* Classic Chrome */);

        let mut fuji = FujiPtp::new(transport);
        fuji.open_session(1).expect("open session");
        let profile = fuji.read_recipes().expect("read recipes");
        assert_eq!(profile.recipes.len(), 7);
        for recipe in &profile.recipes {
            assert_eq!(recipe.name, "CINEMA GOLD");
            assert_eq!(recipe.highlight, 1.0);
            assert_eq!(recipe.shadow, -1.0);
            assert_eq!(recipe.color, 2.0);
            assert_eq!(recipe.sharpness, 1.0);
            assert_eq!(recipe.clarity, 4.0);
        }
        // The JSON contract itself: core serde with snake_case names.
        let json = serde_json::to_string(&profile).unwrap();
        let parsed: serde_json::Value = serde_json::from_str(&json).unwrap();
        assert_eq!(parsed["recipes"].as_array().unwrap().len(), 7);
        assert_eq!(parsed["recipes"][0]["name"], "CINEMA GOLD");
        assert_eq!(parsed["recipes"][0]["film_simulation"], "ClassicChrome");
        assert_eq!(parsed["recipes"][0]["highlight"], 1.0);
        assert_eq!(parsed["recipes"][0]["white_balance"]["shift_r"], 0);
        // And the exact shape the Kotlin CameraClient expects:
        let wrapped = format!("{{\"ok\":true,\"profile\":{json}}}");
        let wrapped_parsed: serde_json::Value = serde_json::from_str(&wrapped).unwrap();
        assert_eq!(wrapped_parsed["ok"], true);
        assert_eq!(
            wrapped_parsed["profile"]["recipes"][0]["grain_effect"],
            "Off"
        );
    }

    #[test]
    fn recipe_json_roundtrips_through_serde() {
        let recipe: fuji_ptp_core::recipe::Recipe = serde_json::from_str(
            r#"{"name":"Portra400","film_simulation":"Velvia","dynamic_range":"Dr200",
               "grain_effect":"StrongSmall","smooth_skin":"Off","color_chrome":"Weak",
               "color_chrome_fx_blue":"Strong","white_balance":{"mode":"ColorTemperature",
               "shift_r":3,"shift_b":-2,"color_temperature":5600},"highlight":1.5,"shadow":-1.0,
               "color":2.0,"sharpness":0.5,"noise_reduction":-2,"clarity":-1.0,"exposure":0.0,
               "dynamic_range_priority":0,"monochrome_wc":0.0,"monochrome_mg":0.0}"#,
        )
        .expect("parse recipe JSON");
        assert_eq!(recipe.name, "Portra400");
        assert_eq!(
            recipe.film_simulation,
            fuji_ptp_core::recipe::FilmSimulation::Velvia
        );
        assert_eq!(
            recipe.grain_effect,
            fuji_ptp_core::recipe::GrainEffect::StrongSmall
        );
        assert_eq!(recipe.white_balance.color_temperature, Some(5600));
        assert_eq!(recipe.highlight, 1.5);
        assert_eq!(recipe.noise_reduction, -2);
    }
}
