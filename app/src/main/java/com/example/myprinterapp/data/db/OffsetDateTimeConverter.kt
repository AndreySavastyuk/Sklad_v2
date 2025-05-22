/* data/db/OffsetDateTimeConverter.kt */
package com.example.myprinterapp.data.db

import androidx.room.TypeConverter
import java.time.OffsetDateTime
import java.time.ZoneOffset

class OffsetDateTimeConverter {

    @TypeConverter
    fun toMillis(odt: OffsetDateTime?): Long? =
        odt?.toInstant()?.toEpochMilli()

    @TypeConverter
    fun fromMillis(value: Long?): OffsetDateTime? =
        value?.let { OffsetDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), ZoneOffset.UTC) }
}
