package com.moamap.app.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.R
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import kotlinx.coroutines.delay

/** 토스트가 떠 있는 시간. */
private const val TOAST_DURATION_MILLIS = 2_000L

/** 시안 바탕 `#4A4F52` 70%. */
private val ToastBackground = MoaMapPrimitiveColors.Gray500.copy(alpha = 0.7f)
private val ToastShape = RoundedCornerShape(32.dp)

/**
 * 완료를 알리는 토스트. 시안 릴리즈 v1 「지도_장소카드/토스트 팝업」(`5061:47834`): 알약 모양 회색 70% 바탕에
 * 파란 원 체크(16) · 흰 caption0 글자, 안쪽 위아래 10·좌우 16.
 *
 * [ErrorSnackbar] 처럼 [message] 가 들어오면 곧바로 [onShown] 으로 상위 상태를 비우고, 들고 있는 값을
 * 잠깐 보였다가 저절로 지운다. 실패 안내는 [ErrorSnackbar] 그대로다 - 시안에 실패용 토스트가 없다.
 *
 * ponytail: 시안의 뒤 흐림(5)은 그리지 않는다. 글자 한 줄 크기라 흐림 바탕을 깔 값이 작다. 필요하면 haze 로.
 */
@Composable
fun MoaMapToast(
    message: String?,
    onShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var shown by remember { mutableStateOf<String?>(null) }
    // 사라지는 동안에도 글자가 남아 있어야 흐려지며 사라진다.
    var lastText by remember { mutableStateOf("") }

    LaunchedEffect(message) {
        message?.let { arrived ->
            shown = arrived
            lastText = arrived
            onShown()
        }
    }
    LaunchedEffect(shown) {
        if (shown != null) {
            delay(TOAST_DURATION_MILLIS)
            shown = null
        }
    }

    AnimatedVisibility(
        visible = shown != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        ToastPill(text = lastText)
    }
}

@Composable
private fun ToastPill(text: String) {
    Row(
        modifier = Modifier
            .background(ToastBackground, ToastShape)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            // 화면 읽기도 떠오르자마자 읽는다.
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .background(MoaMapPrimitiveColors.Blue500, CircleShape)
                .padding(3.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = MoaMapPrimitiveColors.White,
                modifier = Modifier.size(10.dp),
            )
        }
        Text(
            text = text,
            style = MoaMapTheme.typography.caption0.withDesignLineHeight(),
            color = MoaMapPrimitiveColors.White,
        )
    }
}

@Preview
@Composable
private fun MoaMapToastPreview() {
    MoaMapTheme {
        ToastPill(text = "지도에 참여했어요")
    }
}
