package com.moamap.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme

private val ConfirmDialogWidth = 300.dp
private val ConfirmDialogShape = RoundedCornerShape(12.dp)
private val ConfirmDialogContentPadding = PaddingValues(
    start = 24.dp,
    top = 32.dp,
    end = 24.dp,
    bottom = 20.dp,
)
private val ConfirmButtonHeight = 44.dp
private val ConfirmButtonShape = RoundedCornerShape(8.dp)

/** 제목을 한 줄에 맞추려고 줄일 수 있는 가장 작은 글자. 본문(body2)보다 작아지지 않게 한다. */
private val TitleMinFontSize = 14.sp
private const val TITLE_ELLIPSIS = "…"

/**
 * 한 번 더 묻는 확인 팝업. 피그마 「팝업창」 컴포넌트(`2039:13583`)다.
 *
 * 제목·부제 아래에 취소·확인 두 버튼을 둔다. 확인을 눌러도 스스로 닫지 않는다 - 닫을지,
 * 닫은 뒤 무엇을 할지는 호출부가 [onConfirm] 에서 정한다.
 *
 * 제목은 [title] + [titleSuffix] 를 늘 한 줄로 보여 준다. 한국어는 글자 단위로 줄이 바뀌어
 * 두 줄이 되면 "나가시겠습니 / 까?" 처럼 끊긴다. 넘치면 글자를 줄이고, 그래도 넘치면
 * [title] 끝을 말줄임한다 - [fitTitleToOneLine] 참고.
 *
 * @param title 제목 앞부분. 지도 이름처럼 길이를 알 수 없는 값을 둔다. 너무 길면 끝이 줄어든다.
 * @param titleSuffix 제목 뒷부분. 줄이지 않고 늘 다 보여 준다. 질문을 둔다.
 */
@Composable
fun MoaMapConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    titleSuffix: String = "",
    dismissText: String = "취소하기",
) {
    MoaMapDialog(
        onDismissRequest = onDismissRequest,
        width = ConfirmDialogWidth,
        contentPadding = ConfirmDialogContentPadding,
        verticalSpacing = 20.dp,
        backgroundColor = MoaMapTheme.colors.backgroundSecondary,
        shape = ConfirmDialogShape,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SingleLineTitle(title = title, suffix = titleSuffix)
            Text(
                text = message,
                style = MoaMapTheme.typography.caption2,
                color = MoaMapTheme.colors.textAlternative,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ConfirmDialogButton(
                text = dismissText,
                color = MoaMapPrimitiveColors.Gray100,
                onClick = onDismissRequest,
            )
            ConfirmDialogButton(
                text = confirmText,
                color = MoaMapPrimitiveColors.Blue500,
                onClick = onConfirm,
            )
        }
    }
}

@Composable
private fun SingleLineTitle(title: String, suffix: String) {
    val textMeasurer = rememberTextMeasurer()
    val style = MoaMapTheme.typography.title3
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // 잰 폭과 그린 폭이 반올림으로 1px 어긋나도 넘치지 않게 여유를 둔다.
        val availableWidth = with(density) { maxWidth.toPx() } - 1f
        val fitted = remember(title, suffix, availableWidth, style, density) {
            fitTitleToOneLine(
                title = title,
                suffix = suffix,
                availableWidth = availableWidth,
                minScale = (TitleMinFontSize.value / style.fontSize.value).coerceAtMost(1f),
            ) { text ->
                textMeasurer.measure(text, style, softWrap = false, maxLines = 1)
                    .size.width.toFloat()
            }
        }

        Text(
            text = fitted.text,
            // 폭이 글자 크기에 비례하도록 자간·줄 높이도 같은 비율로 줄인다.
            style = style.copy(
                fontSize = style.fontSize * fitted.scale,
                lineHeight = style.lineHeight * fitted.scale,
                letterSpacing = style.letterSpacing * fitted.scale,
            ),
            color = MoaMapTheme.colors.textNormal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 한 줄에 맞춘 제목과 원래 글자 크기에 곱할 배율. */
internal data class FittedTitle(val text: String, val scale: Float)

/**
 * 제목을 한 줄에 맞춘다.
 *
 * 먼저 글자를 [minScale] 까지 줄여 본다. 그래도 넘치면 [minScale] 크기에서 들어가는 만큼만
 * [title] 을 남기고 끝을 말줄임한다. [suffix] 는 줄이지 않는다.
 *
 * @param widthOf 원래 글자 크기로 잰 폭. 폭은 글자 크기에 비례한다고 보고 배율로 나눠 비교한다.
 */
internal fun fitTitleToOneLine(
    title: String,
    suffix: String,
    availableWidth: Float,
    minScale: Float,
    widthOf: (String) -> Float,
): FittedTitle {
    val full = title + suffix
    val fullWidth = widthOf(full)
    if (fullWidth <= availableWidth) return FittedTitle(full, 1f)

    val scale = availableWidth / fullWidth
    if (scale >= minScale) return FittedTitle(full, scale)

    // 가장 작은 글자에서 들어가는 가장 긴 앞부분을 찾는다. 앞부분이 길수록 폭도 넓다.
    val limit = availableWidth / minScale
    var fits = 0
    var low = 1
    var high = title.length - 1
    while (low <= high) {
        val mid = (low + high) / 2
        if (widthOf(shortenedTitle(title, mid, suffix)) <= limit) {
            fits = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    return FittedTitle(shortenedTitle(title, fits, suffix), minScale)
}

private fun shortenedTitle(title: String, keep: Int, suffix: String): String {
    var head = title.take(keep)
    // 이모지처럼 두 칸짜리 글자의 반쪽만 남기면 깨진 글자가 된다.
    if (head.isNotEmpty() && head.last().isHighSurrogate()) head = head.dropLast(1)
    return head.trimEnd() + TITLE_ELLIPSIS + suffix
}

@Composable
private fun RowScope.ConfirmDialogButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    ShadowedSurface(
        modifier = Modifier
            .weight(1f)
            .height(ConfirmButtonHeight),
        shape = ConfirmButtonShape,
        color = color,
        shadowBlurRadius = ButtonShadowBlurRadius,
        shadowColor = ButtonShadowColor,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MoaMapTheme.typography.button2,
                color = MoaMapTheme.colors.textWhite,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Preview(name = "Large font", showBackground = true, fontScale = 2f, widthDp = 393, heightDp = 852)
@Composable
private fun MoaMapConfirmDialogPreview() {
    MoaMapTheme {
        MoaMapConfirmDialog(
            title = "카공족 모여라",
            titleSuffix = "에서 나가시겠습니까?",
            message = "나가시면 모음 탭에서 지도가 사라집니다",
            confirmText = "나가기",
            onConfirm = {},
            onDismissRequest = {},
        )
    }
}

/** 가장 작은 글자로도 넘치면 이름 끝이 줄어든다. */
@Preview(name = "Long title", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MoaMapConfirmDialogLongTitlePreview() {
    MoaMapTheme {
        MoaMapConfirmDialog(
            title = "서울 성수동 주말에 가기 좋은 카페 모음 지도",
            titleSuffix = "에서 나가시겠습니까?",
            message = "나가시면 모음 탭에서 지도가 사라집니다",
            confirmText = "나가기",
            onConfirm = {},
            onDismissRequest = {},
        )
    }
}
