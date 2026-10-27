package com.moamap.app.feature.mypage.domain.repository

import com.moamap.app.feature.mypage.domain.model.BlockedUser

interface BlockRepository {

    /** 최근 차단한 순. */
    suspend fun getBlockedUsers(): List<BlockedUser>

    suspend fun unblock(userId: Long)
}
