# Wird — R8 Rules
#
# Added after release build startup crash on Pixel 6 Pro:
# WorkManager initializes on app start via androidx.startup.InitializationProvider.
# It uses Room's WorkDatabase which is instantiated reflectively via Class.forName.
# Without keep rules, R8 renames or strips Room internal implementations and WorkManager classes.

# 1. Room Database reflection (used by WorkManager & Glance)
-keep class * extends androidx.room.RoomDatabase
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# 2. WorkManager internals and workers
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**
-keep class androidx.work.impl.** { *; }
-dontwarn androidx.work.impl.**
-keep class * extends androidx.work.Worker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# 3. Glance AppWidget (Task 10 home screen widget)
-keep class androidx.glance.** { *; }
-dontwarn androidx.glance.**

# 4. Native methods (whisper.cpp JNI bridge - Task 14)
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.mosman.wird.audio.** { *; }
