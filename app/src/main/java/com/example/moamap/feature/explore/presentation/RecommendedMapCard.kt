package com.example.moamap.feature.explore.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.moamap.core.designsystem.component.ListCardShadowBlurRadius
import com.example.moamap.core.designsystem.component.ListCardShadowColor
import com.example.moamap.core.designsystem.component.ShadowedSurface
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import com.example.moamap.feature.explore.domain.model.CommunityMap

private const val MAX_VISIBLE_HASHTAGS = 2

/** 제목이 두 줄까지 늘어나도 카드 폭이 흔들리지 않도록 텍스트 영역을 고정한다. */
private val TEXT_COLUMN_WIDTH = 115.dp

/**
 * "추천 지도" 가로 스크롤 목록에 쓰이는 작은 카드.
 *
 * 참여 인원·장소 수 없이 대표 이미지와 제목, 해시태그만 보여준다.
 */
@Composable
fun RecommendedMapCard(
    map: CommunityMap,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShadowedSurface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        shadowBlurRadius = ListCardShadowBlurRadius,
        shadowColor = ListCardShadowColor,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MapThumbnail(imageUrl = map.imageUrl, size = 65.dp)

            Column(
                modifier = Modifier.width(TEXT_COLUMN_WIDTH),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = map.title,
                    style = MoaMapTheme.typography.subtitle4,
                    color = MoaMapTheme.colors.textNormal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                MapHashtagRow(
                    hashtags = map.hashtags,
                    maxVisible = MAX_VISIBLE_HASHTAGS,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393)
@Composable
private fun RecommendedMapCardPreview() {
    MoaMapTheme {
        RecommendedMapCard(
            map = CommunityMap(
                id = 1L,
                title = "서울 팝업스토어 맵이다",
                imageUrl = null,
                hashtags = listOf("맛집", "데이트코스", "데이트"),
                memberCount = 2312,
                placeCount = 116,
                joined = false,
            ),
            onClick = {},
            modifier = Modifier.padding(20.dp),
        )
    }
}
