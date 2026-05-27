# Add project specific ProGuard rules here.
-keepattributes Signature
-keepattributes *Annotation*

# Keep JSON model classes
-keep class org.json.** { *; }

# Keep the application classes
-keep class com.guyang.admin.** { *; }