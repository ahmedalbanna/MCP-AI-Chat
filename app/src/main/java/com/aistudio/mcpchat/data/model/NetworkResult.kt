package com.aistudio.mcpchat.data.model

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : NetworkResult<Nothing>()
    data object Loading : NetworkResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> default
    }

    fun <R> map(transform: (T) -> R): NetworkResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> this
    }
}

fun <T> NetworkResult<T>.getOrElse(onError: (String) -> T): T = when (this) {
    is NetworkResult.Success -> data
    is NetworkResult.Error -> onError(message)
    is NetworkResult.Loading -> onError("Loading")
}

fun <T> T.asSuccess(): NetworkResult<T> = NetworkResult.Success(this)
fun <T> String.asError(): NetworkResult<T> = NetworkResult.Error(this)
