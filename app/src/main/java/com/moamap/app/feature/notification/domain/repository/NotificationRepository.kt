package com.moamap.app.feature.notification.domain.repository

import com.moamap.app.feature.notification.domain.model.NotificationPage

interface NotificationRepository {
    /** [page] 는 0 부터 센다. */
    suspend fun getNotifications(page: Int, size: Int): NotificationPage

    suspend fun markRead(id: Long)

    suspend fun markAllRead()
}
