package com.example.myprinterapp

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Инструментальные тесты, которые выполняются на Android устройстве.
 *
 * Смотрите [документацию по тестированию](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Контекст приложения под тестом.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.myprinterapp.debug", appContext.packageName)
    }
    
    @Test
    fun testApplicationClass() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val app = appContext.applicationContext as MyApplication
        assertNotNull("Application должно быть инициализировано", app)
    }
}