package com.moamap.app.feature.mapdetail

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.core.designsystem.component.ConfirmDialogButton
import com.moamap.app.core.designsystem.component.MoaMapInputSurface
import com.moamap.app.core.designsystem.component.PopupCard
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import com.moamap.app.feature.mapdetail.domain.model.ReportReason

/** 「기타」 내용 글자 수. iOS 와 같다. */
private const val REPORT_DETAIL_MAX_LENGTH = 200

/**
 * 신고 사유 고르기. 시안 「팝업창」(`5133:73043`): 제목 → 동그라미 목록(사이 8, 동그라미 20 ↔ 글자 16) →
 * 「기타」면 입력칸(높이 90) → [취소][신고하기]. 사유는 시안의 네 가지 대신 [ReportReason] 여섯 가지다.
 *
 * [신고하기]는 사유를 고르고, 「기타」면 내용까지 적어야 눌린다 - 서버가 「기타」 내용을 꼭 받는다.
 */
@Composable
internal fun ReportDialog(
    title: String,
    onSubmit: (ReportReason, String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var reason by rememberSaveable { mutableStateOf<ReportReason?>(null) }
    var detail by rememberSaveable { mutableStateOf("") }
    val selected = reason
    val canSubmit = selected != null && (selected != ReportReason.Other || detail.isNotBlank())

    PopupCard(onDismissRequest = onDismissRequest) {
        Text(
            text = title,
            style = MoaMapTheme.typography.title3.withDesignLineHeight(),
            color = MoaMapTheme.colors.textNormal,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Column(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReportReason.entries.forEach { option ->
                ReasonRow(label = option.label, selected = option == reason, onClick = { reason = option })
            }
            if (reason == ReportReason.Other) {
                MoaMapInputSurface(modifier = Modifier.fillMaxWidth().height(90.dp)) {
                    BasicTextField(
                        value = detail,
                        onValueChange = { value -> detail = value.take(REPORT_DETAIL_MAX_LENGTH) },
                        textStyle = MoaMapTheme.typography.body2.withDesignLineHeight().copy(
                            color = MoaMapTheme.colors.textNormal,
                        ),
                        cursorBrush = SolidColor(MoaMapTheme.colors.primary),
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                        decorationBox = { innerTextField ->
                            Box {
                                if (detail.isEmpty()) {
                                    Text(
                                        text = "신고 사유를 입력해주세요",
                                        style = MoaMapTheme.typography.body2.withDesignLineHeight(),
                                        color = MoaMapTheme.colors.textAssistive,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ConfirmDialogButton(text = "취소", color = MoaMapPrimitiveColors.Gray200, onClick = onDismissRequest)
            ConfirmDialogButton(
                text = "신고하기",
                color = if (canSubmit) MoaMapPrimitiveColors.Blue500 else MoaMapPrimitiveColors.Gray200,
                onClick = { if (selected != null && canSubmit) onSubmit(selected, detail) },
            )
        }
    }
}

/** 동그라미 20(테두리 Gray100, 고르면 파란 테두리 + 안쪽 14 파랑) ↔ 16 ↔ 글자 body1. 줄 전체가 눌린다. */
@Composable
private fun ReasonRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(
                    width = 1.dp,
                    color = if (selected) MoaMapPrimitiveColors.Blue500 else MoaMapPrimitiveColors.Gray100,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(14.dp).background(MoaMapPrimitiveColors.Blue500, CircleShape))
        }
        Text(
            text = label,
            style = MoaMapTheme.typography.body1.withDesignLineHeight(),
            color = MoaMapTheme.colors.textNormal,
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun ReportDialogPreview() {
    MoaMapTheme {
        ReportDialog(title = "댓글 신고하기", onSubmit = { _, _ -> }, onDismissRequest = {})
    }
}
