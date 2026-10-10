package com.moamap.app.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme

/** 시안 「Button」 Large 높이. 버튼에 가리지 않게 스크롤 아래를 비우는 화면도 이 값을 쓴다. */
internal val MoaMapLargeButtonHeight = 54.dp

private val LargeButtonShape = RoundedCornerShape(8.dp)

/** 진행 표시 크기. 버튼 높이 안에 들어가면서 글자와 비슷한 무게로 보이는 값. */
private val LargeButtonProgressSize = 20.dp

/**
 * 화면 아래 고정 버튼. 시안 「Button」 Large: 높이 54, 모서리 8, 그림자 0 0 10 10%, 글자 button0.
 *
 * 누를 수 없으면 회색이고, 보내는 중에는 글자 대신 진행 표시를 띄운다.
 *
 * @param disabledColor 누를 수 없을 때 색. 프로필 편집만 시안이 Gray100 이다.
 */
@Composable
internal fun MoaMapLargeButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    submitting: Boolean = false,
    disabledColor: Color = MoaMapPrimitiveColors.Gray200,
) {
    ShadowedSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(MoaMapLargeButtonHeight),
        shape = LargeButtonShape,
        color = if (enabled) MoaMapTheme.colors.primary else disabledColor,
        shadowBlurRadius = ButtonShadowBlurRadius,
        shadowColor = ButtonShadowColor,
        onClick = if (enabled) onClick else null,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (submitting) {
                CircularProgressIndicator(
                    color = MoaMapTheme.colors.textWhite,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(LargeButtonProgressSize),
                )
            } else {
                Text(
                    text = label,
                    style = MoaMapTheme.typography.button0,
                    color = MoaMapTheme.colors.textWhite,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393)
@Composable
private fun MoaMapLargeButtonPreview() {
    MoaMapTheme {
        MoaMapLargeButton(label = "저장하기", onClick = {})
    }
}
