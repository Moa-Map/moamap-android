package com.moamap.app.feature.explore.presentation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.moamap.app.R
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.explore.domain.model.Banner

/** 시안 「탐색탭_배너」: 높이 170, 모서리 16, 테두리 1 `Line/Alternative`. */
private val BannerHeight = 170.dp
private val BannerShape = RoundedCornerShape(16.dp)
private val BannerGap = 8.dp

/**
 * 홈 맨 위 배너 줄(백엔드 #132). 그림만 보여 준다 - 문구·버튼은 그림 안에 들어 있다.
 *
 * 한 장씩 맞춰 넘기고 다음 배너가 오른쪽에 살짝 보인다. 왼쪽 20 부터 사이 8, 마지막 배너 오른쪽도 20.
 *
 * 누르면 지도·공지로 가는 것은 백엔드 #169(이동 대상)가 생긴 뒤에 붙인다. 지금은 눌러도 아무 일이 없다.
 *
 * @param banners null 이면 아직 받는 중이다. 자리만 비워 둬 화면이 튀지 않게 한다. 비었으면(등록된 배너가
 *  없거나 못 받았으면) 기본 그림 한 장을 보여 준다(10-25 사용자 결정).
 */
@Composable
internal fun HomeBannerCarousel(
    banners: List<Banner>?,
    modifier: Modifier = Modifier,
) {
    if (banners == null) {
        Spacer(modifier.fillMaxWidth().height(BannerHeight))
        return
    }
    // 기본 그림은 주소 대신 그림 id 로 넘긴다. 이미지 로더가 둘 다 받는다.
    val images: List<Any> = banners.map { banner -> banner.imageUrl }.ifEmpty { listOf(R.drawable.img_home_banner_default) }
    val pagerState = rememberPagerState { images.size }

    HorizontalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxWidth()
            .height(BannerHeight),
        contentPadding = PaddingValues(horizontal = MoaMapDimens.ScreenHorizontalPadding),
        pageSpacing = BannerGap,
    ) { page ->
        AsyncImage(
            model = images[page],
            contentDescription = null,
            contentScale = ContentScale.Crop,
            // 그림을 못 받으면 기본 그림으로 메운다. 빈 칸만 남으면 고장 난 것처럼 보인다.
            error = painterResource(R.drawable.img_home_banner_default),
            modifier = Modifier
                .fillMaxWidth()
                .height(BannerHeight)
                .clip(BannerShape)
                .border(1.dp, MoaMapTheme.colors.lineAlternative, BannerShape),
        )
    }
}

@Preview(showBackground = true, widthDp = 393)
@Composable
private fun HomeBannerCarouselPreview() {
    MoaMapTheme {
        HomeBannerCarousel(banners = emptyList())
    }
}
