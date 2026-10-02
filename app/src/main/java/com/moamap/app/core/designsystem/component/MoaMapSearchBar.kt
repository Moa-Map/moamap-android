package com.moamap.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.R
import com.moamap.app.core.designsystem.theme.MoaMapTheme

/**
 * 검색창 껍데기. 시안 「검색창」(`1238:953`).
 *
 * 높이 44, 흰 바탕, 모서리 12, 그림자 `0 0 8 4%`, 안쪽 좌우 16, 회색 돋보기 20 다음 4 를 띄우고
 * [content] 를 둔다. 안내 글자는 body2 `textAssistive`, 입력한 글자는 body2 `textNormal` 로
 * 채운다 - 시안의 기본·입력 중 상태다.
 */
@Composable
fun MoaMapSearchBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    // 모양·그림자는 ShadowedSurface 기본값(모서리 12, 카드 그림자)이 시안과 같다.
    ShadowedSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
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
            content()
        }
    }
}

@Preview(showBackground = true, widthDp = 393)
@Composable
private fun MoaMapSearchBarPreview() {
    MoaMapTheme {
        MoaMapSearchBar(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "장소를 검색해보세요",
                style = MoaMapTheme.typography.body2,
                color = MoaMapTheme.colors.textAssistive,
            )
        }
    }
}
