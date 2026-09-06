package com.aistudio.mcpchat.util

sealed class Product<out T> {
    data class Success<T>(val data: T) : Product<T>()
    data class Error<T>(val message: String, val exception: Throwable? = null) : Product<T>()
    data class Loading<T>(val cachedData: T? = null) : Product<T>()
}
