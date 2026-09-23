package com.moamap.app.feature.mapdetail

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.moamap.app.core.designsystem.component.MoaMapConfirmDialog
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.mapdetail.domain.model.LeaveOutcome

/**
 * 지도에서 나가기 전에 한 번 더 묻는 팝업.
 *
 * 나가기는 지도에 따라 결과가 다르다. 특히 혼자 남은 프라이빗 지도의 방장은 나가면 지도가
 * 삭제되므로, 무엇이 일어나는지 [outcome] 에 맞춰 알린다.
 */
@Composable
internal fun MapLeaveDialog(
    mapName: String,
    outcome: LeaveOutcome,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    MoaMapConfirmDialog(
        // 제목은 한 줄로 보인다. 이름이 너무 길면 이름 끝만 줄이고 질문은 다 보여 준다.
        title = mapName,
        titleSuffix = "에서 나가시겠습니까?",
        message = leaveConfirmMessage(outcome),
        confirmText = "나가기",
        onConfirm = onConfirm,
        onDismissRequest = onDismiss,
    )
}

internal fun leaveConfirmMessage(outcome: LeaveOutcome): String = when (outcome) {
    LeaveOutcome.Leave -> "나가시면 모음 탭에서 지도가 사라집니다"
    LeaveOutcome.LeaveNeedsInviteCode ->
        "나가시면 모음 탭에서 지도가 사라집니다\n다시 들어오려면 초대코드가 필요합니다"
    LeaveOutcome.DeleteMap -> "혼자 남은 지도라 나가면 지도가 삭제됩니다\n되돌릴 수 없습니다"
}

private class LeaveOutcomeProvider : PreviewParameterProvider<LeaveOutcome> {
    override val values = LeaveOutcome.entries.asSequence()
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MapLeaveDialogPreview(
    @PreviewParameter(LeaveOutcomeProvider::class) outcome: LeaveOutcome,
) {
    MoaMapTheme {
        MapLeaveDialog(
            mapName = "우리끼리 맛집",
            outcome = outcome,
            onConfirm = {},
            onDismiss = {},
        )
    }
}
