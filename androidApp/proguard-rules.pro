# R8/ProGuard keep rules for the release build (isMinifyEnabled = true).
# Most libraries (Ktor, Coil, OkHttp, SQLDelight, Koin) ship their own consumer
# rules; the ones below cover the gaps. Expand as release testing surfaces
# `ClassNotFoundException` / missing-serializer / reflection failures.

# ---- kotlinx.serialization -------------------------------------------------
# Keep @Serializable types, their generated serializers, and Companions.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Enum entries are referenced reflectively by the serializer.
-keepclassmembers enum ** { *; }

# ---- App + Weft model classes used in (de)serialization --------------------
# DTOs/state serialized to DataStore/SQLDelight/network — keep their members.
-keep,includedescriptorclasses class dev.weft.**$$serializer { *; }
-keepclassmembers class dev.weft.** {
    *** Companion;
}

# ---- Coroutines ------------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# ---- Crash readability: keep source file + line numbers --------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
