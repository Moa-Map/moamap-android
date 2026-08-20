package com.moamap.app.core.designsystem.component

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * 실패 안내를 한 번만 띄우는 스낵바.
 *
 * [message] 가 null 이 아니게 되면 표시하고 곧바로 [onShown] 으로 상위 상태를 비운다.
 */
@Composable
fun ErrorSnackbar(
    message: String?,
    onShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hostState = remember { SnackbarHostState() }

    // 표시가 끝나기를 기다리는 동안 화면이 재구성되면 소비가 누락돼 같은 안내가 다시 뜬다.
    // 상위 상태는 먼저 비우고, 표시는 이 조각이 들고 있는 값으로 한다.
    var pending by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(message) {
        message?.let { arrived ->
            pending = arrived
            onShown()
        }
    }

    LaunchedEffect(pending) {
        pending?.let { shown ->
            hostState.showSnackbar(shown)
            pending = null
        }
    }

    SnackbarHost(hostState = hostState, modifier = modifier)
}
