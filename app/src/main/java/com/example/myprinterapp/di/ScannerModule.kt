package com.example.myprinterapp.di

import android.content.Context
import com.example.myprinterapp.scanner.BluetoothScannerService
import com.example.myprinterapp.scanner.NewlandBleService
import com.example.myprinterapp.scanner.ScannerDecoderService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScannerModule {

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
}