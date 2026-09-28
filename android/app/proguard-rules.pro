-keep class com.aionos.service.AgentAccessibilityService { *; }
-keep class com.aionos.service.OverlayBubbleService { *; }
-keepattributes *Annotation*, Signature, Exception, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Serializable <fields>;
}
-keep class com.aionos.action.** { *; }
-keep class com.aionos.plugin.** { *; }
-keep class com.aionos.llm.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**
-keep class org.vosk.** { *; }
-dontwarn org.vosk.**
-keep class androidx.security.** { *; }
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
# AutoValue bundles annotation-processing helpers that are not present on Android
# at runtime. They are build-time-only references and can be ignored by R8.
-dontwarn javax.annotation.processing.**
-dontwarn javax.lang.model.**
# Some transitive libraries probe for the optional SLF4J 1.x static binder.
-dontwarn org.slf4j.impl.StaticLoggerBinder
