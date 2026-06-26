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

# ---- Missing optional/JVM-only references (unreachable on Android) ----------
# PDFBox's JPEG2000 filter references an optional codec we don't bundle; Ktor's
# debugger detector touches java.lang.management, absent on Android. Neither is
# reached at runtime — suppress the R8 missing-class errors.
-dontwarn com.gemalto.jp2.JP2Decoder
-dontwarn com.gemalto.jp2.JP2Encoder
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
