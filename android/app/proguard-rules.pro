# Noodle IDE release rules.
# Keep Retrofit/Gson model metadata available for reflective JSON conversion.
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.arkansasio.nudl.api.** { *; }
-dontwarn javax.annotation.**
