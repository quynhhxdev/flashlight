package com.example.flashlight.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.flashlight.data.database.dao.TableTestDao
import com.example.flashlight.data.database.table.TableTest


@Database(entities = [TableTest::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        const val DATABASE_NAME = "ProjectBase"
    }

    abstract fun tableTestDao(): TableTestDao

}