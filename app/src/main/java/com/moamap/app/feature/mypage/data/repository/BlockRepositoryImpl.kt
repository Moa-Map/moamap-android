package com.moamap.app.feature.mypage.data.repository

import com.moamap.app.feature.mypage.data.remote.BlockService
import com.moamap.app.feature.mypage.data.remote.BlockedUserDto
import com.moamap.app.feature.mypage.domain.model.BlockedUser
import com.moamap.app.feature.mypage.domain.repository.BlockRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class BlockRepositoryImpl @Inject constructor(
    private val blockService: BlockService,
) : BlockRepository {

    override suspend fun getBlockedUsers(): List<BlockedUser> =
        blockService.getBlockedUsers().map { dto -> dto.toBlockedUser() }

    override suspend fun block(userId: Long) = blockService.block(userId)

    override suspend fun unblock(userId: Long) = blockService.unblock(userId)
}

internal fun BlockedUserDto.toBlockedUser(): BlockedUser = BlockedUser(
    userId = userId,
    nickname = nickname.orEmpty(),
    profileImageUrl = profileImageUrl?.takeIf(String::isNotBlank),
)
