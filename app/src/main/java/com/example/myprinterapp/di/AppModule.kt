package com.example.myprinterapp.di

import android.content.Context
import com.example.myprinterapp.data.PrinterSettings
import com.example.myprinterapp.data.repo.PrintLogRepository
import com.example.myprinterapp.printer.PrinterService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePrinterSettings(@ApplicationContext context: Context): PrinterSettings {
        return PrinterSettings(context)
    }

    @Provides
    @Singleton
    fun providePrinterService(
        @ApplicationContext context: Context,
        printRepo: PrintLogRepository
    ): PrinterService {
        return PrinterService(context, printRepo)
    }
}
