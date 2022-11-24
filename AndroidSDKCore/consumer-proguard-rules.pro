# Keep default Leanplum classes.
-keepclassmembers class * {
@com.leanplum.annotations.* <fields>;
}

-keep class com.leanplum.** { *; }
-dontwarn com.leanplum.**

# Keep bytebuddy classes.
-keep class net.bytebuddy.** { *; }
-dontwarn net.bytebuddy.**

# Reflection is used to get CT version in runtime
-keep class com.clevertap.android.sdk.BuildConfig { *; }
