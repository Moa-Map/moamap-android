package com.moamap.app.feature.notification.data.repository

import com.moamap.app.feature.mapdetail.data.repository.parseServerDateTime
import com.moamap.app.feature.notification.data.remote.NotificationDto
import com.moamap.app.feature.notification.data.remote.NotificationService
import com.moamap.app.feature.notification.domain.model.NotificationKind
import com.moamap.app.feature.notification.domain.model.NotificationPage
import com.moamap.app.feature.notification.domain.model.UserNotification
import com.moamap.app.feature.notification.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class NotificationRepositoryImpl @Inject constructor(
    private val notificationService: NotificationService,
) : NotificationRepository {

    override suspend fun getNotifications(page: Int, size: Int): NotificationPage {
        val response = notificationService.getNotifications(page = page, size = size)
        return NotificationPage(
            notifications = response.content.map { dto -> dto.toUserNotification() },
            isLast = response.last || response.content.isEmpty(),
        )
    }

    override suspend fun markRead(id: Long) = notificationService.markRead(id)

    override suspend fun markAllRead() = notificationService.markAllRead()
}

internal fun NotificationDto.toUserNotification(): UserNotification = UserNotification(
    id = notificationId,
    kind = notificationKindOf(type),
    message = message.orEmpty(),
    targetId = targetId,
    mapId = mapId,
    read = read,
    createdAtMillis = parseServerDateTime(createdAt),
)

internal fun notificationKindOf(raw: String?): NotificationKind = when (raw) {
    "PLACE_APPROVED" -> NotificationKind.PlaceApproved
    "PLACE_REJECTED" -> NotificationKind.PlaceRejected
    "PLACE_COMMENTED" -> NotificationKind.PlaceCommented
    "MAP_POST_COMMENTED" -> NotificationKind.MapPostCommented
    "MAP_ADMIN_ASSIGNED" -> NotificationKind.MapAdminAssigned
    "MAP_OWNER_TRANSFERRED" -> NotificationKind.MapOwnerTransferred
    "INQUIRY_ANSWERED" -> NotificationKind.InquiryAnswered
    "PLACE_REPORTED" -> NotificationKind.PlaceReported
    "REPORT_RECEIVED" -> NotificationKind.ReportReceived
    else -> NotificationKind.Unknown
}
