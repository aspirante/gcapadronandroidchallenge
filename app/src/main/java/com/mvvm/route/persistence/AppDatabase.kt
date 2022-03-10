package com.mvvm.route.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mvvm.route.data.model.Route
import com.mvvm.route.tools.DATABASE_NAME

@Database(entities = [Route::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
}

private lateinit var INSTANCE: AppDatabase

fun getDatabase(context: Context): AppDatabase {

    synchronized(AppDatabase::class.java) {
        if (!::INSTANCE.isInitialized) {
            INSTANCE = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            ).build()
        }
    }

    return INSTANCE
}