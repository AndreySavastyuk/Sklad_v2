package com.example.myprinterapp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.OffsetDateTime

@Entity(tableName = "print_log")
data class PrintLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateTime: OffsetDateTime,      // дата/время печати
    val labelType: String,             // «Приемка» / «Комплектация» / …
    val partNumber: String,
    val orderNumber: String?,
    val quantity: Int?,
    val cellCode: String?,
    val qrData: String                 // исходная строка QR
)