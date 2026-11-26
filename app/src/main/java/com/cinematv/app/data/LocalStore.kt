package com.cinematv.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val kind: String,
    val posterUrl: String?,
    val summary: String,
    val year: String,
    val genres: String,
    val rating: String?,
    val detailsUrl: String?,
    val playbackUrl: String?,
    val licenceLabel: String?,
    val licenceUrl: String?,
    val creator: String?
)

@Entity(tableName = "watch_progress")
data class WatchProgressEntity(
    @PrimaryKey val id: String,
    val title: String,
    val posterUrl: String?,
    val playbackUrl: String,
    val licenceUrl: String?,
    val licenceLabel: String?,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long
)

@Dao
interface CinemaDao {
    @Query("SELECT * FROM favorites ORDER BY title COLLATE NOCASE")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)")
    suspend fun isFavorite(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(item: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun removeFavorite(id: String)

    @Query("SELECT * FROM watch_progress ORDER BY updatedAt DESC")
    fun observeWatchProgress(): Flow<List<WatchProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(item: WatchProgressEntity)

    @Query("DELETE FROM watch_progress WHERE id = :id")
    suspend fun removeProgress(id: String)

    @Query("DELETE FROM watch_progress WHERE positionMs >= durationMs - 15000")
    suspend fun clearCompletedProgress()
}

@Database(
    entities = [FavoriteEntity::class, WatchProgressEntity::class],
    version = 1,
    exportSchema = true
)
abstract class CinemaDatabase : RoomDatabase() {
    abstract fun cinemaDao(): CinemaDao
}

data class UserPreferences(
    val theme: String = "dark",
    val language: String = "auto",
    val liquidGlass: Boolean = false,
    val reducedAnimations: Boolean = true,
    val rememberPosition: Boolean = true
)

private val Context.settingsDataStore by preferencesDataStore(name = "cinema_settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val language = stringPreferencesKey("language")
        val liquidGlass = booleanPreferencesKey("liquid_glass")
        val reducedAnimations = booleanPreferencesKey("reduced_animations")
        val rememberPosition = booleanPreferencesKey("remember_position")
    }

    val preferences: Flow<UserPreferences> = context.settingsDataStore.data.map { values ->
        UserPreferences(
            theme = values[Keys.theme] ?: "dark",
            language = values[Keys.language] ?: "auto",
            liquidGlass = values[Keys.liquidGlass] ?: false,
            reducedAnimations = values[Keys.reducedAnimations] ?: true,
            rememberPosition = values[Keys.rememberPosition] ?: true
        )
    }

    suspend fun setTheme(value: String) {
        context.settingsDataStore.edit { it[Keys.theme] = value }
    }

    suspend fun setLanguage(value: String) {
        context.settingsDataStore.edit { it[Keys.language] = value }
    }

    suspend fun setLiquidGlass(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.liquidGlass] = value }
    }

    suspend fun setReducedAnimations(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.reducedAnimations] = value }
    }

    suspend fun setRememberPosition(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.rememberPosition] = value }
    }
}
