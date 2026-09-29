package com.moamap.app.feature.explore.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moamap.app.core.designsystem.theme.MoaMapTheme

/**
 * 목록 자리에 로딩·오류·빈 상태를 같은 높이로 앉혀 화면이 튀지 않게 한다.
 *
 * 탐색 탭과 커뮤니티 지도 전체보기가 같이 쓴다. 두 화면 모두 이 상태들의 시안이 없다.
 */
@Composable
internal fun CommunityMapsPlaceholder(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
internal fun CommunityMapsError(
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
