//! JNI bridge for the Android application.
//!
//! Responsibilities:
//! - Keep `fuji-ptp-core` platform independent (no Android/JNI code in it).
//! - Expose a small, high-level JNI API to Kotlin (JSON strings + one bridge
//!   object), so Kotlin never re-implements PTP containers.
//!
//! Architecture:
//!
//! ```text
//! Kotlin UsbIoBridge (owns UsbDeviceConnection)
//!         │  JNI callbacks: send(byte[]) / receive(int)
//!         ▼
//! AndroidTransport (this crate)  implements fuji_ptp_core::transport::Transport
//!         ▼
//! fuji_ptp_core::FujiPtp  (whole Fujifilm PTP protocol lives here)
//! ```
//!
//! The Kotlin `UsbIo` interface must offer:
//! - `send(data: ByteArray): Int`  (bulk OUT, returns bytes written)
//! - `receive(size: Int): ByteArray` (bulk IN, exactly up to `size` bytes)
//!
//! JSON is used as the DTO boundary so the JNI surface stays tiny and the
//! application can evolve without touching the bridge.

use std::ffi::c_void;
use std::sync::{Mutex, OnceLock};

use fuji_ptp_core::ptp::FujiPtp;
use fuji_ptp_core::recipe::{
    DynamicRange, EffectStrength, FilmSimulation, GrainEffect, Profile, Recipe, WhiteBalance,
    WhiteBalanceMode,
};
use fuji_ptp_core::transport::{Transport, TransportError};
use jni::objects::{GlobalRef, JByteArray, JObject, JString, JValue, JValueOwned};
use jni::sys::{JNI_VERSION_1_6, jint, jstring};
use jni::{JNIEnv, JavaVM};
use serde::{Deserialize, Serialize};

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
        // 1. Read the 12-byte header to learn the container length.
        let header = self.call_receive(12)?;
        if header.len() < 12 {
            return Err(TransportError::ReceiveError);
        }
        let length = u32::from_le_bytes(header[0..4].try_into().unwrap()) as usize;
        if !(12..=1024 * 1024).contains(&length) {
            return Err(TransportError::ReceiveError);
        }
        // 2. Read the declared payload in chunks.
        let mut packet = header;
        packet.resize(length, 0);
        let mut offset = 12;
        while offset < length {
            let chunk = self.call_receive((length - offset) as i32)?;
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
// DTOs (JSON boundary)
// ---------------------------------------------------------------------------

/// Flat, serde-friendly representation of a Fujifilm recipe. Field names and
/// enum string values are the contract with the Kotlin app.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RecipeDto {
    pub name: String,
    pub film_simulation: String,
    pub dynamic_range: String,
    pub grain_effect: String,
    pub smooth_skin: String,
    pub color_chrome: String,
    pub color_chrome_fx_blue: String,
    pub white_balance_mode: String,
    pub white_balance_shift_r: i16,
    pub white_balance_shift_b: i16,
    pub white_balance_temperature: Option<u16>,
    pub highlight: f32,
    pub shadow: f32,
    pub color: f32,
    pub sharpness: f32,
    pub clarity: f32,
    pub noise_reduction: i8,
    pub exposure: f32,
    pub dynamic_range_priority: i32,
    pub monochrome_wc: f32,
    pub monochrome_mg: f32,
}

fn film_from(s: &str) -> Option<FilmSimulation> {
    Some(match s {
        "Provia" => FilmSimulation::Provia,
        "Velvia" => FilmSimulation::Velvia,
        "Astia" => FilmSimulation::Astia,
        "ProNegHigh" => FilmSimulation::ProNegHigh,
        "ProNegStandard" => FilmSimulation::ProNegStandard,
        "Monochrome" => FilmSimulation::Monochrome,
        "MonochromeYellow" => FilmSimulation::MonochromeYellow,
        "MonochromeRed" => FilmSimulation::MonochromeRed,
        "MonochromeGreen" => FilmSimulation::MonochromeGreen,
        "Sepia" => FilmSimulation::Sepia,
        "ClassicChrome" => FilmSimulation::ClassicChrome,
        "Acros" => FilmSimulation::Acros,
        "AcrosYellow" => FilmSimulation::AcrosYellow,
        "AcrosRed" => FilmSimulation::AcrosRed,
        "AcrosGreen" => FilmSimulation::AcrosGreen,
        "Eterna" => FilmSimulation::Eterna,
        "ClassicNegative" => FilmSimulation::ClassicNegative,
        "EternaBleachBypass" => FilmSimulation::EternaBleachBypass,
        "NostalgicNegative" => FilmSimulation::NostalgicNegative,
        "RealaAce" => FilmSimulation::RealaAce,
        _ => return None,
    })
}

fn film_name(f: &FilmSimulation) -> &'static str {
    match f {
        FilmSimulation::Provia => "Provia",
        FilmSimulation::Velvia => "Velvia",
        FilmSimulation::Astia => "Astia",
        FilmSimulation::ProNegHigh => "ProNegHigh",
        FilmSimulation::ProNegStandard => "ProNegStandard",
        FilmSimulation::Monochrome => "Monochrome",
        FilmSimulation::MonochromeYellow => "MonochromeYellow",
        FilmSimulation::MonochromeRed => "MonochromeRed",
        FilmSimulation::MonochromeGreen => "MonochromeGreen",
        FilmSimulation::Sepia => "Sepia",
        FilmSimulation::ClassicChrome => "ClassicChrome",
        FilmSimulation::Acros => "Acros",
        FilmSimulation::AcrosYellow => "AcrosYellow",
        FilmSimulation::AcrosRed => "AcrosRed",
        FilmSimulation::AcrosGreen => "AcrosGreen",
        FilmSimulation::Eterna => "Eterna",
        FilmSimulation::ClassicNegative => "ClassicNegative",
        FilmSimulation::EternaBleachBypass => "EternaBleachBypass",
        FilmSimulation::NostalgicNegative => "NostalgicNegative",
        FilmSimulation::RealaAce => "RealaAce",
    }
}

fn dr_from(s: &str) -> Option<DynamicRange> {
    match s {
        "Dr100" => Some(DynamicRange::Dr100),
        "Dr200" => Some(DynamicRange::Dr200),
        "Dr400" => Some(DynamicRange::Dr400),
        _ => None,
    }
}

fn dr_name(d: &DynamicRange) -> &'static str {
    match d {
        DynamicRange::Dr100 => "Dr100",
        DynamicRange::Dr200 => "Dr200",
        DynamicRange::Dr400 => "Dr400",
    }
}

fn grain_from(s: &str) -> Option<GrainEffect> {
    Some(match s {
        "Off" => GrainEffect::Off,
        "WeakSmall" => GrainEffect::WeakSmall,
        "StrongSmall" => GrainEffect::StrongSmall,
        "WeakLarge" => GrainEffect::WeakLarge,
        "StrongLarge" => GrainEffect::StrongLarge,
        _ => return None,
    })
}

fn grain_name(g: &GrainEffect) -> &'static str {
    match g {
        GrainEffect::Off => "Off",
        GrainEffect::WeakSmall => "WeakSmall",
        GrainEffect::StrongSmall => "StrongSmall",
        GrainEffect::WeakLarge => "WeakLarge",
        GrainEffect::StrongLarge => "StrongLarge",
    }
}

fn strength_from(s: &str) -> Option<EffectStrength> {
    match s {
        "Off" => Some(EffectStrength::Off),
        "Weak" => Some(EffectStrength::Weak),
        "Strong" => Some(EffectStrength::Strong),
        _ => None,
    }
}

fn strength_name(e: &EffectStrength) -> &'static str {
    match e {
        EffectStrength::Off => "Off",
        EffectStrength::Weak => "Weak",
        EffectStrength::Strong => "Strong",
    }
}

fn wb_from(s: &str) -> Option<WhiteBalanceMode> {
    Some(match s {
        "Auto" => WhiteBalanceMode::Auto,
        "Daylight" => WhiteBalanceMode::Daylight,
        "Shade" => WhiteBalanceMode::Shade,
        "Fluorescent1" => WhiteBalanceMode::Fluorescent1,
        "Fluorescent2" => WhiteBalanceMode::Fluorescent2,
        "Fluorescent3" => WhiteBalanceMode::Fluorescent3,
        "Incandescent" => WhiteBalanceMode::Incandescent,
        "Underwater" => WhiteBalanceMode::Underwater,
        "ColorTemperature" => WhiteBalanceMode::ColorTemperature,
        "AmbiencePriority" => WhiteBalanceMode::AmbiencePriority,
        _ => return None,
    })
}

fn wb_name(w: &WhiteBalanceMode) -> &'static str {
    match w {
        WhiteBalanceMode::Auto => "Auto",
        WhiteBalanceMode::Daylight => "Daylight",
        WhiteBalanceMode::Shade => "Shade",
        WhiteBalanceMode::Fluorescent1 => "Fluorescent1",
        WhiteBalanceMode::Fluorescent2 => "Fluorescent2",
        WhiteBalanceMode::Fluorescent3 => "Fluorescent3",
        WhiteBalanceMode::Incandescent => "Incandescent",
        WhiteBalanceMode::Underwater => "Underwater",
        WhiteBalanceMode::ColorTemperature => "ColorTemperature",
        WhiteBalanceMode::AmbiencePriority => "AmbiencePriority",
    }
}

impl RecipeDto {
    pub fn from_recipe(r: &Recipe) -> Self {
        Self {
            name: r.name.clone(),
            film_simulation: film_name(&r.film_simulation).to_string(),
            dynamic_range: dr_name(&r.dynamic_range).to_string(),
            grain_effect: grain_name(&r.grain_effect).to_string(),
            smooth_skin: strength_name(&r.smooth_skin).to_string(),
            color_chrome: strength_name(&r.color_chrome).to_string(),
            color_chrome_fx_blue: strength_name(&r.color_chrome_fx_blue).to_string(),
            white_balance_mode: wb_name(&r.white_balance.mode).to_string(),
            white_balance_shift_r: r.white_balance.shift_r,
            white_balance_shift_b: r.white_balance.shift_b,
            white_balance_temperature: r.white_balance.color_temperature,
            highlight: r.highlight,
            shadow: r.shadow,
            color: r.color,
            sharpness: r.sharpness,
            clarity: r.clarity,
            noise_reduction: r.noise_reduction,
            exposure: r.exposure,
            dynamic_range_priority: r.dynamic_range_priority,
            monochrome_wc: r.monochrome_wc,
            monochrome_mg: r.monochrome_mg,
        }
    }

    pub fn to_recipe(&self) -> Result<Recipe, String> {
        let film_simulation = film_from(&self.film_simulation)
            .ok_or_else(|| format!("unknown film simulation '{}'", self.film_simulation))?;
        let dynamic_range = dr_from(&self.dynamic_range)
            .ok_or_else(|| format!("unknown dynamic range '{}'", self.dynamic_range))?;
        let grain_effect = grain_from(&self.grain_effect)
            .ok_or_else(|| format!("unknown grain effect '{}'", self.grain_effect))?;
        let smooth_skin = strength_from(&self.smooth_skin)
            .ok_or_else(|| format!("unknown smooth skin '{}'", self.smooth_skin))?;
        let color_chrome = strength_from(&self.color_chrome)
            .ok_or_else(|| format!("unknown color chrome '{}'", self.color_chrome))?;
        let color_chrome_fx_blue = strength_from(&self.color_chrome_fx_blue)
            .ok_or_else(|| format!("unknown fx blue '{}'", self.color_chrome_fx_blue))?;
        let white_balance_mode = wb_from(&self.white_balance_mode)
            .ok_or_else(|| format!("unknown white balance '{}'", self.white_balance_mode))?;

        let mut recipe = Recipe::new(self.name.clone());
        recipe.film_simulation = film_simulation;
        recipe.dynamic_range = dynamic_range;
        recipe.grain_effect = grain_effect;
        recipe.smooth_skin = smooth_skin;
        recipe.color_chrome = color_chrome;
        recipe.color_chrome_fx_blue = color_chrome_fx_blue;
        recipe.white_balance = WhiteBalance {
            mode: white_balance_mode,
            shift_r: self.white_balance_shift_r,
            shift_b: self.white_balance_shift_b,
            color_temperature: self.white_balance_temperature,
        };
        recipe.highlight = self.highlight;
        recipe.shadow = self.shadow;
        recipe.color = self.color;
        recipe.sharpness = self.sharpness;
        recipe.clarity = self.clarity;
        recipe.noise_reduction = self.noise_reduction;
        recipe.exposure = self.exposure;
        recipe.dynamic_range_priority = self.dynamic_range_priority;
        recipe.monochrome_wc = self.monochrome_wc;
        recipe.monochrome_mg = self.monochrome_mg;
        Ok(recipe)
    }
}

// ---------------------------------------------------------------------------
// JNI helpers
// ---------------------------------------------------------------------------

fn ok_json() -> String {
    serde_json::json!({ "ok": true }).to_string()
}

fn err_json(message: impl AsRef<str>) -> String {
    serde_json::json!({ "ok": false, "error": message.as_ref() }).to_string()
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
// Controller operations
// ---------------------------------------------------------------------------

fn connect(bridge: GlobalRef) -> Result<(), String> {
    let vm: &'static JavaVM = VM.get().ok_or("JNI_OnLoad was not called")?;
    let transport = AndroidTransport { bridge, vm };
    let fuji = FujiPtp::new(transport);
    let mut guard = lock_controller();
    if guard.is_some() {
        return Err("already connected".into());
    }
    *guard = Some(Controller { fuji });
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
    controller
        .fuji
        .close_session()
        .map_err(|e| format!("close session failed: {e:?}"))
}

fn read_recipes() -> Result<Vec<RecipeDto>, String> {
    let mut guard = lock_controller();
    let controller = guard.as_mut().ok_or("not connected")?;
    let profile = controller
        .fuji
        .read_recipes()
        .map_err(|e| format!("read recipes failed: {e:?}"))?;
    Ok(profile.recipes.iter().map(RecipeDto::from_recipe).collect())
}

fn write_recipe(slot: u8, recipe: &RecipeDto) -> Result<(), String> {
    let mut guard = lock_controller();
    let controller = guard.as_mut().ok_or("not connected")?;
    let recipe = recipe.to_recipe()?;
    controller
        .fuji
        .write_recipe(slot, &recipe)
        .map_err(|e| format!("write recipe C{slot} failed: {e:?}"))
}

/// Writes only the names of the 7 slots, preserving every other camera value.
fn write_recipe_names(names: Vec<String>) -> Result<(), String> {
    if names.len() != 7 {
        return Err(format!("expected 7 names, got {}", names.len()));
    }
    let mut guard = lock_controller();
    let controller = guard.as_mut().ok_or("not connected")?;
    let recipes: [Recipe; 7] = std::array::from_fn(|i| Recipe::new(names[i].clone()));
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
    let json = match read_recipes() {
        Ok(recipes) => serde_json::json!({ "ok": true, "recipes": recipes }).to_string(),
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
        .and_then(|json| serde_json::from_str::<RecipeDto>(&json).map_err(|e| e.to_string()))
        .and_then(|recipe| write_recipe(slot.max(1) as u8, &recipe));
    let json = match result {
        Ok(()) => ok_json(),
        Err(e) => err_json(e),
    };
    env.new_string(json).expect("JNI string").into_raw()
}

/// nativeWriteRecipeNames(namesJson: String): String  -> {"names":["a",...,"g"]}
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_alpefe_fujiptp_FujiNative_nativeWriteRecipeNames(
    mut env: JNIEnv<'_>,
    _this: JObject<'_>,
    names_json: jstring,
) -> jstring {
    clear_pending_exception(&env);
    let result = unsafe { json_param(&mut env, names_json) }.and_then(|json| {
        let names: Vec<String> = serde_json::from_str(&json).map_err(|e| e.to_string())?;
        write_recipe_names(names)
    });
    let json = match result {
        Ok(()) => ok_json(),
        Err(e) => err_json(e),
    };
    env.new_string(json).expect("JNI string").into_raw()
}

/// nativeClose(): String  — drops the controller (best effort).
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_alpefe_fujiptp_FujiNative_nativeClose(
    env: JNIEnv<'_>,
    _this: JObject<'_>,
) -> jstring {
    clear_pending_exception(&env);
    *lock_controller() = None;
    env.new_string(ok_json()).expect("JNI string").into_raw()
}

#[unsafe(no_mangle)]
pub extern "system" fn JNI_OnLoad(vm: JavaVM, _reserved: *mut c_void) -> jint {
    let _ = VM.set(vm);
    JNI_VERSION_1_6
}

// ---------------------------------------------------------------------------
// Host-side tests: validate the DTO mapping and JSON contract against a
// scripted fake camera, without any JVM involved.
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

    /// Scripts a fake camera that answers open_session + read_recipes() for a
    /// single-slot recipe set (all seven slots receive the same recipe).
    /// Transaction ids advance sequentially, exactly like the real core.
    fn queue_read_script(transport: &mut MockTransport, recipe: &RecipeDto) {
        let mut tx = 1u32;
        // open_session: command then response.
        transport.queue_received(response_ok(tx));
        for _ in 0..7u8 {
            // select_slot: set_device_prop_value_wait -> command, data, response.
            tx += 1;
            transport.queue_received(response_ok(tx));
            // name property (PROP_SLOT_NAME 0xD18D)
            let name_units: Vec<u16> = recipe.name.encode_utf16().collect();
            let mut name_bytes = vec![(name_units.len() + 1) as u8];
            for u in name_units {
                name_bytes.extend_from_slice(&u.to_le_bytes());
            }
            name_bytes.extend_from_slice(&[0, 0]);
            tx += 1;
            transport.queue_received(data_container(0x1015, tx, &name_bytes));
            transport.queue_received(response_ok(tx));
            // 19 property values (PROPS list order).
            let props: Vec<u16> = vec![
                // dynamic range -> 100, priority 0, film, mono wc, mono mg,
                // grain, color chrome, fx blue, smooth, wb, wb_r, wb_b, temp,
                // highlight, shadow, color, sharpness, nr, clarity
                100,
                0,
                11, // Classic Chrome
                0,
                0,
                1, // grain off
                1,
                1,
                1,
                2, // wb auto
                0,
                0,
                0,
                (recipe.highlight * 10.0) as i16 as u16,
                (recipe.shadow * 10.0) as i16 as u16,
                (recipe.color * 10.0) as i16 as u16,
                (recipe.sharpness * 10.0) as i16 as u16,
                8192, // NR 0
                (recipe.clarity * 10.0) as i16 as u16,
            ];
            for value in props {
                tx += 1;
                transport.queue_received(data_container(0x1015, tx, &value.to_le_bytes()));
                transport.queue_received(response_ok(tx));
            }
        }
    }

    #[test]
    fn dto_roundtrip_preserves_fields() {
        let dto = RecipeDto {
            name: "Portra 400".into(),
            film_simulation: "ClassicNegative".into(),
            dynamic_range: "Dr200".into(),
            grain_effect: "StrongSmall".into(),
            smooth_skin: "Off".into(),
            color_chrome: "Weak".into(),
            color_chrome_fx_blue: "Strong".into(),
            white_balance_mode: "ColorTemperature".into(),
            white_balance_shift_r: 3,
            white_balance_shift_b: -2,
            white_balance_temperature: Some(5600),
            highlight: 1.5,
            shadow: -1.0,
            color: 2.0,
            sharpness: 0.5,
            clarity: -1.0,
            noise_reduction: -2,
            exposure: 0.0,
            dynamic_range_priority: 0,
            monochrome_wc: 0.0,
            monochrome_mg: 0.0,
        };
        let recipe = dto.to_recipe().expect("valid recipe");
        let back = RecipeDto::from_recipe(&recipe);
        assert_eq!(back.name, "Portra 400");
        assert_eq!(back.film_simulation, "ClassicNegative");
        assert_eq!(back.dynamic_range, "Dr200");
        assert_eq!(back.grain_effect, "StrongSmall");
        assert_eq!(back.color_chrome, "Weak");
        assert_eq!(back.white_balance_mode, "ColorTemperature");
        assert_eq!(back.white_balance_temperature, Some(5600));
        assert_eq!(back.highlight, 1.5);
        assert_eq!(back.noise_reduction, -2);
    }

    #[test]
    fn read_recipes_through_fake_camera_returns_seven_dtos() {
        let mut transport = MockTransport::new();
        let dto = RecipeDto {
            name: "CINEMA GOLD".into(),
            film_simulation: "ClassicChrome".into(),
            dynamic_range: "Dr100".into(),
            grain_effect: "Off".into(),
            smooth_skin: "Off".into(),
            color_chrome: "Off".into(),
            color_chrome_fx_blue: "Off".into(),
            white_balance_mode: "Auto".into(),
            white_balance_shift_r: 0,
            white_balance_shift_b: 0,
            white_balance_temperature: None,
            highlight: 1.0,
            shadow: -1.0,
            color: 2.0,
            sharpness: 1.0,
            clarity: 1.0,
            noise_reduction: 0,
            exposure: 0.0,
            dynamic_range_priority: 0,
            monochrome_wc: 0.0,
            monochrome_mg: 0.0,
        };
        queue_read_script(&mut transport, &dto);

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
            assert_eq!(recipe.clarity, 1.0);
        }
        // The JSON contract itself.
        let dtos: Vec<RecipeDto> = profile.recipes.iter().map(RecipeDto::from_recipe).collect();
        let json = serde_json::json!({ "ok": true, "recipes": dtos }).to_string();
        let parsed: serde_json::Value = serde_json::from_str(&json).unwrap();
        assert_eq!(parsed["ok"], true);
        assert_eq!(parsed["recipes"].as_array().unwrap().len(), 7);
        assert_eq!(parsed["recipes"][0]["name"], "CINEMA GOLD");
    }

    #[test]
    fn write_recipe_names_validates_arity() {
        let result = write_recipe_names(vec!["a".into(), "b".into()]);
        assert!(result.is_err());
    }
}
