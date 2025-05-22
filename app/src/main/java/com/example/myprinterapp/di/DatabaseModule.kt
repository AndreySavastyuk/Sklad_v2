package com.example.myprinterapp.di

import android.content.Context
import androidx.room.Room
import com.example.myprinterapp.data.db.AppDatabase
import com.example.myprinterapp.data.db.PrintLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "printer.db"
        )
            // если хотите видеть *.json-схемы — укажите папку
            .addMigrations()          // migrations позже
            .build()

    @Provides
    fun providePrintLogDao(db: AppDatabase): PrintLogDao = db.printLogDao()
}
