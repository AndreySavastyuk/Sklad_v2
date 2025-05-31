package com.example.myprinterapp

import android.app.Application
import com.nlscan.ble.NlsBleManager
import com.nlscan.ble.NlsReportHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Инициализация Newland BLE SDK
        initNewlandSdk()
    }

    private fun initNewlandSdk() {
        try {
            // Инициализируем BLE менеджер
            NlsBleManager.getInstance().init(this)

            // Включаем логирование для отладки
            NlsReportHelper.getInstance().init(this)
            NlsReportHelper.getInstance().setSaveLogEnable(BuildConfig.DEBUG)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}