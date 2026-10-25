package com.moamap.app.feature.notification.presentation

import com.moamap.app.feature.notification.data.repository.notificationKindOf
import com.moamap.app.feature.notification.domain.model.NotificationKind
import com.moamap.app.feature.notification.domain.model.UserNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class NotificationModelsTest {

    private val seoul = TimeZone.getTimeZone("Asia/Seoul")

    private fun notification(kind: NotificationKind, targetId: Long? = 3L, mapId: Long? = 10L) =
        UserNotification(1L, kind, "문구", targetId, mapId, read = false, createdAtMillis = null)

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance(seoul).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis

    @Test
    fun `지도 알림은 그 지도로 간다`() {
        assertEquals(
            NotificationDestination.Map(10L),
            notification(NotificationKind.MapAdminAssigned).destination(),
        )
        // 서버가 지도를 비워 보내도 대상이 지도다.
        assertEquals(
            NotificationDestination.Map(3L),
            notification(NotificationKind.MapOwnerTransferred, mapId = null).destination(),
        )
        assertEquals(NotificationDestination.Map(10L), notification(NotificationKind.PlaceApproved).destination())
        assertEquals(NotificationDestination.Map(10L), notification(NotificationKind.PlaceCommented).destination())
    }

    @Test
    fun `게시글 댓글은 그 게시물로 간다`() {
        assertEquals(
            NotificationDestination.Post(mapId = 10L, postId = 3L),
            notification(NotificationKind.MapPostCommented).destination(),
        )
        assertNull(notification(NotificationKind.MapPostCommented, mapId = null).destination())
    }

    @Test
    fun `반려·문의·신고·모르는 종류는 이동하지 않는다`() {
        listOf(
            NotificationKind.PlaceRejected,
            NotificationKind.InquiryAnswered,
            NotificationKind.PlaceReported,
            NotificationKind.ReportReceived,
            NotificationKind.Unknown,
        ).forEach { kind -> assertNull(kind.name, notification(kind).destination()) }
    }

    @Test
    fun `모르는 종류는 Unknown 이다`() {
        assertEquals(NotificationKind.MapPostCommented, notificationKindOf("MAP_POST_COMMENTED"))
        assertEquals(NotificationKind.Unknown, notificationKindOf("SOMETHING_NEW"))
        assertEquals(NotificationKind.Unknown, notificationKindOf(null))
    }

    @Test
    fun `시각은 지난 만큼 짧게 쓴다`() {
        val now = at(2026, 10, 26, 15, 30)

        assertEquals("방금", formatNotificationTime(now - 30_000, now, seoul))
        // 기기 시계가 늦어 서버 시각이 앞이어도 방금이다.
        assertEquals("방금", formatNotificationTime(now + 60_000, now, seoul))
        assertEquals("5분 전", formatNotificationTime(at(2026, 10, 26, 15, 25), now, seoul))
        assertEquals("3시간 전", formatNotificationTime(at(2026, 10, 26, 12, 10), now, seoul))
        assertEquals("15시간 전", formatNotificationTime(at(2026, 10, 26, 0, 10), now, seoul))
        assertEquals("어제", formatNotificationTime(at(2026, 10, 25, 23, 50), now, seoul))
        assertEquals("2일 전", formatNotificationTime(at(2026, 10, 24, 9), now, seoul))
        assertEquals("7일 전", formatNotificationTime(at(2026, 10, 19, 9), now, seoul))
        assertEquals("2026.10.18", formatNotificationTime(at(2026, 10, 18, 9), now, seoul))
    }
}
