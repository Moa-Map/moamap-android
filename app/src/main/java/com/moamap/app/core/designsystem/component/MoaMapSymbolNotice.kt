package com.moamap.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moamap.app.core.designsystem.theme.MoaMapTheme

/** 안내 묶음 위의 흐린 로고 본체 너비. */
private val SymbolNoticeLogoWidth = 75.dp
private const val SYMBOL_NOTICE_LOGO_ALPHA = 0.6f

/**
 * 흐린 로고(75, 60%) 아래 제목(subtitle4)과 설명을 가운데로 쌓은 안내. 피그마 릴리즈 v1 의 장소 검색·장소
 * 가져오기 링크 입력 화면이 입력칸 아래 같은 모양으로 쓴다. 설명은 줄 맞춤이 화면마다 달라 [detail] 로 받는다.
 */
@Composable
internal fun MoaMapSymbolNotice(
    title: String,
    modifier: Modifier = Modifier,
    /** 모음 탭 지도 없음 안내는 진회색(#4A4F52)이다. */
    titleColor: Color = MoaMapTheme.colors.textNormal,
    detail: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MoaSymbolLogo(
            bodyWidth = SymbolNoticeLogoWidth,
            modifier = Modifier.alpha(SYMBOL_NOTICE_LOGO_ALPHA),
            contentDescription = null,
        )
        Column(
            modifier = Modifier.semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MoaMapTheme.typography.subtitle4,
                color = titleColor,
                textAlign = TextAlign.Center,
            )
            detail()
        }
    }
}
