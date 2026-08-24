# Keep the native library name stable across builds.
-keep class com.alpefe.fujiptp.FujiNative { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
