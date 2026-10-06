package com.moamap.app.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moamap.app.R
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

private val ActionMenuWidth = 172.dp
private val ActionMenuRowHeight = 50.dp

/** [ActionMenu] 한 줄. */
@Immutable
internal data class ActionMenuItem(
    @DrawableRes val iconRes: Int,
    val label: String,
    val onClick: () -> Unit,
)

private val ModalMenuShape = RoundedCornerShape(12.dp)
private val ModalMenuRowWidth = 164.dp

/**
 * 흐린 회색 바탕의 줄 메뉴. 시안 「모달창」(폭 172 = 줄 164 + 좌우 4, 모서리 12, 줄 높이 50·안쪽 16,
 * 흰 아이콘 20·글자 body2·오른쪽 화살표, 줄 사이 1px 선). 홈 프로필 메뉴와 지도 상세 메뉴가 쓴다.
 *
 * 바탕은 한 겹이다. [hazeState] 의 원본을 흐리고 [scrimColor] 를 덮는다. [hazeState] 가 없으면 색만
 * 깐다 - 지도 상세의 지도는 SurfaceView 라 Compose 흐림이 잡지 못한다. 시안의 줄에도 흐림이 걸려
 * 있지만 줄에 칠한 색이 없어 피그마에서는 아무것도 그려지지 않는다 - 앱에서 그대로 걸면 줄마다
 * 유리판처럼 테두리가 생긴다(10-04 실기기에서 발견).
 *
 * 흐림은 시안 값 5 대신 10 이다. Haze 에 5 를 주면 시안보다 덜 흐려 밑의 글자가 읽힌다. 문의 유형
 * 목록에서 시안 그림과 나란히 놓고 맞춰 봤다(10-05).
 */
@Composable
internal fun ModalActionMenu(
    items: List<ActionMenuItem>,
    scrimColor: Color,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(ModalMenuShape)
                .then(
                    if (hazeState == null) {
                        Modifier.background(scrimColor)
                    } else {
                        Modifier.hazeEffect(
                            hazeState,
                            HazeStyle(
                                tint = HazeTint(scrimColor),
                                blurRadius = 10.dp,
                                // 시안에 없는 노이즈는 끈다.
                                noiseFactor = 0f,
                                fallbackTint = HazeTint(scrimColor),
                            ),
                        ) {
                            // Haze 는 안드로이드 12 미만에서 흐림을 끄고 회색만 깐다. 그러면 밑의 글자가 메뉴
                            // 글자와 겹쳐 읽히지 않아(10-05 에뮬레이터 API 30 에서 발견), 그 기기에서도 흐린다.
                            blurEnabled = true
                        }
                    },
                ),
        )

        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    // 시안 구분선은 높이 0 인 1px 선이라 두 줄 사이에 자리를 차지하지 않는다.
                    Box(
                        modifier = Modifier
                            .width(ModalMenuRowWidth)
                            .drawBehind {
                                drawLine(
                                    color = MoaMapPrimitiveColors.LineNormal,
                                    start = Offset.Zero,
                                    end = Offset(size.width, 0f),
                                    strokeWidth = 1.dp.toPx(),
                                )
                            },
                    )
                }
                ModalActionMenuRow(item)
            }
        }
    }
}

@Composable
private fun ModalActionMenuRow(item: ActionMenuItem) {
    Row(
        modifier = Modifier
            .size(width = ModalMenuRowWidth, height = ActionMenuRowHeight)
            .clickable(onClick = item.onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(item.iconRes),
            contentDescription = null,
            tint = MoaMapPrimitiveColors.White,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = item.label,
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

/** 줄 사이에 구분선을 넣는 흰 바탕 팝업 메뉴. 사진 소스 선택이 쓴다(프로필·지도 메뉴는 [ModalActionMenu]). */
@Composable
internal fun ActionMenu(
    items: List<ActionMenuItem>,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(cornerRadius)

    // 높이는 줄 수에 따라 늘어나므로 고정하지 않고 Column 이 정하게 둔다.
    Box(modifier = modifier.width(ActionMenuWidth)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .compatibleShadow(
                    shape = shape,
                    blurRadius = 5.dp,
                    color = MoaMapPrimitiveColors.Black.copy(alpha = 0.12f),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MoaMapTheme.colors.backgroundSecondary)
                .border(
                    width = 1.dp,
                    color = MoaMapPrimitiveColors.Blue600,
                    shape = shape,
                )
                .padding(horizontal = 4.dp),
        ) {
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MoaMapTheme.colors.lineNormal,
                    )
                }
                ActionMenuRow(
                    iconRes = item.iconRes,
                    label = item.label,
                    onClick = item.onClick,
                )
            }
        }
    }
}

@Composable
private fun ActionMenuRow(
    @DrawableRes iconRes: Int,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ActionMenuRowHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MoaMapTheme.colors.textNormal,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MoaMapTheme.typography.body2,
            color = MoaMapTheme.colors.textNormal,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = MoaMapTheme.colors.textNormal,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** 사진을 어디서 가져올지 고르는 메뉴. */
@Composable
internal fun ImageSourceMenu(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ActionMenu(
        items = listOf(
            ActionMenuItem(R.drawable.ic_photo_camera, "카메라", onCameraClick),
            ActionMenuItem(R.drawable.ic_gallery, "갤러리", onGalleryClick),
        ),
        cornerRadius = 12.dp,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun ImageSourceMenuPreview() {
    MoaMapTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MoaMapTheme.colors.backgroundSecondary),
            contentAlignment = Alignment.Center,
        ) {
            ImageSourceMenu(
                onCameraClick = {},
                onGalleryClick = {},
            )
        }
    }
}
