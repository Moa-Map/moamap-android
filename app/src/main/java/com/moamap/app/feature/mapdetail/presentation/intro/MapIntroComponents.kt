package com.moamap.app.feature.mapdetail.presentation.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.mapdetail.domain.model.MapPlace

internal val MapIntroHeroHeight = 295.dp
internal val MapIntroMapHeight = 236.dp

private val TagShape = RoundedCornerShape(1000.dp)
private val PlaceThumbnailShape = RoundedCornerShape(4.dp)

/**
 * 히어로.
 *
 * 대표 이미지 위에 아래로 갈수록 짙어지는 그라데이션을 덮어 흰 글자가 읽히게 한다.
 * 이미지가 없어도 같은 그라데이션을 쓴다 - 글자 위치가 이미지 유무로 달라지면 안 된다.
 */
@Composable
internal fun MapIntroHero(
    title: String,
    ownerName: String?,
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MapIntroHeroHeight)
            .background(MoaMapPrimitiveColors.Gray50),
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF666666)),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MoaMapTheme.typography.title1,
                color = MoaMapTheme.colors.textWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            // 이름을 못 받으면 아이콘만 남아 더 어색하다. 줄을 통째로 숨긴다.
            if (ownerName != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_person),
                        contentDescription = "제작자",
                        tint = MoaMapTheme.colors.textWhite,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = ownerName,
                        style = MoaMapTheme.typography.body3,
                        color = MoaMapTheme.colors.textWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** 섹션 제목. 본문의 세 섹션이 같은 모양을 쓴다. */
@Composable
internal fun MapIntroSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MoaMapTheme.typography.title3,
        color = MoaMapTheme.colors.textNormal,
        modifier = modifier,
    )
}

/** 섹션 사이 구분선. */
@Composable
internal fun MapIntroDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MoaMapPrimitiveColors.Gray50),
    )
}

/**
 * 태그 칩 한 줄.
 *
 * 태그 개수가 지도마다 달라 줄을 넘길 수 있다. 줄바꿈 대신 가로 스크롤로 흘려보낸다.
 */
@Composable
internal fun MapIntroTagRow(tags: List<String>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tags.forEach { tag ->
            Text(
                text = tag,
                style = MoaMapTheme.typography.caption0,
                color = MoaMapPrimitiveColors.Blue900,
                maxLines = 1,
                modifier = Modifier
                    .background(color = MoaMapPrimitiveColors.Blue50, shape = TagShape)
                    .border(width = 1.dp, color = MoaMapPrimitiveColors.Blue500, shape = TagShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

/** 설명 화면의 장소 카드. 썸네일 · 이름 · 주소만 있는 가벼운 줄이다. */
@Composable
internal fun MapIntroPlaceItem(
    place: MapPlace,
    modifier: Modifier = Modifier,
) {
    ShadowedSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(PlaceThumbnailShape)
                    .background(MoaMapPrimitiveColors.Blue50),
            ) {
                if (place.photoUrl != null) {
                    AsyncImage(
                        model = place.photoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = place.name,
                    style = MoaMapTheme.typography.subtitle2,
                    color = MoaMapTheme.colors.textNormal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (place.address.isNotEmpty()) {
                    Text(
                        text = place.address,
                        style = MoaMapTheme.typography.caption0,
                        color = MoaMapTheme.colors.textNormal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * 장소가 더 있을 때만 뜨는 줄.
 *
 * 전체 장소 목록 화면이 아직 없어 미리보기와 같이 상세로 보낸다.
 */
@Composable
internal fun MapIntroMoreLink(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = "더보기",
        style = MoaMapTheme.typography.caption0,
        color = MoaMapPrimitiveColors.Gray300,
        textDecoration = TextDecoration.Underline,
        modifier = modifier.clickable(onClick = onClick),
    )
}
