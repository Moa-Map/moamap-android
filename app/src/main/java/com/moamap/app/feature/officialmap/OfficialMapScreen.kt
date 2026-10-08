package com.moamap.app.feature.officialmap

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.BelowAnchorPosition
import com.moamap.app.core.designsystem.component.MoaMapBackButton
import com.moamap.app.core.designsystem.component.MoaMapTooltip
import com.moamap.app.core.designsystem.component.MoaMapTopBarIconEdgePadding
import com.moamap.app.core.designsystem.component.PhotoThumbnail
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.officialmap.domain.model.OfficialMap
import com.moamap.app.feature.officialmap.presentation.OfficialMapViewModel
import com.moamap.app.feature.officialmap.presentation.OfficialMapsState

/** 시안 GNB 높이. */
private val TopBarHeight = 58.dp

/** ⓘ 말풍선 꼬리 끝이 ⓘ 아랫부분에 살짝 걸치도록 누르는 자리(48) 아래 끝에서 끌어올리는 거리. */
private val GuideTooltipOffsetY = (-10).dp

/** 꼬리가 ⓘ 가운데를 가리키도록 말풍선 오른쪽 끝에서 들이는 거리. */
private val GuideTooltipTailInset = 16.dp

private const val OFFICIAL_MAP_GUIDE = "모아맵이 공공데이터로 만든 지도예요.\n참여하면 모음 탭에서 바로 볼 수 있어요."

@Composable
fun OfficialMapScreen(
    onBackClick: () -> Unit = {},
    onMapClick: (OfficialMap) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: OfficialMapViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // 지도에 참여하거나 나가고 돌아오면 카드가 낡는다. 화면이 다시 보일 때 읽는다.
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    OfficialMapContent(
        state = state,
        onBackClick = onBackClick,
        onRetryClick = viewModel::retry,
        onMapClick = onMapClick,
        modifier = modifier,
    )
}

@Composable
private fun OfficialMapContent(
    state: OfficialMapsState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onMapClick: (OfficialMap) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundPrimary)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        OfficialMapTopBar(onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 카드 간격 8 과 합쳐 상단 바와 첫 카드 사이가 시안대로 12 가 된다.
            Spacer(Modifier.height(4.dp))

            when (state) {
                OfficialMapsState.Loading -> OfficialMapsPlaceholder {
                    CircularProgressIndicator()
                }

                is OfficialMapsState.Error -> OfficialMapsPlaceholder {
                    ErrorContent(message = state.message, onRetryClick = onRetryClick)
                }

                is OfficialMapsState.Success -> {
                    state.maps.forEach { officialMap ->
                        OfficialMapCard(
                            title = officialMap.title,
                            description = officialMap.description,
                            imageUrl = officialMap.imageUrl,
                            onClick = { onMapClick(officialMap) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

/**
 * 공식지도 카드. 시안 「지도」 State=공공 지도.
 *
 * 참여는 카드가 아니라 지도 소개·지도 화면에서 한다. 인원·장소 수도 시안에 없다.
 */
@Composable
private fun OfficialMapCard(
    title: String,
    description: String,
    imageUrl: String?,
    onClick: () -> Unit,
) {
    // 모서리 12, 그림자 0 0 8 4%, 여백 12, 사진↔글 16. 글은 위에서부터 쌓는다.
    ShadowedSurface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PhotoThumbnail(imageUrl = imageUrl, size = 90.dp)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.heightIn(min = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MoaMapTheme.typography.subtitle2,
                        color = MoaMapTheme.colors.textNormal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_verify_filled),
                        contentDescription = "공식 인증",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp),
                    )
                }
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        style = MoaMapTheme.typography.body2,
                        color = MoaMapTheme.colors.textAlternative,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** 목록 자리에 로딩·오류를 같은 높이로 앉혀 화면이 튀지 않게 한다. */
@Composable
private fun OfficialMapsPlaceholder(
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetryClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            style = MoaMapTheme.typography.body2,
            color = MoaMapTheme.colors.textAssistive,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "다시 시도",
            style = MoaMapTheme.typography.button2,
            color = MoaMapTheme.colors.textNormal,
            modifier = Modifier.clickable(onClick = onRetryClick),
        )
    }
}

/** 시안 GNB: 높이 58, 왼쪽 20 에 뒤로가기 32, 가운데 제목, 오른쪽 20 에 ⓘ 32. */
@Composable
private fun OfficialMapTopBar(
    onBackClick: () -> Unit,
) {
    var guideVisible by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val guidePosition = remember(density) {
        with(density) {
            BelowAnchorPosition(IntOffset(0, GuideTooltipOffsetY.roundToPx()), alignEnd = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TopBarHeight),
    ) {
        // 누르는 자리는 48 로 넓히고 아이콘이 양 끝에서 20 에 서도록 12 만 띄운다.
        MoaMapBackButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = MoaMapTopBarIconEdgePadding),
        )
        Text(
            text = "공식지도",
            style = MoaMapTheme.typography.title3,
            color = MoaMapTheme.colors.textNormal,
            modifier = Modifier.align(Alignment.Center),
        )
        // 말풍선은 ⓘ 누르는 자리를 기준으로 오른쪽 끝을 맞춰 뜬다.
        TopBarIconButton(
            iconRes = R.drawable.ic_info,
            contentDescription = "공식지도 안내",
            onClick = { guideVisible = !guideVisible },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
        ) {
            if (guideVisible) {
                Popup(
                    popupPositionProvider = guidePosition,
                    onDismissRequest = { guideVisible = false },
                    properties = PopupProperties(focusable = true),
                ) {
                    MoaMapTooltip(
                        tailAlignment = Alignment.End,
                        tailInset = GuideTooltipTailInset,
                    ) {
                        Text(
                            text = OFFICIAL_MAP_GUIDE,
                            style = MoaMapTheme.typography.caption0,
                            color = MoaMapTheme.colors.textWhite,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBarIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    popup: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = MoaMapTheme.colors.textNormal,
            modifier = Modifier.size(32.dp),
        )
        popup()
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun OfficialMapScreenPreview() {
    MoaMapTheme {
        OfficialMapContent(
            state = OfficialMapsState.Success(
                listOf(
                    OfficialMap(
                        id = 6L,
                        title = "화장실 위치",
                        description = "공공데이터 기반 공중화장실 위치를 한곳에 모은 지도",
                        imageUrl = null,
                        memberCount = 1,
                        placeCount = 5416,
                        joined = false,
                    ),
                ),
            ),
            onBackClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun OfficialMapScreenErrorPreview() {
    MoaMapTheme {
        OfficialMapContent(
            state = OfficialMapsState.Error("공식지도를 불러오지 못했어요"),
            onBackClick = {},
            onRetryClick = {},
        )
    }
}
