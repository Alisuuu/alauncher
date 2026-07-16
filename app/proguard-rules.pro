# R8/ProGuard configuration rules for ALauncher

# Keep theme models and components
-keep class com.alisu.alauncher.theme.** { *; }
-keep class com.alisu.alauncher.model.** { *; }

# Coil image loading library rules
-keep class coil.** { *; }
-dontwarn coil.**

# LeakCanary rules (only used in debug builds)
-keep class leakcanary.** { *; }
-dontwarn leakcanary.**

# Standard Android optimization options
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep custom Views and their constructors
-keepclasseswithmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# R8 full mode optimizations
-allowaccessmodification
-repackageclasses ''
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
