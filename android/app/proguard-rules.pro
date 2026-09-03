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
