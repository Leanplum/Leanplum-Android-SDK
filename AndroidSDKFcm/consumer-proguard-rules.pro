# Keep default Leanplum classes.
-keepclassmembers class * {
@com.leanplum.annotations.* <fields>;
}

-keep class com.leanplum.** { *; }
-dontwarn com.leanplum.**

# Keep bytebuddy classes.
-keep class net.bytebuddy.** { *; }
-dontwarn net.bytebuddy.**

# Keep Support Library classes.
-dontwarn android.support.v7.**
-keep class android.support.v7.** { *; }
-keep interface android.support.v7.** { *; }