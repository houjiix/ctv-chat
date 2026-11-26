package com.cinematv.app

import android.app.Application
import androidx.room.Room
import com.cinematv.app.data.CinemaDatabase
import com.cinematv.app.data.SettingsStore

class CinemaApplication : Application() {
    val database: CinemaDatabase by lazy {
        Room.databaseBuilder(this, CinemaDatabase::class.java, "cinema-tv.db").build()
    }

    val settings: SettingsStore by lazy { SettingsStore(this) }
}
