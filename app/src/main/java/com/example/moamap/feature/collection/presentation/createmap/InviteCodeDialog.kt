package com.example.moamap.feature.collection.presentation.createmap

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.moamap.core.designsystem.component.MoaMapDialog
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import kotlinx.coroutines.launch

private val DialogWidth = 300.dp
private val DialogContentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp)

/** 코드 박스 아래 경계에 걸치도록 내린다. 말풍선 높이의 절반. */
private val TooltipOverlap = 16.dp

/**
 * 말풍선 전체 높이와 꼬리 높이. 피그마 32dp 안에서 꼬리가 위쪽을 차지하고 몸통이 24dp 다.
 *
 * 글자는 전체가 아니라 **몸통 안에서** 가운데 정렬해야 한다. 전체 기준으로 잡으면 꼬리만큼
 * 위로 밀려 아래 여백이 더 벌어진다.
 */
private val TooltipHeight = 32.dp
private val TooltipTailHeight = 8.dp
private val TooltipTailWidth = 12.dp

private val ButtonHeight = 44.dp
private val ButtonShape = RoundedCornerShape(8.dp)
private val CodeBoxShape = RoundedCornerShape(12.dp)

/**
 * 프라이빗 지도를 만든 직후 초대 코드를 보여준다.
 *
 * 코드는 생성 응답에 함께 실려 오므로 여기서 따로 조회하지 않는다.
 */
@Composable
internal fun InviteCodeDialog(
    mapName: String,
    inviteCode: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    MoaMapDialog(
        onDismissRequest = onDismiss,
        width = DialogWidth,
        contentPadding = DialogContentPadding,
        verticalSpacing = 32.dp,
        backgroundColor = MoaMapTheme.colors.backgroundSecondary,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "프라이빗 지도가 만들어졌어요",
                    style = MoaMapTheme.typography.title3,
                    color = MoaMapTheme.colors.textNormal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "초대코드를 공유하고 친구와 함께 해보세요",
                    style = MoaMapTheme.typography.caption2,
                    color = MoaMapPrimitiveColors.Gray500,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            InviteCodeBox(
                inviteCode = inviteCode,
                onLongClick = {
                    scope.launch {
                        clipboard.setClipEntry(
                            ClipEntry(ClipData.newPlainText("초대코드", inviteCode)),
                        )
                    }
                    // 안드로이드 13 부터는 복사할 때 시스템이 자체 알림을 띄운다.
                    // 우리가 또 띄우면 같은 안내가 두 번 뜬다.
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                        Toast.makeText(context, "초대코드를 복사했어요", Toast.LENGTH_SHORT).show()
                    }
                },
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DialogButton(
                text = "공유하기",
                color = MoaMapPrimitiveColors.Blue500,
                onClick = { context.shareInviteCode(mapName = mapName, inviteCode = inviteCode) },
            )
            DialogButton(
                text = "닫기",
                color = MoaMapPrimitiveColors.Gray200,
                onClick = onDismiss,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InviteCodeBox(
    inviteCode: String,
    onLongClick: () -> Unit,
) {
    // 말풍선이 박스 아래 경계를 넘어가므로 겹쳐 그린다.
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CodeBoxShape)
                .background(MoaMapTheme.colors.backgroundSecondary)
                .border(1.dp, MoaMapTheme.colors.lineNormal, CodeBoxShape)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick,
                )
                .padding(vertical = 20.dp, horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "초대코드",
                style = MoaMapTheme.typography.subtitle4,
                color = MoaMapPrimitiveColors.Gray500,
            )
            Text(
                text = inviteCode,
                style = MoaMapTheme.typography.display1,
                color = MoaMapTheme.colors.textNormal,
            )
        }

        CopyHintBubble(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = TooltipOverlap),
        )
    }
}

/**
 * "길게 눌러 복사" 안내 말풍선.
 *
 * 피그마에서는 꼬리가 붙은 단일 벡터인데, SVG 를 벡터 드로어블로 변환하지 않고 직접 그린다.
 * 둥근 사각형에 삼각형 꼬리 하나라 모양이 단순하다.
 */
@Composable
private fun CopyHintBubble(modifier: Modifier = Modifier) {
    val shape = remember {
        TooltipBubbleShape(
            tailWidth = TooltipTailWidth,
            tailHeight = TooltipTailHeight,
            corner = 8.dp,
        )
    }

    Box(
        modifier = modifier
            .height(TooltipHeight)
            .background(MoaMapPrimitiveColors.Yellow100, shape)
            .border(1.dp, MoaMapPrimitiveColors.Yellow500, shape)
            // 꼬리 몫을 빼야 글자가 몸통 한가운데 온다.
            .padding(top = TooltipTailHeight)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "초대코드를 길게 눌러 복사해보세요",
            style = MoaMapTheme.typography.caption2,
            color = MoaMapPrimitiveColors.Gray500,
        )
    }
}

/**
 * 위쪽 가운데에 꼬리가 달린 말풍선.
 *
 * 둥근 사각형과 삼각형을 따로 그리면 테두리가 두 도형의 윤곽을 각각 따라가, 꼬리 아래에
 * 가로줄이 남고 꼬리가 떠 보인다. **끊기지 않는 하나의 경로로 그려야 한다.**
 */
private class TooltipBubbleShape(
    private val tailWidth: Dp,
    private val tailHeight: Dp,
    private val corner: Dp,
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val tailW = with(density) { tailWidth.toPx() }
        val tailH = with(density) { tailHeight.toPx() }
        val r = with(density) { corner.toPx() }
        val w = size.width
        val h = size.height
        val centerX = w / 2f
        val diameter = r * 2f

        val path = Path().apply {
            // 좌상단 모서리 끝에서 시작해 시계 방향으로 한 바퀴 돈다.
            moveTo(r, tailH)

            // 위쪽 변 - 가운데에서 꼬리로 솟았다가 내려온다.
            lineTo(centerX - tailW / 2f, tailH)
            lineTo(centerX, 0f)
            lineTo(centerX + tailW / 2f, tailH)
            lineTo(w - r, tailH)

            arcTo(Rect(w - diameter, tailH, w, tailH + diameter), 270f, 90f, false)
            lineTo(w, h - r)
            arcTo(Rect(w - diameter, h - diameter, w, h), 0f, 90f, false)
            lineTo(r, h)
            arcTo(Rect(0f, h - diameter, diameter, h), 90f, 90f, false)
            lineTo(0f, tailH + r)
            arcTo(Rect(0f, tailH, diameter, tailH + diameter), 180f, 90f, false)

            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
private fun DialogButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ButtonHeight)
            .clip(ButtonShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MoaMapTheme.typography.button2,
            color = MoaMapTheme.colors.textWhite,
        )
    }
}

/** 딥링크가 없어 코드 텍스트만 보낸다. */
private fun Context.shareInviteCode(mapName: String, inviteCode: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "$mapName 지도에 초대합니다.\n초대코드: $inviteCode")
    }
    startActivity(Intent.createChooser(intent, "초대코드 공유"))
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun InviteCodeDialogPreview() {
    MoaMapTheme {
        InviteCodeDialog(
            mapName = "성수 카페 투어",
            inviteCode = "A1B2C3",
            onDismiss = {},
        )
    }
}
