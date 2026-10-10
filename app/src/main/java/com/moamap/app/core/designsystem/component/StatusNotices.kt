package com.moamap.app.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.core.designsystem.theme.MoaMapTheme

// 목록을 불러오는 중·못 불러옴·비어 있음 안내. 시안이 없는 상태라 앱 전체가 이 모양 하나를 쓴다.

/**
 * 목록 자리를 높이 200 으로 채우는 상자. 탐색·커뮤니티 지도 전체보기·공식지도·모음 탭이 쓴다.
 *
 * 세 상태가 같은 높이를 차지해 상태가 바뀌어도 화면이 튀지 않는다.
 */
@Composable
internal fun MoaMapListPlaceholder(
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

/** 목록 자리를 위아래 60 으로 채우는 상자. 지도 상세 안의 목록(게시물·멤버·활동 기록·댓글)이 쓴다. */
@Composable
internal fun MoaMapCenteredNotice(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** 목록 안의 작은 로딩 표시. */
@Composable
internal fun MoaMapLoadingIndicator(modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        color = MoaMapTheme.colors.textAssistive,
        strokeWidth = 2.dp,
        modifier = modifier.size(24.dp),
    )
}

/**
 * 「다시 시도」 글자 버튼. 앱 전체가 이 모양 하나를 쓴다(10-10 사용자 결정).
 *
 * @param onDark 어두운 바탕 위에서 흰 글자로 그릴지.
 */
@Composable
internal fun MoaMapRetryText(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
) {
    Text(
        text = "다시 시도",
        style = MoaMapTheme.typography.button2,
        color = if (onDark) MoaMapTheme.colors.textWhite else MoaMapTheme.colors.textNormal,
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
    )
}

/**
 * 불러오지 못했을 때의 안내. 회색 안내 글 아래 12 띄워 [MoaMapRetryText].
 *
 * @param onDark 어두운 바탕(게시물 상세의 검은 막) 위라 안내 글도 흰색으로 그릴지.
 */
@Composable
internal fun MoaMapErrorNotice(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            style = MoaMapTheme.typography.body2,
            color = if (onDark) MoaMapTheme.colors.textWhite else MoaMapTheme.colors.textAssistive,
            textAlign = TextAlign.Center,
        )
        MoaMapRetryText(onClick = onRetryClick, onDark = onDark)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F9FA, widthDp = 393)
@Composable
private fun MoaMapErrorNoticePreview() {
    MoaMapTheme {
        MoaMapListPlaceholder {
            MoaMapErrorNotice(message = "지도를 불러오지 못했어요", onRetryClick = {})
        }
    }
}
