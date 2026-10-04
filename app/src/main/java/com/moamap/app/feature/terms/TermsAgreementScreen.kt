package com.moamap.app.feature.terms

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.ButtonShadowBlurRadius
import com.moamap.app.core.designsystem.component.ButtonShadowColor
import com.moamap.app.core.designsystem.component.MoaMapTitleTopBar
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import com.moamap.app.feature.terms.domain.model.Terms
import com.moamap.app.feature.terms.domain.model.TermsConsentType
import com.moamap.app.feature.terms.presentation.TermsAgreementResult
import com.moamap.app.feature.terms.presentation.TermsAgreementUiState
import com.moamap.app.feature.terms.presentation.TermsAgreementViewModel

/** 아래 버튼과 그 위아래 여백(12)만큼 스크롤 끝을 비워 마지막 항목이 가리지 않게 한다. */
private val BottomButtonReservedHeight = 78.dp

/**
 * 소셜 로그인 다음의 이용약관 동의. 시안 「이용약관 동의 기본」(`3738:24422`)·「이용약관 동의」(`3738:31152`).
 *
 * - 왼쪽 체크 표시를 누르면 체크/해제, 약관 이름이나 「>」 를 누르면 [onTermsClick] 로 전문을 연다.
 * - 뒤로 가면(← 또는 기기 뒤로가기) 로그인을 취소하고 [onCancelled] 로 로그인 화면에 돌아간다.
 * - 필수에 모두 동의하고 「다음으로」 를 누르면 [onAgreed] 로 로그인을 마친다.
 */
@Composable
fun TermsAgreementScreen(
    onAgreed: () -> Unit,
    onCancelled: () -> Unit,
    onTermsClick: (code: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TermsAgreementViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.result) {
        when (uiState.result) {
            TermsAgreementResult.Agreed -> {
                viewModel.consumeResult()
                onAgreed()
            }

            TermsAgreementResult.Cancelled -> {
                viewModel.consumeResult()
                onCancelled()
            }

            null -> Unit
        }
    }

    BackHandler(onBack = viewModel::cancel)

    TermsAgreementContent(
        uiState = uiState,
        onBackClick = viewModel::cancel,
        onToggleAll = viewModel::toggleAll,
        onToggle = viewModel::toggle,
        onTermsClick = onTermsClick,
        onBottomButtonClick = viewModel::onBottomButtonClick,
        modifier = modifier,
    )
}

@Composable
private fun TermsAgreementContent(
    uiState: TermsAgreementUiState,
    onBackClick: () -> Unit,
    onToggleAll: () -> Unit,
    onToggle: (String) -> Unit,
    onTermsClick: (String) -> Unit,
    onBottomButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            MoaMapTitleTopBar(title = "이용약관 동의", onBackClick = onBackClick)

            // 시안: 상단 바 아래 20, 좌우 20, 제목(왼쪽 안쪽 4) ↔ 「모두 동의하기」 20, 그 아래 32 간격.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 20.dp, start = 20.dp, end = 20.dp)
                    .padding(bottom = BottomButtonReservedHeight)
                    .navigationBarsPadding(),
            ) {
                Text(
                    text = "서비스 이용약관에\n동의해주세요",
                    style = MoaMapTheme.typography.title2.withDesignLineHeight(),
                    color = MoaMapTheme.colors.textNormal,
                    modifier = Modifier.padding(start = 4.dp),
                )
                Spacer(Modifier.height(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
                    AgreeAllRow(checked = uiState.allChecked, onClick = onToggleAll)
                    uiState.terms.forEach { terms ->
                        TermsRow(
                            terms = terms,
                            checked = terms.code in uiState.checked,
                            onToggle = { onToggle(terms.code) },
                            onOpen = { onTermsClick(terms.code) },
                        )
                    }
                }
            }
        }

        AgreementButton(
            // 필수를 다 체크하면 「다음으로」, 아니면 「모두 동의하기」(누르면 전부 체크만 한다).
            label = if (uiState.requiredChecked) "다음으로" else "모두 동의하기",
            onClick = onBottomButtonClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

/** 시안 「모두 동의하기」 줄: 높이 60, 회색 바탕, 모서리 12, 안쪽 12/16. 줄 전체가 눌린다. */
@Composable
private fun AgreeAllRow(
    checked: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MoaMapPrimitiveColors.Gray50)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AgreementCheck(checked = checked)
        Spacer(Modifier.width(8.dp))
        Text(
            text = "모두 동의하기",
            style = MoaMapTheme.typography.subtitle3,
            color = MoaMapTheme.colors.textNormal,
        )
    }
}

/** 약관 한 줄: 체크 표시 · 이름 (필수/선택) · 「>」. 체크 표시와 나머지가 따로 눌린다. */
@Composable
private fun TermsRow(
    terms: Terms,
    checked: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            ),
        ) {
            AgreementCheck(checked = checked)
        }
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(role = Role.Button, onClickLabel = "전문 보기", onClick = onOpen),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = terms.title,
                style = MoaMapTheme.typography.subtitle4,
                color = MoaMapTheme.colors.textAlternative,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (terms.consentType == TermsConsentType.REQUIRED) "(필수)" else "(선택)",
                style = MoaMapTheme.typography.subtitle4,
                color = MoaMapTheme.colors.textAlternative,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = MoaMapTheme.colors.textAlternative,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** 체크 표시. 시안: 안 고르면 회색 `#C5C7C7`, 고르면 파랑 `#09A8FA`. */
@Composable
private fun AgreementCheck(checked: Boolean) {
    Icon(
        painter = painterResource(R.drawable.ic_check_thin),
        contentDescription = null,
        tint = if (checked) MoaMapPrimitiveColors.Blue500 else MoaMapPrimitiveColors.Gray100,
        modifier = Modifier.size(24.dp),
    )
}

/** 시안 「Button」 Large: 높이 54, 모서리 8, 그림자 0 0 10 10%, 글자 button0. */
@Composable
private fun AgreementButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShadowedSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(8.dp),
        color = MoaMapTheme.colors.primary,
        shadowBlurRadius = ButtonShadowBlurRadius,
        shadowColor = ButtonShadowColor,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MoaMapTheme.typography.button0,
                color = MoaMapTheme.colors.textWhite,
            )
        }
    }
}

private fun previewTerms() = listOf(
    Terms("SERVICE", "서비스 이용약관", TermsConsentType.REQUIRED, "1", ""),
    Terms("PRIVACY_COLLECTION", "개인정보수집 및 이용동의", TermsConsentType.REQUIRED, "1", ""),
    Terms("MARKETING", "마케팅활용 동의", TermsConsentType.OPTIONAL, "1", ""),
)

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun TermsAgreementScreenPreview() {
    MoaMapTheme {
        TermsAgreementContent(
            uiState = TermsAgreementUiState(terms = previewTerms(), checked = setOf("SERVICE")),
            onBackClick = {},
            onToggleAll = {},
            onToggle = {},
            onTermsClick = {},
            onBottomButtonClick = {},
        )
    }
}
