package com.moamap.app.feature.mapdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme

/**
 * 댓글 입력 줄: 흰 입력칸과 오른쪽 보내기 원. 장소 댓글과 로그 게시물 댓글이 같이 쓴다.
 *
 * 시안은 「검색창」 모양(흰 바탕·모서리 12·카드 그림자)이다. [ShadowedSurface] 기본값이 그 모양이다. 그림자는
 * 내용 뒤에 따로 깔아야 한다 - blur 를 입력창에 걸면 글자까지 흐려진다.
 *
 * @param leading 입력칸 왼쪽 끝 자리. 장소 댓글의 사진 첨부 + 가 쓴다. 없으면 글자가 끝에서 16 에서 시작한다.
 * @param onInputPositioned 흰 입력칸의 자리. 사진 고르기 메뉴가 그 바로 위에 붙는다.
 */
@Composable
internal fun CommentInputRow(
    text: String,
    onTextChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    canSend: Boolean,
    sending: Boolean,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
    onInputPositioned: (LayoutCoordinates) -> Unit = {},
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShadowedSurface(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .onGloballyPositioned(onInputPositioned),
        ) {
            Row(
                // + 는 누르는 칸 32 가운데 20 이라 왼쪽 10 이면 그림이 시안처럼 끝에서 16 에 선다.
                modifier = Modifier.padding(start = if (leading != null) 10.dp else 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leading?.invoke()

                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    singleLine = true,
                    textStyle = MoaMapTheme.typography.body2.copy(
                        color = MoaMapTheme.colors.textNormal,
                    ),
                    cursorBrush = SolidColor(MoaMapPrimitiveColors.Blue500),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (text.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = MoaMapTheme.typography.body2,
                                    color = MoaMapTheme.colors.textAssistive,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (canSend) MoaMapPrimitiveColors.Blue500 else MoaMapPrimitiveColors.Gray200,
                )
                .clickable(
                    enabled = canSend,
                    role = Role.Button,
                    // 입력은 여기서 비우지 않는다. 서버가 받아들였는지는 아직 모른다.
                    onClick = onSendClick,
                )
                .semantics { contentDescription = "댓글 보내기" },
            contentAlignment = Alignment.Center,
        ) {
            if (sending) {
                CircularProgressIndicator(
                    color = MoaMapPrimitiveColors.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                // 시안 보내기 원 40 안의 아이콘 32. 앱 ic_send 는 같은 그림을 24 틀로 줄인 것이다.
                Icon(
                    painter = painterResource(R.drawable.ic_send),
                    contentDescription = null,
                    tint = MoaMapPrimitiveColors.White,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}
