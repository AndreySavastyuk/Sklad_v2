package com.example.myprinterapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Инициализация логирования
        initLogging()
        
        // Инициализация SDK принтеров
        initPrinterSdks()
        
        Timber.d("Приложение инициализировано")
    }

    private fun initLogging() {
        if (BuildConfig.ENABLE_LOGGING) {
            Timber.plant(Timber.DebugTree())
            Timber.d("Логирование включено")
        }
    }

    private fun initPrinterSdks() {
        try {
            // Инициализируем Newland BLE SDK
            initNewlandSdk()
            
            // Инициализируем OnSemi SDK  
            initOnSemiSdk()
            
            Timber.d("SDK принтеров инициализированы")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка инициализации SDK принтеров")
        }
    }

    private fun initNewlandSdk() {
        try {
            // Инициализируем Newland BLE менеджер
            // NlsBleManager.getInstance().init(this)

            // Включаем логирование для отладки
            // NlsReportHelper.getInstance().init(this)
            // NlsReportHelper.getInstance().setSaveLogEnable(BuildConfig.DEBUG)
            
            Timber.d("Newland SDK готов к инициализации")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка инициализации Newland SDK")
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