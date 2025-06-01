package com.example.myprinterapp

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

/**
 * Unit тесты, которые выполняются на локальной JVM.
 *
 * Смотрите [документацию по тестированию](http://d.android.com/tools/testing).
 */
@RunWith(MockitoJUnitRunner::class)
class ExampleUnitTest {
    
    @Before
    fun setup() {
        // Настройка перед каждым тестом
    }
    
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
    
    @Test
    fun testStringOperations() {
        val testString = "Hello World"
        assertNotNull("Строка не должна быть null", testString)
        assertTrue("Строка должна содержать 'Hello'", testString.contains("Hello"))
        assertEquals("Длина строки должна быть 11", 11, testString.length)
    }
    
    @Test
    fun testListOperations() {
        val list = mutableListOf<String>()
        assertTrue("Список должен быть пустым", list.isEmpty())
        
        list.add("item1")
        list.add("item2")
        
        assertEquals("Размер списка должен быть 2", 2, list.size)
        assertTrue("Список должен содержать item1", list.contains("item1"))
    }
}