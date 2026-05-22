# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson models (Hevy API DTOs)
-keep class com.jdluu.flexinsight.data.model.** { *; }
-keep class com.jdluu.flexinsight.data.api.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt / WorkManager
-keep class dagger.hilt.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker

# ML Kit GenAI
-keep class com.google.mlkit.genai.** { *; }
-dontwarn com.google.mlkit.**

# Preserve line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
