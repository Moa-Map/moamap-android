package com.moamap.app.feature.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.ActionMenuItem
import com.moamap.app.core.designsystem.component.ModalActionMenu
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/** 시안 홈 프로필 「모달창」 바탕색: #828586 의 60%. */
private val ProfileMenuScrim = MoaMapPrimitiveColors.Gray300.copy(alpha = 0.6f)

@Stable
internal class ProfileMenuState(
    initiallyVisible: Boolean = false,
) {
    var isVisible by mutableStateOf(initiallyVisible)
        private set

    fun show() {
        isVisible = true
    }

    fun dismiss() {
        isVisible = false
    }
}

@Composable
internal fun rememberProfileMenuState(): ProfileMenuState = remember { ProfileMenuState() }

/**
 * 홈 상단 프로필 아이콘을 누르면 뜨는 메뉴. 바탕이 [hazeState] 의 원본(홈 화면)을 흐린다 - [ModalActionMenu].
 */
@Composable
internal fun ProfileMenu(
    hazeState: HazeState,
    onProfileEditClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalActionMenu(
        items = listOf(
            ActionMenuItem(R.drawable.ic_person, "프로필", onProfileEditClick),
            ActionMenuItem(R.drawable.ic_settings, "설정", onSettingsClick),
        ),
        scrimColor = ProfileMenuScrim,
        hazeState = hazeState,
        modifier = modifier,
    )
}

@Preview(
    showBackground = true,
    widthDp = 393,
    heightDp = 852,
)
@Composable
private fun ProfileMenuPreview() {
    MoaMapTheme {
        val hazeState = rememberHazeState()
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .background(MoaMapTheme.colors.backgroundPrimary),
            )
            ProfileMenu(
                hazeState = hazeState,
                onProfileEditClick = {},
                onSettingsClick = {},
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-20).dp, y = 86.dp),
            )
        }
    }
}
