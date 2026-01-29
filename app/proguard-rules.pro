# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# ============================================
# SQLCipher - CRITICAL: DO NOT REMOVE
# ============================================
# Keep ALL SQLCipher classes - no exceptions
# NOTE: Core runtime package is net.sqlcipher.database.*, Room support lives in net.zetetic.database.sqlcipher.*
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-keepclassmembers class net.sqlcipher.** { *; }
-keepclassmembers class net.sqlcipher.database.** { *; }

# Keep SQLCipher native library loading methods
-keepclassmembers class net.sqlcipher.database.SQLiteDatabase {
    public static void loadLibs(android.content.Context);
    public static void loadLibs();
    public static *** loadLibs(...);
}

# Keep all SQLCipher interfaces and implementations
-keep interface net.sqlcipher.database.** { *; }
-keep class * implements net.sqlcipher.database.** { *; }

# Don't warn about missing SQLCipher classes
-dontwarn net.sqlcipher.**
-dontwarn net.sqlcipher.database.**

# Keep SQLite classes used by SQLCipher
-keep class androidx.sqlite.db.** { *; }
-keep class androidx.sqlite.db.framework.** { *; }
-dontwarn androidx.sqlite.db.**

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Prevent R8 from removing SQLCipher even if only accessed via reflection
-keep,allowobfuscation class net.sqlcipher.database.SQLiteDatabase

# Keep Room classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Hilt generated classes
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }

# Keep data classes
-keep class com.instantledger.data.model.** { *; }
-keep class com.instantledger.data.entities.** { *; }

# Keep Room DAOs
-keep class com.instantledger.data.database.dao.** { *; }

# Keep Room entities
-keep @androidx.room.Entity class com.instantledger.data.database.entities.** { *; }

# Keep Compose classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# Keep Biometric classes
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# Keep Security Crypto classes
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# Keep Hilt ViewModel
-keep class com.instantledger.ui.**ViewModel { *; }
-keep class com.instantledger.ui.**ViewModel$* { *; }

# Keep application class
-keep class com.instantledger.InstantLedgerApplication { *; }

# Keep all Composables (needed for reflection)
-keep @androidx.compose.runtime.Composable class * { *; }

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Google Errorprone annotations (used by Tink/EncryptedSharedPreferences)
# These are compile-time annotations and not needed at runtime
-dontwarn com.google.errorprone.annotations.**
-keep class com.google.errorprone.annotations.** { *; }

# Tink (used by EncryptedSharedPreferences)
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Keep Tink Key classes
-keep class com.google.crypto.tink.proto.** { *; }
-keep class com.google.crypto.tink.aead.** { *; }
