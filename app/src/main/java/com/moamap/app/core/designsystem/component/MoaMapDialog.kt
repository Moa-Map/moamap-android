package com.moamap.app.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors

private val DialogShape = RoundedCornerShape(16.dp)
private val DialogBorderWidth = 2.dp

/**
 * 파란 테두리를 두른 중앙 카드 모달.
 *
 * 스크림과 뒤로가기·바깥 탭 닫기는 [Dialog] 가 처리한다. 여기서는 카드만 그린다.
 * 키보드가 올라오는 모달도 있어 [imePadding] 으로 카드가 가리지 않게 한다.
 *
 * @param dismissible 바깥을 누르거나 뒤로가기로 닫을 수 있는지. 요청이 진행 중일 때 잠근다.
 */
@Composable
fun MoaMapDialog(
    onDismissRequest: () -> Unit,
    width: Dp,
    contentPadding: PaddingValues,
    verticalSpacing: Dp,
    modifier: Modifier = Modifier,
    dismissible: Boolean = true,
    backgroundColor: Color = MoaMapPrimitiveColors.White,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
            // 카드 폭을 디자인 수치로 잡으려면 기본 플랫폼 폭 제한을 풀어야 한다.
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = modifier
                .width(width)
                .imePadding(),
            shape = DialogShape,
            color = backgroundColor,
            border = BorderStroke(DialogBorderWidth, MoaMapPrimitiveColors.Blue500),
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
        }
    }
}
