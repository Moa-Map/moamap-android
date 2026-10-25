package com.moamap.app.feature.notification.presentation

import androidx.annotation.DrawableRes
import com.moamap.app.R
import com.moamap.app.feature.notification.domain.model.NotificationKind
import com.moamap.app.feature.notification.domain.model.UserNotification
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** 알림을 눌렀을 때 갈 곳. */
sealed interface NotificationDestination {
    data class Map(val mapId: Long) : NotificationDestination

    /** 지도 상세를 열고 그 위에 게시물 상세를 띄운다. */
    data class Post(val mapId: Long, val postId: Long) : NotificationDestination
}

/**
 * 눌렀을 때 갈 곳(10-26 사용자 결정). null 이면 읽음으로만 바꾼다.
 *
 * - 관리자 지정·방장 승계·장소 승인·장소 댓글 → 그 지도. 장소 시트까지는 열지 않는다.
 * - 게시글 댓글 → 그 게시물.
 * - 장소 반려(장소가 없을 수 있다)·문의 답변(앱에 문의 목록이 없다)·신고·모르는 종류 → 이동하지 않는다.
 */
internal fun UserNotification.destination(): NotificationDestination? = when (kind) {
    // 지도 알림은 대상이 곧 지도다. 서버가 mapId 를 비워 보내도 targetId 로 간다.
    NotificationKind.MapAdminAssigned, NotificationKind.MapOwnerTransferred ->
        (mapId ?: targetId)?.let(NotificationDestination::Map)
    NotificationKind.PlaceApproved, NotificationKind.PlaceCommented -> mapId?.let(NotificationDestination::Map)
    NotificationKind.MapPostCommented -> {
        val map = mapId
        val post = targetId
        if (map != null && post != null) NotificationDestination.Post(map, post) else null
    }
    NotificationKind.PlaceRejected,
    NotificationKind.InquiryAnswered,
    NotificationKind.PlaceReported,
    NotificationKind.ReportReceived,
    NotificationKind.Unknown,
    -> null
}

/**
 * 카드 왼쪽 아이콘(10-26 사용자 결정). 시안 아이콘은 참여·기록·지도·체크·경고 다섯이라 서버 종류에 나눠 붙인다.
 * 그림마다 색이 들어 있어 칠하지 않고 그린다.
 */
@DrawableRes
internal fun NotificationKind.iconRes(): Int = when (this) {
    NotificationKind.PlaceApproved, NotificationKind.InquiryAnswered -> R.drawable.ic_notification_check
    NotificationKind.PlaceRejected, NotificationKind.PlaceReported, NotificationKind.ReportReceived ->
        R.drawable.ic_notification_alert
    NotificationKind.PlaceCommented, NotificationKind.MapPostCommented -> R.drawable.ic_edit
    NotificationKind.MapAdminAssigned, NotificationKind.MapOwnerTransferred -> R.drawable.ic_notification_group
    NotificationKind.Unknown -> R.drawable.ic_map
}

private const val MINUTE_MILLIS = 60_000L
private const val HOUR_MILLIS = 60 * MINUTE_MILLIS

/** 「N일 전」으로 보여 주는 가장 오래된 날. 그보다 오래되면 날짜로 쓴다. */
private const val MAX_RELATIVE_DAYS = 7

/**
 * 알림 시각(10-26 사용자 결정). 1분 안 「방금」, 1시간 안 「N분 전」, 같은 날 「N시간 전」, 하루 전 날 「어제」,
 * 7일까지 「N일 전」, 그 뒤로 `2026.10.26`. 날은 기기 시간대 기준이다.
 *
 * 기기 시계가 서버보다 늦어 [createdAtMillis] 가 앞이면 「방금」으로 둔다.
 *
 * `java.time` 은 쓰지 않는다 - minSdk 24 에 desugaring 이 꺼져 있다(`PostCalendar` 참고).
 */
internal fun formatNotificationTime(
    createdAtMillis: Long,
    nowMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault(),
): String {
    val elapsed = nowMillis - createdAtMillis
    if (elapsed < MINUTE_MILLIS) return "방금"
    if (elapsed < HOUR_MILLIS) return "${elapsed / MINUTE_MILLIS}분 전"

    val days = calendarDaysBetween(createdAtMillis, nowMillis, timeZone)
    return when {
        days == 0 -> "${elapsed / HOUR_MILLIS}시간 전"
        days == 1 -> "어제"
        days <= MAX_RELATIVE_DAYS -> "${days}일 전"
        else -> SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
            .apply { this.timeZone = timeZone }
            .format(Date(createdAtMillis))
    }
}

/** 두 시각 사이에 바뀐 날짜 수. 자정을 몇 번 넘었는지다. */
private fun calendarDaysBetween(fromMillis: Long, toMillis: Long, timeZone: TimeZone): Int {
    fun dayStart(millis: Long): Long = Calendar.getInstance(timeZone).apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    // 서머타임으로 하루가 23·25시간인 날도 반올림으로 하루가 된다.
    return Math.round((dayStart(toMillis) - dayStart(fromMillis)) / (24.0 * HOUR_MILLIS)).toInt()
}
