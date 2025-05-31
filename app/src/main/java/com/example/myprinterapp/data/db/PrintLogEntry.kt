package com.example.myprinterapp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.OffsetDateTime

@Entity(tableName = "print_log")
data class PrintLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: OffsetDateTime,
    val operationType: String, // "ACCEPT", "PICK", "TEST"
    val partNumber: String,
    val partName: String,
    val quantity: Int,
    val location: String,
    val orderNumber: String? = null,
    val qrData: String,
    val printerStatus: String, // "SUCCESS", "FAILED"
    val errorMessage: String? = null,
    val userName: String? = null,
    val deviceId: String? = null
)