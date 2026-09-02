-keep class com.schoolmanager.app.MainActivity { *; }
-keep class com.schoolmanager.app.BuildConfig { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-dontwarn org.chromium.**
