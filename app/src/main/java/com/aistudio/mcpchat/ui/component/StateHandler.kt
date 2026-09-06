package com.aistudio.mcpchat.ui.component

import androidx.compose.runtime.Composable
import com.aistudio.mcpchat.util.Product

@Composable
fun <T> StateHandler(
    state: Product<T>,
    onLoading: @Composable (cachedData: T?) -> Unit = { androidx.compose.material3.CircularProgressIndicator() },
    onError: @Composable (message: String, onRetry: () -> Unit) -> Unit,
    onSuccess: @Composable (data: T) -> Unit,
) {
    when (state) {
        is Product.Loading -> onLoading(state.cachedData)
        is Product.Error -> onError(state.message) { }
        is Product.Success -> onSuccess(state.data)
    }
}
