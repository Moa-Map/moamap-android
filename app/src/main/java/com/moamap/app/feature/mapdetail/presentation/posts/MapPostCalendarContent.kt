package com.moamap.app.feature.mapdetail.presentation.posts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.component.compatibleShadow
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.mapdetail.domain.model.MapPost
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val WeekdayLabels = listOf("월", "화", "수", "목", "금", "토", "일")

private const val DAYS_IN_WEEK = 7

/** 시안의 날짜 칸 간격. 가로·세로가 같다. */
private val DayGap = 8.dp

/** 확대 카드의 사진 높이. */
private val ExpandedPhotoHeight = 313.dp

private val ExpandedCardShape = RoundedCornerShape(16.dp)
private val ExpandedPhotoShape = RoundedCornerShape(12.dp)

private const val POST_DATE_PATTERN = "yyyy. MM. dd."

/**
 * 로그 탭 달력 형식.
 *
 * 위에는 한 달 달력, 아래에는 고른 날의 게시물이 이어진다. 게시물이 많은 날은 달력까지 함께
 * 밀려 올라가도록 한 목록 안에 둔다 - 달력을 고정하면 작은 화면에서 게시물을 볼 자리가 거의 없다.
 */
@Composable
internal fun MapPostCalendarContent(
    state: MapPostCalendarUiState,
    onViewModeSelect: (MapPostViewMode) -> Unit,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit,
    onDayClick: (CalendarDay) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val month = state.month
    // 게시물·달·날이 바뀔 때만 다시 묶는다. 재구성마다 전체 게시물을 훑지 않게 한다.
    val covers = remember(state.posts, month) {
        month?.let { state.posts.coverPhotosIn(it) }.orEmpty()
    }
    val dayPosts = remember(state.posts, state.selectedDay) {
        state.selectedDay?.let { state.posts.postsOn(it) }.orEmpty()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MoaMapDimens.ScreenHorizontalPadding,
            end = MoaMapDimens.ScreenHorizontalPadding,
            top = TabBarClearance,
            bottom = BottomClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "toolbar") {
            PostsToolbar(viewMode = MapPostViewMode.Calendar, onViewModeSelect = onViewModeSelect)
        }

        if (month == null) return@LazyColumn

        item(key = "month") {
            MonthHeader(
                month = month,
                onPreviousClick = onPreviousMonthClick,
                onNextClick = onNextMonthClick,
            )
        }

        item(key = "calendar") {
            MonthGrid(
                month = month,
                covers = covers,
                selectedDay = state.selectedDay,
                onDayClick = onDayClick,
                modifier = Modifier.padding(bottom = 20.dp),
            )
        }

        when {
            state.loading -> item(key = "loading") { CenteredNotice { Progress() } }

            state.errorMessage != null -> item(key = "error") {
                CenteredNotice { ErrorNotice(message = state.errorMessage, onRetryClick = onRetryClick) }
            }

            else -> {
                val selectedDay = state.selectedDay
                if (selectedDay != null) {
                    item(key = "day-title") {
                        Text(
                            text = "${selectedDay.dayOfMonth}일",
                            style = MoaMapTheme.typography.subtitle1,
                            color = MoaMapTheme.colors.textNormal,
                        )
                    }
                }

                if (dayPosts.isEmpty()) {
                    item(key = "day-empty") { EmptyDayCard() }
                } else {
                    items(dayPosts, key = { post -> "day-post-${post.id}" }) { post ->
                        ExpandedPostCard(post = post)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    month: CalendarMonth,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonthArrow(
            iconRes = R.drawable.ic_arrow_left,
            contentDescription = "이전 달",
            onClick = onPreviousClick,
        )
        Text(
            text = month.label,
            style = MoaMapTheme.typography.button0,
            color = MoaMapTheme.colors.textNormal,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        MonthArrow(
            iconRes = R.drawable.ic_arrow_right,
            contentDescription = "다음 달",
            onClick = onNextClick,
        )
    }
}

@Composable
private fun MonthArrow(iconRes: Int, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = MoaMapTheme.colors.textNormal,
            modifier = Modifier.size(28.dp),
        )
    }
}

/**
 * 월요일부터 시작하는 한 달 달력.
 *
 * 1일 앞은 빈칸으로 두고, 마지막 주는 남는 칸을 빈칸으로 채워 줄 폭을 맞춘다.
 */
@Composable
private fun MonthGrid(
    month: CalendarMonth,
    covers: Map<Int, String>,
    selectedDay: CalendarDay?,
    onDayClick: (CalendarDay) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cells: List<Int?> = remember(month) {
        val blanks = List(month.leadingBlankDays) { null }
        val days = (1..month.daysInMonth).toList()
        val filled = blanks + days
        filled + List((DAYS_IN_WEEK - filled.size % DAYS_IN_WEEK) % DAYS_IN_WEEK) { null }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(DayGap),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(DayGap)) {
            WeekdayLabels.forEach { label ->
                Text(
                    text = label,
                    style = MoaMapTheme.typography.body3,
                    color = MoaMapPrimitiveColors.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        cells.chunked(DAYS_IN_WEEK).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(DayGap)) {
                week.forEach { dayOfMonth ->
                    if (dayOfMonth == null) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val day = month.day(dayOfMonth)
                        DayCell(
                            dayOfMonth = dayOfMonth,
                            coverUrl = covers[dayOfMonth],
                            selected = day == selectedDay,
                            onClick = { onDayClick(day) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 날짜 칸. 그날 가장 최신 사진 게시물의 사진으로 채우고, 없으면 회색이다.
 *
 * 숫자는 게시물 수가 아니라 날짜다. 사진 위에서도 읽히게 흰색으로 둔다.
 */
@Composable
private fun DayCell(
    dayOfMonth: Int,
    coverUrl: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.aspectRatio(1f)) {
        if (selected) {
            // 시안의 파란 빛. 칸 뒤에 번진 그림자를 깔아 칸 테두리 밖으로 퍼지게 한다.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .compatibleShadow(
                        shape = CircleShape,
                        blurRadius = 10.dp,
                        color = MoaMapPrimitiveColors.Blue500.copy(alpha = 0.7f),
                    ),
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(MoaMapTheme.colors.lineAlternative)
                .then(
                    if (selected) Modifier.border(1.dp, MoaMapPrimitiveColors.Blue500, CircleShape) else Modifier,
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Text(
                text = dayOfMonth.toString(),
                style = MoaMapTheme.typography.caption0,
                color = MoaMapTheme.colors.textWhite,
            )
        }
    }
}

@Composable
private fun EmptyDayCard() {
    ShadowedSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpandedCardShape,
    ) {
        Text(
            text = "이 날은 게시물이 없어요",
            style = MoaMapTheme.typography.body2,
            color = MoaMapTheme.colors.textAssistive,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
        )
    }
}

/**
 * 고른 날의 게시물을 크게 보여주는 카드.
 *
 * 사진은 첫 장만 보여준다. 여러 장을 넘겨 보는 방식은 팀 논의 후 정한다. 링크 아이콘은 공유할
 * 곳이 아직 없어 그리지 않는다.
 */
@Composable
internal fun ExpandedPostCard(post: MapPost, modifier: Modifier = Modifier) {
    val photoUrl = post.imageUrls.firstOrNull()
    val placeName = post.placeNames.firstOrNull()
    val dateLabel = remember(post.createdAtMillis) {
        post.createdAtMillis?.let { millis ->
            SimpleDateFormat(POST_DATE_PATTERN, Locale.KOREA).format(Date(millis))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(ExpandedCardShape)
            .background(MoaMapTheme.colors.textWhite)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (photoUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ExpandedPhotoHeight)
                    .clip(ExpandedPhotoShape)
                    .background(MoaMapTheme.colors.lineAlternative)
                    .border(1.dp, MoaMapTheme.colors.textWhite, ExpandedPhotoShape),
            ) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (placeName != null) {
                    PlacePill(
                        name = placeName,
                        textStyle = MoaMapTheme.typography.body2,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                    )
                }
            }
        } else if (placeName != null) {
            PlacePill(
                name = placeName,
                textStyle = MoaMapTheme.typography.body2,
                modifier = Modifier.align(Alignment.End),
            )
        }

        if (post.content.isNotBlank()) {
            Text(
                text = post.content,
                style = MoaMapTheme.typography.subtitle2,
                color = MoaMapTheme.colors.textNormal,
            )
        }

        if (dateLabel != null) {
            Text(
                text = dateLabel,
                style = MoaMapTheme.typography.body2,
                color = MoaMapTheme.colors.textAlternative,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F9FA, widthDp = 393, heightDp = 852)
@Composable
private fun MapPostCalendarContentPreview() {
    val month = CalendarMonth(2026, 8)
    MoaMapTheme {
        MapPostCalendarContent(
            state = MapPostCalendarUiState(
                month = month,
                selectedDay = month.day(26),
            ),
            onViewModeSelect = {},
            onPreviousMonthClick = {},
            onNextMonthClick = {},
            onDayClick = {},
            onRetryClick = {},
        )
    }
}
