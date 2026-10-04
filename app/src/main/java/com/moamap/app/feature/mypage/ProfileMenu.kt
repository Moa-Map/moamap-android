package com.moamap.app.feature.mypage

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.R
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

private val ProfileMenuShape = RoundedCornerShape(12.dp)
private val ProfileMenuRowWidth = 164.dp
private val ProfileMenuRowHeight = 50.dp

/** 시안 「모달창」 바탕색: #828586 의 60%. */
private val ProfileMenuScrim = MoaMapPrimitiveColors.Gray300.copy(alpha = 0.6f)

/**
 * 바탕 한 겹: 뒤를 흐림 5 로 흐리고 회색을 덮는다. 흐림을 못 쓰는 기기(안드로이드 12 미만)는
 * 회색만 깐다. 시안에 없는 노이즈는 끈다.
 */
private val ProfileMenuBackdropStyle = HazeStyle(
    tint = HazeTint(ProfileMenuScrim),
    blurRadius = 5.dp,
    noiseFactor = 0f,
    fallbackTint = HazeTint(ProfileMenuScrim),
)

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
 * 홈 상단 프로필 아이콘을 누르면 뜨는 메뉴. 시안 「모달창」: 폭 172(줄 164 + 좌우 4), 모서리 12,
 * 줄 높이 50·안쪽 16, 흰 아이콘 20·글자 body2, 줄 사이 1px 선.
 *
 * 흐림은 바탕 한 겹이다. 바탕이 [hazeState] 의 원본(홈 화면)을 흐리고 회색을 덮는다. 시안의 줄에도
 * 흐림 10 이 걸려 있지만 줄에 칠한 색이 없어 피그마에서는 아무것도 그려지지 않는다 - 앱에서
 * 그대로 걸면 줄마다 유리판처럼 테두리가 생긴다(10-04 실기기에서 발견, 컴포넌트로 확인).
 */
@Composable
internal fun ProfileMenu(
    hazeState: HazeState,
    onProfileEditClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(ProfileMenuShape)
                .hazeEffect(hazeState, ProfileMenuBackdropStyle),
        )

        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
            ProfileMenuRow(
                iconRes = R.drawable.ic_person,
                label = "프로필",
                onClick = onProfileEditClick,
            )
            // 시안 구분선은 높이 0 인 1px 선이라 두 줄 사이에 자리를 차지하지 않는다.
            Box(
                modifier = Modifier
                    .width(ProfileMenuRowWidth)
                    .drawBehind {
                        drawLine(
                            color = MoaMapPrimitiveColors.LineNormal,
                            start = Offset.Zero,
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx(),
                        )
                    },
            )
            ProfileMenuRow(
                iconRes = R.drawable.ic_settings,
                label = "설정",
                onClick = onSettingsClick,
            )
        }
    }
}

@Composable
private fun ProfileMenuRow(
    @DrawableRes iconRes: Int,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .size(width = ProfileMenuRowWidth, height = ProfileMenuRowHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MoaMapPrimitiveColors.White,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MoaMapTheme.typography.body2,
            color = MoaMapPrimitiveColors.White,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = MoaMapPrimitiveColors.White,
            modifier = Modifier.size(20.dp),
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
