package com.moamap.app.feature.explore.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight

/**
 * 홈 맨 위 운영자 추천 지도 카드 한 장.
 *
 * 운영자 추천 지도 API 가 아직 없어 [FeaturedMapMocks] 로 채운다(10-03 사용자 결정). API 가 생기면
 * 서버 응답에서 만들고, 누르면 그 지도로 가게 한다.
 *
 * @property image 사진. 서버 주소나 앱 안 그림 id - 이미지 로더가 둘 다 받는다.
 */
@Immutable
internal data class FeaturedMap(
    val description: String,
    val title: String,
    val tags: List<String>,
    val image: Any,
)

/** 시안 「메인 화면」 히어로에 들어 있는 내용 그대로의 임시 데이터. */
internal val FeaturedMapMocks: List<FeaturedMap> = List(3) {
    FeaturedMap(
        description = "운동 많이 된다...",
        title = "서울 필수 러닝 코스 추천 맵",
        tags = List(3) { "러닝맵 바로가기" },
        image = R.drawable.img_home_hero_mock,
    )
}

private val CardHeight = 150.dp
private val CardShape = RoundedCornerShape(16.dp)
private val CardGap = 8.dp

/** 글 묶음 위치·폭. 제목이 한 줄이어도 이 자리에서 시작한다(10-03 사용자 결정). */
private val TextStart = 12.dp
private val TextTop = 52.dp
private val TextWidth = 242.dp

/** 태그 칩 줄 위치. 태그가 없어도 다른 글은 그대로다. */
private val TagStart = 10.dp
private val TagTop = 116.dp
private val TagGap = 4.dp

/** 사진 위 덮개: 아래로 갈수록 어두워지는 그라데이션 + 전체 검정 8%. 시안 값. */
private val CardGradient = Brush.verticalGradient(
    0f to Color.Transparent,
    0.27f to Color.Transparent,
    0.56f to Color.Black.copy(alpha = 0.41f),
    0.81f to Color.Black.copy(alpha = 0.6f),
    1f to Color.Black.copy(alpha = 0.6f),
)
private val CardDim = Color.Black.copy(alpha = 0.08f)

/**
 * 운영자 추천 지도 카드 줄. 시안: 높이 150, 왼쪽 20 부터 카드 사이 8, 다음 카드가 오른쪽에 살짝 보인다.
 *
 * 한 장씩 맞춰 넘기고, 마지막 카드 오른쪽도 20 을 띄운다(10-03 사용자 결정). 카드 폭은 화면에서
 * 좌우 20 을 뺀 값이라 393 화면에서 시안의 353 이 된다.
 */
@Composable
internal fun FeaturedMapCarousel(
    maps: List<FeaturedMap>,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState { maps.size }

    HorizontalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxWidth()
            .height(CardHeight),
        contentPadding = PaddingValues(horizontal = MoaMapDimens.ScreenHorizontalPadding),
        pageSpacing = CardGap,
    ) { page ->
        FeaturedMapCard(map = maps[page])
    }
}

@Composable
private fun FeaturedMapCard(map: FeaturedMap) {
    // 시안 글 그림자 0 0 10 20%. 그림자 흐림은 픽셀 단위라 화면 밀도로 바꾼다.
    val titleStyle = FeaturedTitleStyle.copy(
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.2f),
            offset = Offset.Zero,
            blurRadius = with(LocalDensity.current) { 10.dp.toPx() },
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CardHeight)
            .clip(CardShape),
    ) {
        AsyncImage(
            model = map.image,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(CardGradient)
                .background(CardDim),
        )

        Column(
            modifier = Modifier
                .padding(start = TextStart, top = TextTop)
                .width(TextWidth),
        ) {
            listOf(map.description, map.title).forEach { line ->
                Text(
                    text = line,
                    style = titleStyle,
                    color = MoaMapPrimitiveColors.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            modifier = Modifier.padding(start = TagStart, top = TagTop),
            horizontalArrangement = Arrangement.spacedBy(TagGap),
        ) {
            map.tags.forEach { tag -> FeaturedTagChip(text = tag) }
        }
    }
}

/**
 * 운영자가 다는 태그 칩. 크기는 사용자가 피그마에서 읽어 준 값(10-03): 좌우 11.15, 위아래 4.46,
 * 높이 18.92. 글자 7.8 은 시안 그대로다.
 */
@Composable
private fun FeaturedTagChip(text: String) {
    ShadowedSurface(
        shape = RoundedCornerShape(percent = 50),
        shadowBlurRadius = 5.57.dp,
        shadowColor = Color.Black.copy(alpha = 0.08f),
    ) {
        Text(
            text = text,
            style = FeaturedTagStyle,
            color = MoaMapTheme.colors.textNormal,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 11.15.dp, vertical = 4.46.dp),
        )
    }
}

/** 시안 히어로 글: ExtraBold 22, 줄 높이 1.3, 자간 −0.02em. 앱 글꼴 단계에 22 가 없어 title2(20)에서 만든다. */
private val FeaturedTitleStyle
    @Composable get() = MoaMapTheme.typography.title2.copy(
        fontSize = 22.sp,
        lineHeight = (22 * 1.3).sp,
        letterSpacing = (-0.44).sp,
    ).withDesignLineHeight()

/** 시안 칩 글: Regular 7.8, 줄 높이 1.3, 자간 −0.02em. 칩 인스턴스가 축소된 크기 그대로다. */
private val FeaturedTagStyle
    @Composable get() = MoaMapTheme.typography.caption0.copy(
        fontSize = 7.8.sp,
        lineHeight = (7.8 * 1.3).sp,
        letterSpacing = (-0.156).sp,
    ).withDesignLineHeight()

@Preview(showBackground = true, widthDp = 393)
@Composable
private fun FeaturedMapCarouselPreview() {
    MoaMapTheme {
        FeaturedMapCarousel(maps = FeaturedMapMocks)
    }
}
