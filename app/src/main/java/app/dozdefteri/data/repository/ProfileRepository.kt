package app.dozdefteri.data.repository

import androidx.room.withTransaction
import app.dozdefteri.data.db.AppDatabase
import app.dozdefteri.data.db.entity.ProfileEntity
import app.dozdefteri.data.prefs.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val db: AppDatabase,
    private val settingsRepository: SettingsRepository
) {
    private val profileDao = db.profileDao()
    private val doseDao = db.doseEventDao()

    val profiles: Flow<List<ProfileEntity>> = profileDao.observeAll()

    /** The active profile; falls back to the first profile when the stored id is missing or stale. */
    val activeProfile: Flow<ProfileEntity?> =
        combine(profiles, settingsRepository.settings) { all, settings ->
            all.firstOrNull { it.id == settings.activeProfileId } ?: all.firstOrNull()
        }.distinctUntilChanged()

    suspend fun getAll(): List<ProfileEntity> = profileDao.getAll()

    suspend fun getActiveProfile(): ProfileEntity? {
        val all = profileDao.getAll()
        val activeId = settingsRepository.current().activeProfileId
        return all.firstOrNull { it.id == activeId } ?: all.firstOrNull()
    }

    suspend fun count(): Int = profileDao.count()

    /** Creates the default profile on first launch when the database has none (plan 3.3). */
    suspend fun ensureDefaultProfile(defaultName: String, now: Instant) {
        val existing = profileDao.getAll()
        if (existing.isEmpty()) {
            val id = profileDao.insert(ProfileEntity(name = defaultName, colorIndex = 0, createdAt = now.toEpochMilli()))
            settingsRepository.setActiveProfileId(id)
        } else if (existing.none { it.id == settingsRepository.current().activeProfileId }) {
            settingsRepository.setActiveProfileId(existing.first().id)
        }
    }

    suspend fun add(name: String, colorIndex: Int, now: Instant): Long =
        profileDao.insert(ProfileEntity(name = name.trim(), colorIndex = colorIndex, createdAt = now.toEpochMilli()))

    suspend fun update(profile: ProfileEntity) = profileDao.update(profile.copy(name = profile.name.trim()))

    suspend fun setActive(profileId: Long) = settingsRepository.setActiveProfileId(profileId)

    /**
     * Deletes the profile and, through foreign-key cascades, all of its medications, schedules, doses and stock logs.
     * The last profile cannot be deleted. Returns the ids of the profile's shown dose notifications, or `null`
     * when the profile was not deleted.
     */
    suspend fun delete(profile: ProfileEntity): List<Long>? {
        val notified = db.withTransaction {
            if (profileDao.count() <= 1) return@withTransaction null
            val ids = doseDao.getNotifiedOpenIdsForProfile(profile.id)
            profileDao.delete(profile)
            ids
        } ?: return null
        val remaining = profileDao.getAll()
        if (remaining.none { it.id == settingsRepository.current().activeProfileId }) {
            remaining.firstOrNull()?.let { settingsRepository.setActiveProfileId(it.id) }
        }
        return notified
    }
}
