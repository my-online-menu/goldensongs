# kotlinx.serialization keeps its generated serializers via annotations
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.blackamp.data.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.blackamp.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
