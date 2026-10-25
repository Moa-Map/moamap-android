package com.moamap.app.feature.notification.domain.model

import androidx.compose.runtime.Immutable

/** 알림 종류. 앱이 모르는 값([Unknown])도 목록에는 서버 문구로 보여 준다. */
enum class NotificationKind {
    PlaceApproved,
    PlaceRejected,
    PlaceCommented,
    MapPostCommented,
    MapAdminAssigned,
    MapOwnerTransferred,
    InquiryAnswered,
    PlaceReported,
    ReportReceived,
    Unknown,
}

/** 내 알림 한 건. 안드로이드 `Notification` 과 헷갈리지 않게 이름을 달리했다. */
@Immutable
data class UserNotification(
    val id: Long,
    val kind: NotificationKind,
    val message: String,
    val targetId: Long?,
    val mapId: Long?,
    val read: Boolean,
    val createdAtMillis: Long?,
)

data class NotificationPage(
    val notifications: List<UserNotification>,
    val isLast: Boolean,
)
