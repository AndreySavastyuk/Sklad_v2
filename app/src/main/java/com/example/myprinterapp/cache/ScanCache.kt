package com.example.myprinterapp.cache

import android.util.LruCache
import com.example.myprinterapp.ui.ParsedQrData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Кеш для результатов сканирования QR-кодов
 * Использует LRU алгоритм для управления памятью
 */
@Singleton
class ScanCache @Inject constructor() {
    private val cache = LruCache<String, ParsedQrData>(50)
    
    /**
     * Получение закешированного результата
     */
    fun get(qrCode: String): ParsedQrData? = cache.get(qrCode)
    
    /**
     * Сохранение результата в кеш
     */
    fun put(qrCode: String, data: ParsedQrData) {
        cache.put(qrCode, data)
    }
    
    /**
     * Очистка кеша
     */
    fun clear() = cache.evictAll()
    
    /**
     * Получение размера кеша
     */
    fun size(): Int = cache.size()
    
    /**
     * Получение максимального размера кеша
     */
    fun maxSize(): Int = cache.maxSize()
    
    /**
     * Проверка содержимого в кеше
     */
    fun contains(qrCode: String): Boolean = cache.get(qrCode) != null
} 