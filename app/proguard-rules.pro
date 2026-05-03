# Add project specific ProGuard rules here.
# By default, the flags in this file are applied to all build types.

# Keep Retrofit model classes (needed so Gson can deserialize JSON)
-keep class com.weather.app.data.model.** { *; }

# Retrofit rules
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
