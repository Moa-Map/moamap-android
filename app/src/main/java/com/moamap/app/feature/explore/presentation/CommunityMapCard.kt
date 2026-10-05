package com.moamap.app.feature.explore.presentation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.R
import com.moamap.app.core.common.format.formatMemberCount
import com.moamap.app.core.common.format.formatPlaceCount
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.explore.domain.model.CommunityMap

private const val MAX_VISIBLE_HASHTAGS = 3

/** 사진 오른쪽 글 영역 높이. 사진(90)보다 6 작고 가운데에 선다. */
private val TextAreaHeight = 84.dp

/** 이름 줄 높이. 글자보다 커서 이름은 줄 가운데에 선다. */
private val TitleRowHeight = 24.dp

/** 메타 줄 규격. 시안의 "지도" 컴포넌트 값과 같다. */
private val MetaItemGap = 8.dp
private val MetaIconGap = 2.dp
private val MetaIconSize = 14.dp

/**
 * 탐색 탭의 커뮤니티 지도 목록 카드.
 */
@Composable
fun CommunityMapCard(
    map: CommunityMap,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 시안 「지도」 State=커뮤니티 지도: 모서리 12, 그림자 0 0 8 4%, 여백 12, 사진↔글 16.
    ShadowedSurface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MapThumbnail(imageUrl = map.imageUrl, size = 90.dp)

            // 글 영역은 높이 84(위아래 4)에 이름·태그는 위, 인원·장소 수는 아래 오른쪽.
            // 태그가 없으면 이름 아래가 비어 보여 이름을 가운데(= 사진 높이 가운데)에 둔다.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(TextAreaHeight)
                    .padding(vertical = 4.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(if (map.hashtags.isEmpty()) Alignment.CenterStart else Alignment.TopStart),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = map.title,
                        style = MoaMapTheme.typography.subtitle2,
                        color = MoaMapTheme.colors.textNormal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = TitleRowHeight)
                            .wrapContentHeight(Alignment.CenterVertically),
                    )
                    MapHashtagRow(
                        hashtags = map.hashtags,
                        maxVisible = MAX_VISIBLE_HASHTAGS,
                    )
                }
                Row(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    horizontalArrangement = Arrangement.spacedBy(MetaItemGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CommunityMapMeta(
                        iconRes = R.drawable.ic_person,
                        text = formatMemberCount(map.memberCount),
                        contentDescription = "참여 인원",
                    )
                    // 장소가 없어도 "0곳" 을 그린다. 줄을 숨기면 인원만 있는 카드와
                    // 섞여 어느 쪽이 0인지 알 수 없다.
                    CommunityMapMeta(
                        iconRes = R.drawable.ic_location,
                        text = formatPlaceCount(map.placeCount),
                        contentDescription = "등록 장소",
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunityMapMeta(
    @DrawableRes iconRes: Int,
    text: String,
    contentDescription: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MetaIconGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = MoaMapTheme.colors.textAssistive,
            modifier = Modifier.size(MetaIconSize),
        )
        Text(
            text = text,
            style = MoaMapTheme.typography.caption0,
            color = MoaMapTheme.colors.textAssistive,
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true, widthDp = 393)
@Composable
private fun CommunityMapCardPreview() {
    MoaMapTheme {
        CommunityMapCard(
            map = CommunityMap(
                id = 1L,
                title = "서울 팝업스토어 맵",
                imageUrl = null,
                hashtags = listOf("맛집", "데이트코스", "데이트", "카페"),
                memberCount = 2312,
                placeCount = 116,
                joined = false,
            ),
            onClick = {},
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 393)
@Composable
private fun CommunityMapCardNoHashtagsPreview() {
    MoaMapTheme {
        CommunityMapCard(
            map = CommunityMap(
                id = 2L,
                title = "태그 없는 지도",
                imageUrl = null,
                hashtags = emptyList(),
                memberCount = 12,
                placeCount = 3,
                joined = false,
            ),
            onClick = {},
            modifier = Modifier.padding(20.dp),
        )
    }
}
