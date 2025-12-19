# EchoKit consumer rules
# Keep serialization classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.michaelblades.echokit.**$$serializer { *; }
-keepclassmembers class com.michaelblades.echokit.** {
    *** Companion;
}
-keepclasseswithmembers class com.michaelblades.echokit.** {
    kotlinx.serialization.KSerializer serializer(...);
}
