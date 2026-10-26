package com.moamap.app.feature.collection.presentation.placeimport

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.moamap.app.core.designsystem.component.MoaMapTitleTopBar
import com.moamap.app.core.designsystem.modifier.dismissKeyboardOnBackgroundTap
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.mapdetail.domain.model.PlaceCandidate
import com.moamap.app.feature.mapdetail.presentation.addplace.PlaceSearchContent
import com.moamap.app.feature.mapdetail.presentation.addplace.PlaceSearchState

/**
 * 영상에서 장소를 못 찾았을 때 「직접 장소 검색하기」. 지도 상세의 장소 추가와 같은 카카오 장소 검색 화면이다.
 * 고르면 영상에서 찾아 고른 것처럼 편집 → 지도 고르기로 이어진다.
 */
@Composable
internal fun PlaceImportSearchScreen(
    query: String,
    search: PlaceSearchState,
    onQueryChange: (String) -> Unit,
    onRetryClick: () -> Unit,
    onCandidateClick: (PlaceCandidate) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary)
            .dismissKeyboardOnBackgroundTap()
            .statusBarsPadding()
            .imePadding(),
    ) {
        MoaMapTitleTopBar(title = "장소 검색", onBackClick = onBackClick)
        PlaceSearchContent(
            query = query,
            search = search,
            onQueryChange = onQueryChange,
            onRetryClick = onRetryClick,
            onCandidateClick = onCandidateClick,
        )
    }
}
