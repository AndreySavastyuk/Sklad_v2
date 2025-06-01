package com.example.myprinterapp.di

import android.content.Context
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ScannerModule {

    // Временно отключено - недостающие классы
    /*
    @Provides
    @Singleton
    fun provideBluetoothScannerService(
        @ApplicationContext context: Context
    ): BluetoothScannerService = BluetoothScannerService(context)

    @Provides
    @Singleton
    fun provideScannerDecoderService(): ScannerDecoderService = ScannerDecoderService()

    @Provides
    @Singleton
    fun provideNewlandBleService(
        @ApplicationContext context: Context
    ): NewlandBleService = NewlandBleService(context)
    */
}