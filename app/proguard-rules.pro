-keepclassmembers class com.monochrome.app.JsBlobReceiver {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class androidx.media.** { *; }
-keep class com.monochrome.app.AppExitReceiver { *; }
-keep class com.monochrome.app.NoisyReceiver { *; }
-keep class com.monochrome.app.MusicService { *; }
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
