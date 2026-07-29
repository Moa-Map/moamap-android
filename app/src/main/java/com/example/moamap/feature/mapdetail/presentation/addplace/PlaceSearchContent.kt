package com.example.moamap.feature.mapdetail.presentation.addplace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.moamap.R
import com.example.moamap.core.designsystem.component.CardShadowBlurRadius
import com.example.moamap.core.designsystem.component.CardShadowColor
import com.example.moamap.core.designsystem.component.ShadowedSurface
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import com.example.moamap.feature.mapdetail.domain.model.PlaceCandidate

private val SearchFieldShape = RoundedCornerShape(1000.dp)
private val ResultCardShape = RoundedCornerShape(12.dp)

/** 1단계. 카카오에서 장소를 찾아 고른다. */
@Composable
internal fun PlaceSearchContent(
    query: String,
    search: PlaceSearchState,
    onQueryChange: (String) -> Unit,
    onRetryClick: () -> Unit,
    onCandidateClick: (PlaceCandidate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "장소 추가",
            style = MoaMapTheme.typography.title3,
            color = MoaMapTheme.colors.textNormal,
            modifier = Modifier.padding(horizontal = AddPlaceHorizontalPadding),
        )

        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            modifier = Modifier.padding(
                start = AddPlaceHorizontalPadding,
                top = 20.dp,
                end = AddPlaceHorizontalPadding,
            ),
        )

        when (search) {
            PlaceSearchState.Idle -> SearchPlaceholder("장소를 검색해보세요")

            PlaceSearchState.Loading -> SearchPlaceholder { CircularProgressIndicator() }

            is PlaceSearchState.Error -> SearchPlaceholder {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = search.message,
                        style = MoaMapTheme.typography.body2,
                        color = MoaMapTheme.colors.textAssistive,
                    )
                    Text(
                        text = "다시 시도",
                        style = MoaMapTheme.typography.button2,
                        color = MoaMapTheme.colors.textNormal,
                        modifier = Modifier.clickable(onClick = onRetryClick),
                    )
                }
            }

            is PlaceSearchState.Success -> {
                if (search.candidates.isEmpty()) {
                    SearchPlaceholder("검색 결과가 없어요")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = AddPlaceHorizontalPadding,
                            top = 20.dp,
                            end = AddPlaceHorizontalPadding,
                            bottom = 32.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(search.candidates, key = PlaceCandidate::kakaoPlaceId) { candidate ->
                            PlaceCandidateCard(
                                candidate = candidate,
                                onClick = { onCandidateClick(candidate) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ShadowedSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = SearchFieldShape,
        shadowBlurRadius = CardShadowBlurRadius,
        shadowColor = CardShadowColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
                tint = MoaMapTheme.colors.textAssistive,
                modifier = Modifier.size(20.dp),
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MoaMapTheme.typography.body2.copy(
                    color = MoaMapTheme.colors.textNormal,
                ),
                cursorBrush = SolidColor(MoaMapPrimitiveColors.Blue500),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "추가하고 싶은 장소를 검색해주세요",
                            style = MoaMapTheme.typography.body2,
                            color = MoaMapTheme.colors.textAssistive,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                },
            )
        }
    }
}

/**
 * 검색 결과 카드.
 *
 * 썸네일은 늘 기본 이미지다. 카카오 키워드 검색은 사진 URL 을 주지 않는다.
 */
@Composable
private fun PlaceCandidateCard(
    candidate: PlaceCandidate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShadowedSurface(
        modifier = modifier.fillMaxWidth(),
        shape = ResultCardShape,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaceImagePlaceholder(size = 64.dp, cornerRadius = 4.dp)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = candidate.name,
                    style = MoaMapTheme.typography.subtitle2,
                    color = MoaMapTheme.colors.textNormal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (candidate.displayAddress.isNotEmpty()) {
                    Text(
                        text = candidate.displayAddress,
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

/** 사진이 없는 장소의 자리표시. 검색 결과와 등록 폼이 같은 모양을 쓴다. */
@Composable
internal fun PlaceImagePlaceholder(
    size: androidx.compose.ui.unit.Dp,
    cornerRadius: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MoaMapPrimitiveColors.Blue50),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_location),
            contentDescription = null,
            tint = MoaMapPrimitiveColors.Blue200,
            modifier = Modifier.size(size / 2.5f),
        )
    }
}

@Composable
private fun SearchPlaceholder(message: String) {
    SearchPlaceholder {
        Text(
            text = message,
            style = MoaMapTheme.typography.body2,
            color = MoaMapTheme.colors.textAssistive,
        )
    }
}

@Composable
private fun SearchPlaceholder(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private val PreviewCandidates = List(3) { index ->
    PlaceCandidate(
        kakaoPlaceId = "$index",
        name = "커피나무 ${index + 1}호점",
        address = "서울 동작구 상도동 ${index + 1}",
        roadAddress = "서울시 동작구 369",
        latitude = 37.4963,
        longitude = 126.9574,
        category = "음식점 > 카페",
        placeUrl = null,
    )
}

@Preview(showBackground = true, widthDp = 393, heightDp = 700)
@Composable
private fun PlaceSearchContentPreview() {
    MoaMapTheme {
        Box(modifier = Modifier.background(MoaMapTheme.colors.backgroundSecondary)) {
            PlaceSearchContent(
                query = "",
                search = PlaceSearchState.Success(PreviewCandidates),
                onQueryChange = {},
                onRetryClick = {},
                onCandidateClick = {},
            )
        }
    }
}
