package com.example.myprinterapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import com.nlscan.ble.NlsBleManager
import com.nlscan.ble.NlsReportHelper

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Инициализация логирования
        initLogging()
        
        // Инициализация SDK принтеров и сканеров
        initSdks()
        
        Timber.d("Приложение инициализировано")
    }

    private fun initLogging() {
        if (BuildConfig.ENABLE_LOGGING) {
            Timber.plant(Timber.DebugTree())
            Timber.d("Логирование включено")
        }
    }

    private fun initSdks() {
        try {
            // Инициализируем Newland BLE SDK для сканеров
            initNewlandSdk()
            
            // Инициализируем OnSemi SDK  
            initOnSemiSdk()
            
            Timber.d("SDK принтеров и сканеров инициализированы")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка инициализации SDK")
        }
    }

    private fun initNewlandSdk() {
        try {
            // Инициализируем Newland BLE SDK (обязательно)
            NlsBleManager.getInstance().init(this)
            
            // Включаем сохранение логов (опционально, но рекомендуется при отладке)
            NlsReportHelper.getInstance().init(this)
            NlsReportHelper.getInstance().setSaveLogEnable(BuildConfig.ENABLE_LOGGING)
            
            Timber.d("Newland BLE SDK инициализирован")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка инициализации Newland BLE SDK")
        }
    }
    
    private fun initOnSemiSdk() {
        try {
            // Инициализация OnSemi BLE библиотеки
            // Конкретная инициализация зависит от API библиотеки
            
            Timber.d("OnSemi SDK готов к инициализации")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка инициализации OnSemi SDK")
        }
    }
}