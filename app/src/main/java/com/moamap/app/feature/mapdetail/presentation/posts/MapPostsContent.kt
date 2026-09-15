package com.moamap.app.feature.mapdetail.presentation.posts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.core.designsystem.theme.MoaMapTheme

/** 탭바가 위에 겹쳐 있어 그만큼 내려서 시작한다. */
private val TabBarClearance = 90.dp

/**
 * 로그 탭 내용. 지도 멤버들이 남긴 게시물 자리다.
 *
 * 게시물 목록이 들어오기 전까지는 빈 상태 안내만 그린다. 목록이 비었을 때도 같은 안내를 쓴다.
 */
@Composable
internal fun MapPostsContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = TabBarClearance),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "아직 게시물이 없어요",
            style = MoaMapTheme.typography.body2,
            color = MoaMapTheme.colors.textAssistive,
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MapPostsContentPreview() {
    MoaMapTheme {
        MapPostsContent()
    }
}
