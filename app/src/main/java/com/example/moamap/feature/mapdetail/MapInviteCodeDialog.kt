package com.example.moamap.feature.mapdetail

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.moamap.R
import com.example.moamap.core.designsystem.component.MoaMapDialog
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import kotlinx.coroutines.launch

private val DialogWidth = 300.dp
private val DialogContentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp)

private val CodeBoxShape = RoundedCornerShape(12.dp)
private val CodeBoxPadding = PaddingValues(horizontal = 60.dp, vertical = 20.dp)

private val ButtonHeight = 44.dp
private val ButtonShape = RoundedCornerShape(8.dp)

private val CopyIconSize = 16.dp

/**
 * 프라이빗 지도 안에서 초대 코드를 다시 꺼내 보는 모달.
 *
 * 지도를 만든 직후 뜨는
 * [com.example.moamap.feature.collection.presentation.createmap.InviteCodeDialog] 와 생김새가
 * 비슷하지만 쓰임이 다르다. 저쪽은 방금 만든 걸 알리는 자리라 코드를 눌러 복사하라는 안내를
 * 말풍선으로 띄우고, 여기는 코드를 보러 일부러 연 자리라 복사 줄을 박스 안에 둔다.
 *
 * 코드는 상세 조회 응답에 실려 오므로 여기서 따로 조회하지 않는다.
 */
@Composable
internal fun MapInviteCodeDialog(
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
        verticalSpacing = 24.dp,
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
                    text = "초대코드",
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
                onCopyClick = {
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DialogButton(
                text = "닫기",
                color = MoaMapPrimitiveColors.Gray200,
                onClick = onDismiss,
            )
            DialogButton(
                text = "공유하기",
                color = MoaMapPrimitiveColors.Blue500,
                onClick = { context.shareInviteCode(mapName = mapName, inviteCode = inviteCode) },
            )
        }
    }
}

/**
 * 코드와 복사 줄을 담은 박스.
 *
 * 복사는 박스 전체가 아니라 안내 줄에만 건다. 코드 글자를 눌러도 아무 일이 없어야 길게 눌러
 * 텍스트를 고르는 기본 동작과 부딪히지 않고, 어디를 눌러야 복사되는지도 한눈에 보인다.
 */
@Composable
private fun InviteCodeBox(
    inviteCode: String,
    onCopyClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CodeBoxShape)
            .background(MoaMapTheme.colors.backgroundSecondary)
            .border(1.dp, MoaMapTheme.colors.lineNormal, CodeBoxShape)
            .padding(CodeBoxPadding),
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

        Row(
            modifier = Modifier.clickable(onClick = onCopyClick),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_copy),
                contentDescription = null,
                tint = MoaMapPrimitiveColors.Gray300,
                modifier = Modifier.size(CopyIconSize),
            )
            Text(
                text = "초대 코드 복사하기",
                style = MoaMapTheme.typography.caption2,
                color = MoaMapPrimitiveColors.Gray300,
            )
        }
    }
}

@Composable
private fun RowScope.DialogButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(1f)
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
private fun MapInviteCodeDialogPreview() {
    MoaMapTheme {
        MapInviteCodeDialog(
            mapName = "우리끼리 맛집",
            inviteCode = "A1B2C3",
            onDismiss = {},
        )
    }
}
