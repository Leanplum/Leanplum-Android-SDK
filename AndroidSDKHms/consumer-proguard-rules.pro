# Keep default Leanplum classes.
-keepclassmembers class * {
@com.leanplum.annotations.* <fields>;
}

-keep class com.leanplum.** { *; }
-dontwarn com.leanplum.**

-keep class com.huawei.**{*;}
-keep class com.hianalytics.android.**{*;}
-dontwarn com.huawei.**
