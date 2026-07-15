package com.example.moamap.feature.mypage

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.moamap.R
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

private val ProfileMenuShape = RoundedCornerShape(16.dp)

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
    hazeState: HazeState,
    onProfileEditClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfileMenuContent(
        hazeState = hazeState,
        onProfileEditClick = onProfileEditClick,
        onSettingsClick = onSettingsClick,
        modifier = modifier,
    )
}

@Composable
private fun ProfileMenuContent(
    hazeState: HazeState,
    onProfileEditClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(172.dp)
            .height(100.dp),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(
                    radius = 5.dp,
                    edgeTreatment = BlurredEdgeTreatment.Unbounded,
                )
                .background(
                    color = MoaMapPrimitiveColors.Black.copy(alpha = 0.08f),
                    shape = ProfileMenuShape,
                ),
        )

        Column(
            modifier = Modifier
                .matchParentSize()
                .clip(ProfileMenuShape)
                .hazeChild(state = hazeState) {
                    backgroundColor = MoaMapPrimitiveColors.Blue100
                    blurRadius = 10.dp
                    noiseFactor = 0f
                    tints = listOf(
                        HazeTint(MoaMapPrimitiveColors.Blue700.copy(alpha = 0.5f)),
                    )
                    fallbackTint = HazeTint(
                        MoaMapPrimitiveColors.Blue700.copy(alpha = 0.5f),
                    )
                },
        ) {
            ProfileMenuItem(
                iconRes = R.drawable.ic_person,
                label = "프로필 편집",
                onClick = onProfileEditClick,
            )
            ProfileMenuItem(
                iconRes = R.drawable.ic_settings,
                label = "설정",
                onClick = onSettingsClick,
            )
        }
    }
}

@Composable
private fun ProfileMenuItem(
    @DrawableRes iconRes: Int,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MoaMapPrimitiveColors.White,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MoaMapTheme.typography.body2,
            color = MoaMapTheme.colors.textWhite,
        )
        Spacer(Modifier.weight(1f))
        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = MoaMapPrimitiveColors.White,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Preview(
    showBackground = true,
    widthDp = 393,
    heightDp = 852,
)
@Composable
private fun ProfileMenuPreview() {
    MoaMapTheme {
        val hazeState = remember { HazeState() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MoaMapTheme.colors.backgroundPrimary)
                .haze(state = hazeState)
        ) {
            ProfileMenuContent(
                hazeState = hazeState,
                onProfileEditClick = {},
                onSettingsClick = {},
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-20).dp, y = 49.dp),
            )
        }
    }
}
