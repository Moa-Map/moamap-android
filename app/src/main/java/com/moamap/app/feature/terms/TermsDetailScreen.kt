package com.moamap.app.feature.terms

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.moamap.app.core.designsystem.component.MoaMapErrorNotice
import com.moamap.app.core.designsystem.component.MoaMapLoadingIndicator
import com.moamap.app.core.designsystem.component.MoaMapTitleTopBar
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import com.moamap.app.feature.terms.domain.model.TermsDetail
import com.moamap.app.feature.terms.domain.model.TermsKind
import com.moamap.app.feature.terms.presentation.TermsDetailUiState
import com.moamap.app.feature.terms.presentation.TermsDetailViewModel

private const val TAG = "TermsDetailScreen"

/**
 * 약관 전문. 시안 「서비스 이용약관」(`3738:31380`): 상단 바 제목이 약관 이름, 아래에 본문이 이어진다. 동의 화면의
 * 항목과 설정 화면의 이용약관·개인정보처리방침이 같은 화면을 쓴다.
 *
 * 본문은 서버가 주는 마크다운이라 마크다운 렌더러로 그린다. HTML 은 실행하지 않는다.
 */
@Composable
fun TermsDetailScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TermsDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TermsDetailContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onRetryClick = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
private fun TermsDetailContent(
    uiState: TermsDetailUiState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary)
            .statusBarsPadding(),
    ) {
        MoaMapTitleTopBar(title = uiState.terms?.title.orEmpty(), onBackClick = onBackClick)

        val terms = uiState.terms
        if (terms == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center,
            ) {
                val errorMessage = uiState.errorMessage
                if (errorMessage != null) {
                    MoaMapErrorNotice(message = errorMessage, onRetryClick = onRetryClick)
                } else if (uiState.loading) {
                    MoaMapLoadingIndicator()
                }
            }
            return@Column
        }

        // 시안: 상단 바 아래 20, 좌우 20, 위아래 16.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 20.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .navigationBarsPadding(),
        ) {
            TermsMarkdown(content = terms.content)
        }
    }
}

/**
 * 약관 본문. 제목·굵게·링크·번호 목록·중첩 불릿을 그린다.
 *
 * 글자는 앱 글꼴로 맞춘다 - 시안의 소제목(`subtitle2`)·본문(`body2`). 링크는 외부 브라우저나 메일 앱으로 연다.
 */
@Composable
private fun TermsMarkdown(content: String) {
    val heading = MoaMapTheme.typography.subtitle2.withDesignLineHeight().copy(color = MoaMapTheme.colors.textNormal)
    val body = MoaMapTheme.typography.body2.withDesignLineHeight().copy(color = MoaMapTheme.colors.textNormal)
    val link = TextLinkStyles(
        style = SpanStyle(color = MoaMapTheme.colors.textNormal, textDecoration = TextDecoration.Underline),
    )

    CompositionLocalProvider(LocalUriHandler provides rememberSafeUriHandler()) {
        Markdown(
            content = content,
            colors = markdownColor(text = MoaMapTheme.colors.textNormal),
            typography = markdownTypography(
                // 약관 맨 위 `#` 는 상단 바와 같은 약관 이름이라 본문 소제목보다 한 단계만 키운다.
                h1 = MoaMapTheme.typography.title3.withDesignLineHeight().copy(color = MoaMapTheme.colors.textNormal),
                h2 = heading,
                h3 = heading,
                h4 = heading,
                h5 = heading,
                h6 = heading,
                text = body,
                paragraph = body,
                ordered = body,
                bullet = body,
                list = body,
                textLink = link,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 링크를 열 앱이 없으면(메일 앱이 없는 기기의 `mailto:` 등) 기본 처리기가 예외를 던져 앱이 죽는다. 열지 못하면
 * 아무 일도 하지 않는다.
 */
@Composable
private fun rememberSafeUriHandler(): UriHandler {
    val platform = LocalUriHandler.current
    return remember(platform) {
        object : UriHandler {
            override fun openUri(uri: String) {
                try {
                    platform.openUri(uri)
                } catch (e: Exception) {
                    Log.w(TAG, "약관 링크를 열지 못함: $uri", e)
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun TermsDetailScreenPreview() {
    MoaMapTheme {
        TermsDetailContent(
            uiState = TermsDetailUiState(
                loading = false,
                terms = TermsDetail(
                    termId = 1,
                    code = "SERVICE",
                    title = "서비스 이용약관",
                    version = "1",
                    kind = TermsKind.REQUIRED,
                    active = true,
                    content = """
                        ## 제1조 (목적)
                        이 약관은 **모아맵**이 제공하는 서비스의 이용 조건을 정합니다.

                        ## 제2조 (용어의 정의)
                        1. "회원": 이 약관에 동의하고 소셜 계정으로 로그인해 서비스를 이용하는 사람
                           - 만 14세 이상이어야 합니다.
                        2. 문의: [moamap@example.com](mailto:moamap@example.com)
                    """.trimIndent(),
                ),
            ),
            onBackClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun TermsDetailScreenErrorPreview() {
    MoaMapTheme {
        TermsDetailContent(
            uiState = TermsDetailUiState(loading = false, errorMessage = "약관을 불러오지 못했어요"),
            onBackClick = {},
            onRetryClick = {},
        )
    }
}
