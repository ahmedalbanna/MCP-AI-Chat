package com.aistudio.mcpchat.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aistudio.mcpchat.util.Product

@Composable
fun <T> StateHandler(
    state: Product<T>,
    onLoading: @Composable (cachedData: T?) -> Unit = {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            CircularProgressIndicator()
        }
    },
    onRetry: () -> Unit = {},
    onError: @Composable (message: String, onRetry: () -> Unit) -> Unit,
    onSuccess: @Composable (data: T) -> Unit,
) {
    when (state) {
        is Product.Loading -> onLoading(state.cachedData)
        is Product.Error -> onError(state.message, onRetry)
        is Product.Success -> onSuccess(state.data)
    }
}
