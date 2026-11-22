# ProGuard / R8 rules for AURA Music

# WebView JavaScript interfaces for PoToken / BotGuard
-keepclassmembers class com.auramusic.app.utils.potoken.PoTokenWebView {
    @android.webkit.JavascriptInterface public *;
}
-keep class com.auramusic.app.utils.potoken.** { *; }

# Keep InnerTube & InnerTubeX models and serializers
-keep class com.metrolist.innertube.** { *; }
-keep class com.metrolist.innertubex.** { *; }

# Keep coroutine continuation for WebView callbacks
-keepclassmembers class * {
    void resume(...);
    void resumeWithException(...);
}

# Kotlin Serialization
-if @kotlinx.serialization.Serializable class **
-keepclasseswithmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclasseswithmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclasseswithmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,SourceFile,LineNumberTable

-dontwarn javax.servlet.ServletContainerInitializer
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn org.slf4j.**
-dontwarn org.mozilla.javascript.**
-dontwarn javax.script.**
-dontwarn jdk.dynalink.**

