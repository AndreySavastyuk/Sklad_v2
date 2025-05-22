package com.example.myprinterapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PrintLogDao {

    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insert(entry: PrintLogEntry)

    @Query("SELECT * FROM print_log ORDER BY dateTime DESC")
    fun getAll(): Flow<List<PrintLogEntry>>

    @Query("DELETE FROM print_log")
    suspend fun clear()
}