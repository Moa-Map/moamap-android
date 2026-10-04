package com.moamap.app.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moamap.app.core.designsystem.theme.MoaMapTheme

/** 상단 바 높이. 시안 GNB. */
private val TitleTopBarHeight = 58.dp

/** 제목이 길어도 뒤로가기와 겹치지 않게 양옆을 비운다(뒤로가기 칸 + 화면 끝 여백). */
private val TitleTopBarTitleHorizontalPadding = 72.dp

/**
 * 뒤로가기 + 가운데 제목만 있는 상단 바. 시안 GNB: 높이 58, 뒤로 32(끝에서 20), 가운데 제목 title3.
 * 약관 화면들과 문의하기가 쓴다.
 */
@Composable
internal fun MoaMapTitleTopBar(
    title: String,
    onBackClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TitleTopBarHeight),
    ) {
        MoaMapBackButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = MoaMapTopBarIconEdgePadding),
        )
        Text(
            text = title,
            style = MoaMapTheme.typography.title3,
            color = MoaMapTheme.colors.textNormal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = TitleTopBarTitleHorizontalPadding),
        )
    }
}
