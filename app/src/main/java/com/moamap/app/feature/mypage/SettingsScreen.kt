package com.moamap.app.feature.mypage

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.ErrorSnackbar
import com.moamap.app.core.designsystem.component.MoaMapTitleTopBar
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.mypage.presentation.SettingsViewModel

private val ToggleShape = RoundedCornerShape(18.dp)
private val SettingsRowHeight = 47.dp

@Composable
internal fun SettingsScreen(
    onBackClick: () -> Unit,
    onLoggedOut: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onInquiryClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val loggedOut by viewModel.loggedOut.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(loggedOut) {
        if (loggedOut) onLoggedOut()
    }

    Box(modifier = modifier.fillMaxSize()) {
        SettingsContent(
            onBackClick = onBackClick,
            onTermsClick = onTermsClick,
            onPrivacyPolicyClick = onPrivacyPolicyClick,
            onInquiryClick = onInquiryClick,
            onLogoutClick = viewModel::logout,
        )
        ErrorSnackbar(
            message = errorMessage,
            onShown = viewModel::consumeError,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }
}

@Composable
private fun SettingsContent(
    onBackClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onInquiryClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary)
            .statusBarsPadding(),
    ) {
        MoaMapTitleTopBar(title = "설정", onBackClick = onBackClick)
        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingsSection(title = "알림") {
                SettingsCard(
                    shadowRadius = 5.dp,
                    content = {
                        SettingsRow(
                            label = "알림 온오프",
                            trailingContent = { SettingsToggle() },
                        )
                    },
                )
            }

            SettingsSection(title = "정보") {
                SettingsCard(shadowRadius = 2.5.dp) {
                    SettingsNavigationRow(label = "공지사항", showDivider = true)
                    SettingsNavigationRow(label = "이용약관", showDivider = true, onClick = onTermsClick)
                    SettingsNavigationRow(
                        label = "개인정보처리방침",
                        showDivider = true,
                        onClick = onPrivacyPolicyClick,
                    )
                    // 시안의 「신고하기」 자리를 문의하기로 쓴다(10-05 사용자 결정).
                    SettingsNavigationRow(label = "문의하기", onClick = onInquiryClick)
                }
            }

            SettingsSection(title = "앱 정보") {
                SettingsCard(
                    shadowRadius = 5.dp,
                    content = {
                        SettingsRow(
                            label = "버전 정보",
                            trailingContent = {
                                Text(
                                    text = "v1.0.0",
                                    style = MoaMapTheme.typography.caption2,
                                    color = MoaMapTheme.colors.textAssistive,
                                )
                            },
                        )
                    },
                )
            }

            SettingsSection(title = "계정") {
                SettingsCard(shadowRadius = 2.5.dp) {
                    SettingsNavigationRow(
                        label = "로그아웃",
                        showDivider = true,
                        onClick = onLogoutClick,
                    )
                    // 회원탈퇴는 서버 API 가 없어 아직 연결하지 않는다.
                    SettingsNavigationRow(
                        label = "회원탈퇴",
                        labelColor = MoaMapTheme.colors.statusAlert,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = title,
                style = MoaMapTheme.typography.subtitle1,
                color = MoaMapTheme.colors.textNormal,
            )
        }
        content()
    }
}

@Composable
private fun SettingsCard(
    shadowRadius: Dp,
    content: @Composable () -> Unit,
) {
    // 모서리 12·흰 바탕은 ShadowedSurface 기본값이다.
    ShadowedSurface(
        modifier = Modifier.fillMaxWidth(),
        shadowBlurRadius = shadowRadius,
        shadowColor = MoaMapPrimitiveColors.Black.copy(alpha = 0.08f),
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsNavigationRow(
    label: String,
    showDivider: Boolean = false,
    labelColor: Color = MoaMapTheme.colors.textNormal,
    onClick: () -> Unit = {},
) {
    SettingsRow(
        label = label,
        labelColor = labelColor,
        showDivider = showDivider,
        onClick = onClick,
        trailingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = MoaMapTheme.colors.textNormal,
                modifier = Modifier.size(16.dp),
            )
        },
    )
}

@Composable
private fun SettingsRow(
    label: String,
    labelColor: Color = MoaMapTheme.colors.textNormal,
    showDivider: Boolean = false,
    onClick: () -> Unit = {},
    trailingContent: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(SettingsRowHeight)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MoaMapTheme.typography.body1,
                color = labelColor,
                modifier = Modifier.weight(1f),
            )
            trailingContent()
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.align(Alignment.BottomCenter),
                thickness = 1.dp,
                color = MoaMapTheme.colors.lineAlternative,
            )
        }
    }
}

@Composable
private fun SettingsToggle() {
    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 24.dp)
            .clip(ToggleShape)
            .background(MoaMapPrimitiveColors.Blue100)
            .padding(3.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(MoaMapTheme.colors.primary, CircleShape),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun SettingsScreenPreview() {
    MoaMapTheme {
        SettingsContent(
            onBackClick = {},
            onTermsClick = {},
            onPrivacyPolicyClick = {},
            onInquiryClick = {},
            onLogoutClick = {},
        )
    }
}
