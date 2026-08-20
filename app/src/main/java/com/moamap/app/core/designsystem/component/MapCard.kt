package com.moamap.app.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moamap.app.R
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme

/**
 * 지도 목록에 쓰이는 카드.
 *
 * 탐색 탭의 커뮤니티 지도와 공식지도 탭이 같은 카드를 공유하며,
 * 인증 배지 노출 여부(`verified`)와 해시태그 유무로 두 화면을 구분한다.
 */
@Composable
fun MapCard(
    title: String,
    description: String,
    memberCount: String,
    placeCount: String,
    joined: Boolean,
    onClick: () -> Unit,
    onJoinClick: () -> Unit,
    modifier: Modifier = Modifier,
    verified: Boolean = false,
    hashtags: List<String> = emptyList(),
    descriptionStyle: TextStyle = MoaMapTheme.typography.body2,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MoaMapPrimitiveColors.White,
        shadowElevation = 5.dp,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = title,
                                style = MoaMapTheme.typography.subtitle2,
                                color = MoaMapTheme.colors.textNormal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (verified) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_verify_filled),
                                    contentDescription = "공식 인증",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                        Text(
                            text = description,
                            style = descriptionStyle,
                            color = MoaMapTheme.colors.textAlternative,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    JoinButton(joined = joined, onClick = onJoinClick)
                }
                if (hashtags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        hashtags.forEach { hashtag ->
                            Text(
                                text = "# $hashtag",
                                style = MoaMapTheme.typography.caption0,
                                color = MoaMapPrimitiveColors.Blue800,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MapCardMeta(
                    iconRes = R.drawable.ic_person,
                    text = memberCount,
                    contentDescription = "참여 인원",
                )
                MapCardMeta(
                    iconRes = R.drawable.ic_location,
                    text = placeCount,
                    contentDescription = "등록 장소",
                )
            }
        }
    }
}

@Composable
private fun JoinButton(
    joined: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = if (joined) MoaMapPrimitiveColors.Gray300 else MoaMapPrimitiveColors.Blue500,
        onClick = onClick,
    ) {
        Text(
            text = if (joined) "참여중" else "참여하기",
            style = MoaMapTheme.typography.button3,
            color = MoaMapTheme.colors.textWhite,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun MapCardMeta(
    @DrawableRes iconRes: Int,
    text: String,
    contentDescription: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = MoaMapTheme.colors.textAssistive,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = text,
            style = MoaMapTheme.typography.caption0,
            color = MoaMapTheme.colors.textAssistive,
            maxLines = 1,
        )
    }
}
