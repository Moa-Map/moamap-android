package com.moamap.app.feature.terms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.core.designsystem.component.MoaMapTitleTopBar
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.terms.presentation.TermsDetailUiState
import com.moamap.app.feature.terms.presentation.TermsDetailViewModel
import com.moamap.app.feature.terms.presentation.TermsLine
import com.moamap.app.feature.terms.presentation.TermsSection

/**
 * 약관 전문. 시안 「서비스 이용약관」(`3738:31380`): 상단 바 제목이 약관 이름, 아래에 소제목 + 본문이
 * 반복된다. 동의 화면의 항목과 설정 화면의 이용약관·개인정보처리방침이 같은 화면을 쓴다.
 */
@Composable
fun TermsDetailScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TermsDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TermsDetailContent(uiState = uiState, onBackClick = onBackClick, modifier = modifier)
}

@Composable
private fun TermsDetailContent(
    uiState: TermsDetailUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary)
            .statusBarsPadding(),
    ) {
        MoaMapTitleTopBar(title = uiState.title, onBackClick = onBackClick)

        // 시안: 상단 바 아래 20, 좌우 20, 위아래 16, 덩어리 사이 32, 소제목 ↔ 본문 12.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 20.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            uiState.sections.forEach { section -> TermsSectionBlock(section) }
        }
    }
}

@Composable
private fun TermsSectionBlock(section: TermsSection) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        section.heading?.let { heading ->
            Text(
                text = heading,
                style = MoaMapTheme.typography.subtitle2,
                color = MoaMapTheme.colors.textNormal,
            )
        }
        if (section.lines.isNotEmpty()) {
            Column {
                section.lines.forEach { line -> TermsLineText(line) }
            }
        }
    }
}

/** 기호가 있는 줄은 기호와 글을 나눠, 글이 다음 줄로 넘어가도 글 시작 위치에 맞춰 잇는다. */
@Composable
private fun TermsLineText(line: TermsLine) {
    val style = MoaMapTheme.typography.body2
    val color = MoaMapTheme.colors.textNormal
    if (line.marker == null) {
        Text(text = line.text, style = style, color = color)
        return
    }
    Row {
        Text(text = line.marker, style = style, color = color)
        Spacer(Modifier.width(4.dp))
        Text(text = line.text, style = style, color = color, modifier = Modifier.weight(1f))
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun TermsDetailScreenPreview() {
    MoaMapTheme {
        TermsDetailContent(
            uiState = TermsDetailUiState(
                title = "서비스 이용약관",
                sections = listOf(
                    TermsSection(
                        heading = "제1조 (목적)",
                        lines = listOf(TermsLine(null, "이 약관은 모아맵이 제공하는 서비스의 이용 조건을 정합니다.")),
                    ),
                    TermsSection(
                        heading = "제2조 (용어의 정의)",
                        lines = listOf(
                            TermsLine("·", "\"회원\": 이 약관에 동의하고 소셜 계정으로 로그인해 서비스를 이용하는 사람"),
                            TermsLine("1.", "운영자는 관련 법령을 어기지 않는 범위에서 약관을 바꿀 수 있습니다."),
                        ),
                    ),
                ),
            ),
            onBackClick = {},
        )
    }
}
