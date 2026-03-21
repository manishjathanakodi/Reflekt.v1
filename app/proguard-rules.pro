# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line numbers for crash stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── kotlinx.serialization ──────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.reflekt.journal.**$$serializer { *; }
-keepclassmembers class com.reflekt.journal.** {
    *** Companion;
}
-keepclasseswithmembers class com.reflekt.journal.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Room entities & DAOs ───────────────────────────────────────────────────────
-keep class com.reflekt.journal.data.db.** { *; }
-keep interface com.reflekt.journal.data.db.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { abstract *; }

# ── SQLCipher ──────────────────────────────────────────────────────────────────
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# ── Hilt / Dagger ──────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclasseswithmembernames class * { @javax.inject.* <fields>; }

# ── MediaPipe ──────────────────────────────────────────────────────────────────
-keep class com.google.mediapipe.** { *; }

# ── Suppress noisy warnings ────────────────────────────────────────────────────
-dontwarn kotlinx.serialization.**