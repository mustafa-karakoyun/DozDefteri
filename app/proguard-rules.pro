# DozDefteri R8 rules.
# Room, Hilt, WorkManager, Glance and Play Billing ship consumer rules in their AARs;
# the rules below cover what this app adds on top.

# kotlinx.serialization: keep generated serializers for backup DTOs and navigation routes.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class app.dozdefteri.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class app.dozdefteri.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class app.dozdefteri.**$$serializer { *; }

# Enums stored by name in Room and DataStore.
-keepclassmembers enum app.dozdefteri.domain.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Hilt workers are instantiated via the HiltWorkerFactory.
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
