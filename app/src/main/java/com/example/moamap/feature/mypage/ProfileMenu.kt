package com.example.moamap.feature.mypage

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
import com.example.moamap.R
import com.example.moamap.core.designsystem.theme.MoaMapTheme

private val ProfileMenuCornerRadius = 16.dp

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

@Composable
internal fun ProfileMenu(
    onProfileEditClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfileMenuContent(
        onProfileEditClick = onProfileEditClick,
        onSettingsClick = onSettingsClick,
        modifier = modifier,
    )
}

@Composable
private fun ProfileMenuContent(
    onProfileEditClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfileActionMenu(
        firstIconRes = R.drawable.ic_person,
        firstLabel = "프로필",
        onFirstClick = onProfileEditClick,
        secondIconRes = R.drawable.ic_settings,
        secondLabel = "설정",
        onSecondClick = onSettingsClick,
        cornerRadius = ProfileMenuCornerRadius,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MoaMapTheme.colors.backgroundPrimary),
        ) {
            ProfileMenuContent(
                onProfileEditClick = {},
                onSettingsClick = {},
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-20).dp, y = 49.dp),
            )
        }
    }
}
