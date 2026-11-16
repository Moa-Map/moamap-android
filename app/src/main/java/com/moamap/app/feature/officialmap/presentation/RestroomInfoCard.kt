package com.moamap.app.feature.officialmap.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.officialmap.domain.model.RestroomDetail
import com.moamap.app.feature.officialmap.domain.model.RestroomMarker

/**
 * 카드에 쓰는 「항목 - 내용」 줄. 비어 있는 항목은 줄째 뺀다.
 *
 * 칸 수는 0인 것을 빼고 적는다. 공공데이터는 없는 시설을 0으로 채워 온다.
 */
internal fun RestroomDetail.toInfoRows(): List<Pair<String, String>> = listOfNotNull(
    address?.let { "주소" to it },
    listOfNotNull(openHours, openHoursDetail).joinToString(" · ").ifEmpty { null }
        ?.let { "개방시간" to it },
    counts("대변기" to maleToilet, "소변기" to maleUrinal)?.let { "남자" to it },
    counts("대변기" to femaleToilet)?.let { "여자" to it },
    counts(
        "남 대변기" to maleDisabledToilet,
        "남 소변기" to maleDisabledUrinal,
        "여 대변기" to femaleDisabledToilet,
    )?.let { "장애인용" to it },
    counts(
        "남 대변기" to maleChildToilet,
        "남 소변기" to maleChildUrinal,
        "여 대변기" to femaleChildToilet,
    )?.let { "어린이용" to it },
    listOfNotNull(
        "기저귀 교환대".takeIf { diaperTable },
        "비상벨".takeIf { emergencyBell },
        "입구 CCTV".takeIf { entranceCctv },
    ).joinToString(" · ").ifEmpty { null }?.let { "편의시설" to it },
    listOfNotNull(managerOrg, phone).joinToString(" · ").ifEmpty { null }?.let { "관리기관" to it },
)

private fun counts(vararg items: Pair<String, Int>): String? =
    items.filter { (_, count) -> count > 0 }
        .joinToString(" · ") { (name, count) -> "$name $count" }
        .ifEmpty { null }

/**
 * 마커를 눌렀을 때 아래에 뜨는 화장실 정보.
 *
 * 이름·분류는 마커에 이미 있어 바로 그리고, 나머지는 상세를 받아 채운다.
 */
@Composable
internal fun RestroomInfoCard(
    restroom: RestroomMarker,
    detail: RestroomDetailState?,
    modifier: Modifier = Modifier,
) {
    // 다른 화장실로 바꿔 누르면 받는 동안 앞 내용 모양으로 자리를 지켜 카드 높이가 출렁이지 않게 한다.
    var lastLoaded by remember { mutableStateOf<RestroomDetail?>(null) }
    LaunchedEffect(detail) {
        if (detail is RestroomDetailState.Loaded) lastLoaded = detail.detail
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MoaMapPrimitiveColors.White,
        shadowElevation = 5.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = restroom.name,
                    style = MoaMapTheme.typography.subtitle2,
                    color = MoaMapTheme.colors.textNormal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // 이름이 길어도 분류를 밀어내지 않게 남는 폭만 차지한다.
                    modifier = Modifier.weight(1f, fill = false),
                )
                restroom.category?.let { category ->
                    Text(
                        text = category,
                        style = MoaMapTheme.typography.caption0,
                        color = MoaMapTheme.colors.textAssistive,
                        maxLines = 1,
                    )
                }
            }

            when (detail) {
                is RestroomDetailState.Loaded -> RestroomInfoRows(detail.detail)
                RestroomDetailState.Failed -> CardMessage("정보를 불러오지 못했어요. 화장실을 다시 누르면 다시 불러와요")
                RestroomDetailState.Loading, null -> lastLoaded?.let { previous ->
                    RestroomInfoRows(previous, placeholder = true)
                } ?: CardMessage(LoadingMessage)
            }
        }
    }
}

private const val LoadingMessage = "정보를 불러오는 중이에요"

/**
 * @param placeholder 앞 화장실 내용으로 자리만 지킨다. 글자는 회색 막대로 가리고 읽기 도구에는 불러오는 중이라고 알린다.
 */
@Composable
private fun RestroomInfoRows(detail: RestroomDetail, placeholder: Boolean = false) {
    val bar = if (placeholder) Modifier.background(MoaMapPrimitiveColors.Gray50, RoundedCornerShape(4.dp)) else Modifier
    fun Color.orHidden(): Color = if (placeholder) Color.Transparent else this
    Column(
        modifier = if (placeholder) Modifier.clearAndSetSemantics { contentDescription = LoadingMessage } else Modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        detail.toInfoRows().forEach { (label, value) ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.width(56.dp)) {
                    Text(
                        text = label,
                        style = MoaMapTheme.typography.caption0,
                        color = MoaMapTheme.colors.textAssistive.orHidden(),
                        modifier = bar,
                    )
                }
                Text(
                    text = value,
                    style = MoaMapTheme.typography.body3,
                    color = MoaMapTheme.colors.textAlternative.orHidden(),
                    modifier = bar,
                )
            }
        }
        detail.dataRefDate?.let { date ->
            Text(
                text = "${date.replace('-', '.')} 기준 공공데이터",
                style = MoaMapTheme.typography.caption0,
                color = MoaMapTheme.colors.textAssistive.orHidden(),
                modifier = Modifier.padding(top = 4.dp).then(bar),
            )
        }
    }
}

@Composable
private fun CardMessage(text: String) {
    Text(
        text = text,
        style = MoaMapTheme.typography.body3,
        color = MoaMapTheme.colors.textAssistive,
    )
}
