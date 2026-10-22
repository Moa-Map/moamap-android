package com.moamap.app.feature.onboarding.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.core.designsystem.component.MoaMapErrorNotice
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.terms.domain.TERMS_CHECK_FAILED_MESSAGE

/** 「로그아웃」을 눌렀는데 세션을 지우지 못했을 때의 안내. 화면에 머문다. */
internal const val TERMS_CHECK_LOGOUT_FAILED_MESSAGE = "로그아웃하지 못했어요. 잠시 후 다시 시도해 주세요."

/** 「로그아웃」 글자가 작아도 손가락으로 누를 수 있게 잡는 최소 영역. */
private val LogoutTouchSize = 44.dp

/**
 * 스플래시·로그인 직후 약관 동의 여부를 확인하지 못했을 때 화면 아래에 띄우는 안내. iOS(moamap-iOS#124)와 같은 모양이다.
 *
 * 「다시 시도」만 두면 서버가 계속 실패할 때 앱 안에서 빠져나갈 길이 없어, 그 아래 12 에 「로그아웃」을 둔다.
 *
 * @param loggingOut 로그아웃 중. 「로그아웃」을 다시 누를 수 없다(「다시 시도」는 화면 상태가 막는다).
 */
@Composable
internal fun TermsCheckFailedNotice(
    loggingOut: Boolean,
    onRetryClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MoaMapErrorNotice(message = TERMS_CHECK_FAILED_MESSAGE, onRetryClick = onRetryClick)
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .sizeIn(minWidth = LogoutTouchSize, minHeight = LogoutTouchSize)
                .clickable(enabled = !loggingOut, role = Role.Button, onClick = onLogoutClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "로그아웃",
                style = MoaMapTheme.typography.body2,
                color = MoaMapTheme.colors.textAssistive,
                textDecoration = TextDecoration.Underline,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393)
@Composable
private fun TermsCheckFailedNoticePreview() {
    MoaMapTheme {
        TermsCheckFailedNotice(loggingOut = false, onRetryClick = {}, onLogoutClick = {})
    }
}
