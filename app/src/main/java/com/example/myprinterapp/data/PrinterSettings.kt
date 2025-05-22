package com.example.myprinterapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrinterSettings @Inject constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    var printerMacAddress: String
        get() = prefs.getString(KEY_PRINTER_MAC, "") ?: ""
        set(value) = prefs.edit { putString(KEY_PRINTER_MAC, value) }

    var printerName: String
        get() = prefs.getString(KEY_PRINTER_NAME, "") ?: ""
        set(value) = prefs.edit { putString(KEY_PRINTER_NAME, value) }

    var printDensity: Int
        get() = prefs.getInt(KEY_PRINT_DENSITY, DEFAULT_DENSITY)
        set(value) = prefs.edit { putInt(KEY_PRINT_DENSITY, value) }

    var printSpeed: Float
        get() = prefs.getFloat(KEY_PRINT_SPEED, DEFAULT_SPEED)
        set(value) = prefs.edit { putFloat(KEY_PRINT_SPEED, value) }

    companion object {
        private const val PREFS_NAME = "printer_settings"
        private const val KEY_PRINTER_MAC = "printer_mac"
        private const val KEY_PRINTER_NAME = "printer_name"
        private const val KEY_PRINT_DENSITY = "print_density"
        private const val KEY_PRINT_SPEED = "print_speed"

        const val DEFAULT_DENSITY = 8
        const val DEFAULT_SPEED = 2.0f
    }
}
