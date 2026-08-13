# --- kotlinx.serialization ---
# (Modern kotlinx-serialization ships consumer rules, but keep these for safety.)
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$Companion Companion;
}
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Our @Serializable models + their generated serializers.
-keep,includedescriptorclasses class com.shadabshaikh.networth.model.**$$serializer { *; }
-keepclassmembers class com.shadabshaikh.networth.model.** { *; }

# --- Google Play Services (Auth) keeps its own consumer rules; nothing extra needed. ---
