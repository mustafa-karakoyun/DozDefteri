package app.dozdefteri.domain.model

enum class ScheduleType { DAILY, WEEKDAYS, EVERY_N_DAYS, AS_NEEDED }

enum class DoseStatus { PENDING, TAKEN, SNOOZED, SKIPPED, MISSED }

enum class MedicationForm { TABLET, CAPSULE, LIQUID, DROPS, INJECTION, INHALER, CREAM, OTHER }

enum class StockChangeReason { DOSE_TAKEN, MANUAL_ADD, MANUAL_SET, DOSE_UNDONE }

/** Statuses of a dose that still waits for the user's action. */
val OPEN_DOSE_STATUSES = setOf(DoseStatus.PENDING, DoseStatus.SNOOZED)
