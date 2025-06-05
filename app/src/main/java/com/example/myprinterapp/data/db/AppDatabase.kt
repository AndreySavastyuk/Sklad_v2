/* data/db/AppDatabase.kt */
package com.example.myprinterapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        PrintLogEntry::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(OffsetDateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun printLogDao(): PrintLogDao
}
