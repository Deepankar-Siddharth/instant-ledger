# Consumer ProGuard rules - applied to dependencies
# This ensures SQLCipher classes are kept when this library is used as a dependency

-keep class net.zetetic.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }
