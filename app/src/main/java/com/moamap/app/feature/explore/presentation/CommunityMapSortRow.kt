package com.moamap.app.feature.explore.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import com.moamap.app.feature.explore.domain.model.CommunityMapSort

/**
 * 인기순·최신순. 탐색 탭과 커뮤니티 지도 전체보기가 같이 쓴다.
 *
 * 시안: 왼쪽 정렬, 사이 8. 고른 쪽은 14 Bold 검정, 나머지는 14 Regular 회색.
 */
@Composable
internal fun CommunityMapSortRow(
    selected: CommunityMapSort,
    onClick: (CommunityMapSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CommunityMapSort.entries.forEach { sort ->
            val isSelected = sort == selected
            Text(
                text = sort.label,
                style = if (isSelected) {
                    MoaMapTheme.typography.button2
                } else {
                    MoaMapTheme.typography.button3
                }.withDesignLineHeight(),
                color = if (isSelected) {
                    MoaMapTheme.colors.textNormal
                } else {
                    MoaMapTheme.colors.textAssistive
                },
                maxLines = 1,
                modifier = Modifier.clickable(role = Role.Button) { onClick(sort) },
            )
        }
    }
}
