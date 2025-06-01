package com.example.myprinterapp.domain.usecase

import com.example.myprinterapp.data.models.Result
import com.example.myprinterapp.data.models.UiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Базовый класс для всех Use Cases
 */
abstract class BaseUseCase<in P, R>(
    private val dispatcher: CoroutineDispatcher
) {
    /**
     * Выполнение Use Case с обработкой ошибок
     */
    suspend operator fun invoke(parameters: P): Result<R> {
        return try {
            withContext(dispatcher) {
                execute(parameters).let {
                    Result.Success(it)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in ${this::class.simpleName}")
            Result.Error(e.message ?: "Unknown error", e)
        }
    }

    @Throws(RuntimeException::class)
    protected abstract suspend fun execute(parameters: P): R
}

/**
 * Базовый класс для Flow Use Cases
 */
abstract class FlowUseCase<in P, R>(
    private val dispatcher: CoroutineDispatcher
) {
    operator fun invoke(parameters: P): Flow<R> {
        return execute(parameters)
    }

    protected abstract fun execute(parameters: P): Flow<R>
}

/**
 * Use Case без параметров
 */
abstract class NoParamsUseCase<R>(
    private val dispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): Result<R> {
        return try {
            withContext(dispatcher) {
                execute().let {
                    Result.Success(it)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in ${this::class.simpleName}")
            Result.Error(e.message ?: "Unknown error", e)
        }
    }

    @Throws(RuntimeException::class)
    protected abstract suspend fun execute(): R
}

/**
 * Flow Use Case без параметров
 */
abstract class NoParamsFlowUseCase<R>(
    private val dispatcher: CoroutineDispatcher
) {
    operator fun invoke(): Flow<R> {
        return execute()
    }

    protected abstract fun execute(): Flow<R>
} 