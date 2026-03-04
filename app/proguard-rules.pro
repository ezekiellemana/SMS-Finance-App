# Keep Room entities
-keep class com.smsfinance.data.entity.** { *; }
-keep class com.smsfinance.data.dao.** { *; }

# Keep Hilt generated code
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Kotlin data classes
-keepclassmembers class ** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# SQLCipher
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# Biometric
-keep class androidx.biometric.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Apache POI (Excel)
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**
-keep class org.openxmlformats.schemas.** { *; }
-dontwarn org.openxmlformats.**

# iText PDF
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# Google Drive API
-keep class com.google.api.** { *; }
-keep class com.google.apis.** { *; }
-dontwarn com.google.api.**
-dontwarn com.google.apis.**

# Google Sign-In
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
