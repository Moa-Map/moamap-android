package com.moamap.app.feature.collection.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.MoaMapDialog
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme

private val DialogWidth = 353.dp
private val DialogContentPadding = PaddingValues(horizontal = 12.dp, vertical = 20.dp)

/** 밑줄만 있는 입력 필드. 피그마 수치. */
private val CodeFieldWidth = 181.dp
private val CodeFieldUnderlineHeight = 2.dp

private val SubmitButtonSize = 40.dp
private val SubmitIconSize = 32.dp

/**
 * 초대 코드로 프라이빗 지도에 합류하는 모달.
 *
 * 모음 화면 안에 두어 합류 후 탭 전환과 목록 갱신을 같은 ViewModel 로 처리한다.
 */
@Composable
internal fun JoinMapDialog(
    state: JoinState.Editing,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    // 코드를 받아 적으러 들어온 화면이라 키보드를 바로 올린다.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    MoaMapDialog(
        onDismissRequest = onDismiss,
        width = DialogWidth,
        contentPadding = DialogContentPadding,
        verticalSpacing = 12.dp,
        // 요청 중에 닫히면 결과를 알릴 곳이 사라진다.
        dismissible = !state.submitting,
    ) {
        Text(
            text = "지도 참여하기",
            style = MoaMapTheme.typography.title3,
            color = MoaMapTheme.colors.textNormal,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = buildAnnotatedString {
                append("친구에게 ")
                withStyle(SpanStyle(color = MoaMapPrimitiveColors.Blue500)) {
                    append("참여 코드")
                }
                append("를 받으세요")
            },
            style = MoaMapTheme.typography.subtitle2,
            color = MoaMapTheme.colors.textNormal,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InviteCodeField(
                code = state.code,
                enabled = !state.submitting,
                onCodeChange = onCodeChange,
                onSubmit = onSubmit,
                focusRequester = focusRequester,
            )
            SubmitButton(
                enabled = state.canSubmit,
                submitting = state.submitting,
                onClick = onSubmit,
            )
        }

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage,
                style = MoaMapTheme.typography.caption0,
                color = MoaMapTheme.colors.statusAlert,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun InviteCodeField(
    code: String,
    enabled: Boolean,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    focusRequester: FocusRequester,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        // 값에는 포함하지 않는 장식이다.
        Text(
            text = "#",
            style = MoaMapTheme.typography.display1.copy(fontSize = 30.sp),
            color = MoaMapTheme.colors.textNormal,
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicTextField(
                value = code,
                onValueChange = onCodeChange,
                enabled = enabled,
                singleLine = true,
                textStyle = MoaMapTheme.typography.display1.copy(
                    fontSize = 30.sp,
                    color = MoaMapTheme.colors.textNormal,
                    textAlign = TextAlign.Center,
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier
                    .width(CodeFieldWidth)
                    .focusRequester(focusRequester),
            )
            Box(
                modifier = Modifier
                    .width(CodeFieldWidth)
                    .height(CodeFieldUnderlineHeight)
                    .background(MoaMapPrimitiveColors.Black),
            )
        }
    }
}

@Composable
private fun SubmitButton(
    enabled: Boolean,
    submitting: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(SubmitButtonSize)
            .clip(CircleShape)
            .background(
                if (enabled) MoaMapPrimitiveColors.Blue500 else MoaMapPrimitiveColors.Gray200,
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (submitting) {
            CircularProgressIndicator(
                color = MoaMapTheme.colors.textWhite,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward),
                contentDescription = "지도 참여하기",
                tint = MoaMapTheme.colors.textWhite,
                modifier = Modifier.size(SubmitIconSize),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun JoinMapDialogPreview() {
    MoaMapTheme {
        JoinMapDialog(
            state = JoinState.Editing(code = "A1B2C3"),
            onCodeChange = {},
            onSubmit = {},
            onDismiss = {},
        )
    }
}
