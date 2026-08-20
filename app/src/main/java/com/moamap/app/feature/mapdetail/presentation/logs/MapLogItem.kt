package com.moamap.app.feature.mapdetail.presentation.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme

/** 타임라인 규격. 시안의 "로그" 컴포넌트 값이다. */
private val RailHorizontalPadding = 8.dp
private val DotSize = 8.dp
private val DotTopSpace = 8.dp
private val ItemBottomSpace = 20.dp
private val CardShape = RoundedCornerShape(12.dp)
private val AvatarSize = 24.dp

/**
 * 활동 내역 한 건.
 *
 * 좌측 레일(점 + 세로선)과 우측 내용을 나란히 둔다. 레일의 세로선은 카드 높이만큼 늘어나야
 * 해서 행 높이를 [IntrinsicSize.Min] 으로 잡는다 - 그래야 `fillMaxHeight` 가 내용 높이를
 * 따라간다.
 */
@Composable
internal fun MapLogItem(
    log: MapLogUiModel,
    hasLineAbove: Boolean,
    hasLineBelow: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        LogTimelineRail(
            type = log.type,
            hasLineAbove = hasLineAbove,
            hasLineBelow = hasLineBelow,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = ItemBottomSpace),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LogAuthorRow(log = log)
            LogCard(log = log)
        }
    }
}

/**
 * 점과 세로선.
 *
 * 첫 항목은 점 위로, 마지막 항목은 점 아래로 선을 긋지 않는다. 목록 양 끝에서 선이 허공으로
 * 뻗어 나가면 위아래에 더 있는 것처럼 보인다.
 */
@Composable
private fun LogTimelineRail(
    type: MapLogType,
    hasLineAbove: Boolean,
    hasLineBelow: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(horizontal = RailHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 점을 사용자명 줄 높이에 맞춘다. 위 선이 없는 첫 항목도 같은 자리에 놓여야 한다.
        if (hasLineAbove) {
            TimelineLine(modifier = Modifier.height(DotTopSpace))
        } else {
            Spacer(Modifier.height(DotTopSpace))
        }

        Box(
            modifier = Modifier
                .size(DotSize)
                .clip(CircleShape)
                .background(type.dotColor),
        )

        if (hasLineBelow) {
            TimelineLine(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TimelineLine(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(1.dp)
            .background(MoaMapPrimitiveColors.Gray50),
    )
}

@Composable
private fun LogAuthorRow(log: MapLogUiModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LogAuthor(
            userName = log.userName,
            userImageUrl = log.userImageUrl,
        )
        LogTime(timeAgo = log.timeAgo)
    }
}

/**
 * 로그 카드.
 *
 * 글 한 줄뿐이다. 활동 내역 응답에는 장소 사진이 없어 카드에 얹을 것이 없다.
 */
@Composable
private fun LogCard(log: MapLogUiModel) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MoaMapPrimitiveColors.White)
            .border(1.dp, MoaMapPrimitiveColors.Gray50, CardShape)
            .padding(12.dp),
    ) {
        Text(
            text = log.message,
            style = MoaMapTheme.typography.body2,
            color = MoaMapTheme.colors.textNormal,
        )
    }
}

/** 아바타 + 이름. 알림 카드도 같은 줄을 쓴다. */
@Composable
internal fun LogAuthor(
    userName: String,
    userImageUrl: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(AvatarSize)
                .clip(CircleShape)
                .background(MoaMapPrimitiveColors.Blue50),
        ) {
            if (userImageUrl != null) {
                AsyncImage(
                    model = userImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(AvatarSize),
                )
            }
        }
        Text(
            text = userName,
            style = MoaMapTheme.typography.caption0,
            color = MoaMapTheme.colors.textNormal,
        )
    }
}

@Composable
internal fun LogTime(timeAgo: String, modifier: Modifier = Modifier) {
    Text(
        text = timeAgo,
        style = MoaMapTheme.typography.caption2,
        color = LogTimeColor,
        modifier = modifier,
    )
}

/** 시안 값. 팔레트에 없는 한 번 쓰이는 회색이라 토큰으로 올리지 않는다. */
private val LogTimeColor = Color(0xFF4A4F52)

private val MapLogType.dotColor: Color
    get() = when (this) {
        MapLogType.PlaceAdded -> MoaMapPrimitiveColors.Blue500
        MapLogType.PlaceRemoved -> MoaMapPrimitiveColors.Gray400
        MapLogType.ReviewCreated -> MoaMapPrimitiveColors.Yellow500
    }

@Preview(showBackground = true, widthDp = 393)
@Composable
private fun MapLogItemPreview() {
    MoaMapTheme {
        Column(modifier = Modifier.padding(20.dp)) {
            SampleMapLogs.forEachIndexed { index, log ->
                MapLogItem(
                    log = log,
                    hasLineAbove = index > 0,
                    hasLineBelow = index < SampleMapLogs.lastIndex,
                )
            }
        }
    }
}
