package com.moamap.app.feature.mapdetail.presentation.addplace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.core.designsystem.component.MoaMapSearchBar
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.mapdetail.domain.model.PlaceCandidate

/** 제목·안내 글이 검색창보다 안쪽으로 들어간 만큼(시안 4). */
private val IntroStartInset = 4.dp

/** 검색창 아래 검색 예시까지(시안 1번 화면). 디자인이 나중에 고친다고 했다(10-09). */
private val SearchHintTopGap = 86.dp

private const val SEARCH_HINT_EXAMPLES =
    "- 도로명 + 건물번호 (위례성대로 2)\n- 건물명 + 번지 (방이동 44-2)\n- 건물명, 학교명 (반포 자이, 숭실대학교)"

/**
 * 장소 추가 1단계. 카카오에서 장소를 찾아 고른다. 시안 「상세지도/장소추가」(검색 `4070:37922`, 결과
 * `4070:38652`).
 *
 * 검색 전에는 검색창 아래에 검색 예시를, 결과가 오면 그 자리에 이름·주소 줄을 모은 카드 하나를
 * 띄운다. 카카오 키워드 검색은 사진을 주지 않아 결과에 사진 칸이 없다.
 */
@Composable
internal fun PlaceSearchContent(
    query: String,
    search: PlaceSearchState,
    onQueryChange: (String) -> Unit,
    onRetryClick: () -> Unit,
    onCandidateClick: (PlaceCandidate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AddPlaceHorizontalPadding),
    ) {
        Spacer(Modifier.height(20.dp))
        Column(
            modifier = Modifier.padding(start = IntroStartInset),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "어떤 장소를 추가할까요?",
                style = MoaMapTheme.typography.subtitle1,
                color = MoaMapTheme.colors.textNormal,
            )
            Text(
                text = "장소나 도로명으로 검색해보세요",
                style = MoaMapTheme.typography.body2,
                color = MoaMapTheme.colors.textNormal,
            )
        }
        Spacer(Modifier.height(20.dp))

        SearchField(query = query, onQueryChange = onQueryChange)

        when (search) {
            PlaceSearchState.Idle -> SearchHint(
                modifier = Modifier.padding(start = IntroStartInset, top = SearchHintTopGap),
            )

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
                    Spacer(Modifier.height(4.dp))
                    CandidateListCard(candidates = search.candidates, onCandidateClick = onCandidateClick)
                    Spacer(Modifier.height(32.dp))
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
    MoaMapSearchBar(modifier = modifier) {
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

/**
 * 검색 예시. 시안은 Pretendard Medium 14 인데 앱에 그 굵기가 없고 디자인을 나중에 고친다고 해서
 * 본문 글꼴·회색으로 둔다(10-09).
 */
@Composable
private fun SearchHint(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = "이렇게 검색해보세요",
            style = MoaMapTheme.typography.body2,
            color = MoaMapTheme.colors.textAlternative,
        )
        Text(
            text = SEARCH_HINT_EXAMPLES,
            style = MoaMapTheme.typography.body2,
            color = MoaMapTheme.colors.textAssistive,
        )
    }
}

/** 검색 결과. 흰 카드 하나에 이름·주소 줄을 구분선으로 나눠 모은다(시안 「추가 전_장소카드」). */
@Composable
private fun CandidateListCard(
    candidates: List<PlaceCandidate>,
    onCandidateClick: (PlaceCandidate) -> Unit,
) {
    ShadowedSurface(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MoaMapTheme.colors.lineAlternative),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            candidates.forEachIndexed { index, candidate ->
                if (index > 0) {
                    HorizontalDivider(thickness = 1.dp, color = MoaMapTheme.colors.lineAlternative)
                }
                CandidateRow(candidate = candidate, onClick = { onCandidateClick(candidate) })
            }
        }
    }
}

@Composable
private fun CandidateRow(
    candidate: PlaceCandidate,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
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
                query = "커피",
                search = PlaceSearchState.Success(PreviewCandidates),
                onQueryChange = {},
                onRetryClick = {},
                onCandidateClick = {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 700)
@Composable
private fun PlaceSearchContentIdlePreview() {
    MoaMapTheme {
        Box(modifier = Modifier.background(MoaMapTheme.colors.backgroundSecondary)) {
            PlaceSearchContent(
                query = "",
                search = PlaceSearchState.Idle,
                onQueryChange = {},
                onRetryClick = {},
                onCandidateClick = {},
            )
        }
    }
}
