# ====================================================================
# ai i.z - Advanced Code Obfuscation & Security Protection Rules
# Developer: Ibrahim Zeinab (إبراهيم زينب)
# ====================================================================

# Optimize and obfuscate code aggressively
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 7
-allowaccessmodification
-repackageclasses 'ai.iz.core.secure'

# Remove all logging, debug traces, and assertions in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# Preserve attributes needed for reflection, serialization, and Compose UI
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# Keep domain models and core security/storage classes used via reflection/Moshi
-keep class com.example.domain.model.** { *; }
-keep class com.example.core.** { *; }
-keep class com.example.data.remote.** { *; }

# Keep AndroidX and Compose components
-keep class androidx.compose.** { *; }
-keep class com.google.firebase.** { *; }
-keepattributes *JavascriptInterface*
-keepattributes *JavascriptInterface*,Signature
