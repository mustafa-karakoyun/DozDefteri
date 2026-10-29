package app.dozdefteri.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Onboarding

@Serializable
data object Today

@Serializable
data object Medications

@Serializable
data class MedicationEdit(val medicationId: Long? = null)

@Serializable
data object History

@Serializable
data object Settings

@Serializable
data object Export

@Serializable
data object Profiles

@Serializable
data object Pro

@Serializable
data object BatteryGuide
