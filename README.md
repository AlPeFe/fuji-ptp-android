# Fuji Recipes (Android)

App Android para leer y escribir las 7 Custom Recipes (C1–C7) de una
**Fujifilm X100VI** por USB. Kotlin + Jetpack Compose + Material 3, con el
protocolo PTP/Fujifilm 100% en Rust a través de JNI.

```text
Jetpack Compose / Material 3 (Kotlin)
        ↓
FujiUsbManager + UsbIoBridge (bulk IN/OUT)
        ↓ JNI
Rust AndroidTransport (crate JNI de este repo)
        ↓
fuji-ptp-core (repo separado: protocolo PTP/Fujifilm)
```

El protocolo PTP NO se duplica en Kotlin: el transporte Android solo hace
USB discovery, permisos y bulk I/O; todo lo demás vive en
[fuji-ptp-core](https://github.com/AlPeFe/fuji-ptp-core).

## Funcionalidades

- **Biblioteca**: crea recipes sin cámara conectada; backlog local ilimitado
  con persistencia Room (SQLite). Duplicar, editar, borrar.
- **7 slots activos**: asigna cualquier recipe de la biblioteca a C1–C7.
- **Cámara**: detecta la X100VI por USB (vendor 0x04CB), pide permiso,
  abre sesión PTP, lee C1–C7 (importa a la biblioteca con dedupe) y escribe
  una recipe completa en cualquier slot. Confirmación explícita antes de
  escribir en la cámara.
- **Controles X100VI**: simulación de película (20), DR (100/200/400),
  Prioridad DR (Off/Auto/Strong/Weak), Grain, Color Chrome, FX Blue, Smooth
  Skin, Balance de blancos (modo, shift R/B, temperatura), Highlight, Shadow,
  Color, Sharpness, Clarity, High ISO NR y WC/MG en monocromo.

## Estructura

```text
android/   Proyecto Gradle: app Kotlin (Compose), jniLibs por ABI
rust/      Crate JNI (cdylib) que consume fuji-ptp-core
```

## Compilar

Requisitos: JDK 17+, Android SDK (NDK solo para recompilar el puente Rust),
Rust + cargo-ndk.

```bash
# 1. Bridge Rust → .so (opcional, las .so ya están en jniLibs/)
rustup target add aarch64-linux-android x86_64-linux-android
cargo install cargo-ndk
# apunta fuji-ptp-core al repo clonado en Cargo.toml del crate rust/,
# luego:
ANDROID_NDK_HOME="$ANDROID_HOME/ndk/<versión>" \
  cargo ndk -t arm64-v8a -t x86_64 \
  -o android/app/src/main/jniLibs \
  build --release --manifest-path rust/Cargo.toml

# 2. APK
cd android
./gradlew assembleDebug          # requiere local.properties con sdk.dir
```

El dispositivo debe estar en modo USB **«RAW CONV./BACKUP RESTORE»**. Android
no necesita Zadig/WinUSB.

## Capa nativa

Fachada JNI pequeña y de alto nivel (JSON como DTO):

| Kotlin (`FujiNative`)            | Rust (este repo)                      |
|----------------------------------|---------------------------------------|
| `nativeConnect(bridge, id)`      | guarda `GlobalRef` del puente USB     |
| `nativeOpenSession(id)`          | `FujiPtp::open_session`               |
| `nativeReadRecipes()`            | `FujiPtp::read_recipes` → JSON        |
| `nativeWriteRecipe(slot, json)`  | `FujiPtp::write_recipe`               |
| `nativeWriteRecipeNames(json)`   | `FujiPtp::write_recipe_names`         |
| `nativeCloseSession()`           | `FujiPtp::close_session`              |
| `nativeClose()`                  | libera el controlador                 |

El transporte Android implementa `fuji_ptp_core::transport::Transport` y
llama por JNI a `UsbIo.send(byte[])` / `receive(int)` de Kotlin.

## Tests

```bash
cargo test --manifest-path rust/Cargo.toml    # bridge contra cámara simulada
```

## Pendiente de verificar con hardware

- Mapeo de «Prioridad DR» → valores de cable (Off/Auto/Strong/Weak).
- Escribir Mono WC/MG en simulaciones monocromo (el core ya omite esas
  propiedades para película en color, como exige la cámara real).
