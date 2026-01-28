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
