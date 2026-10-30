package com.moamap.app.feature.mypage.domain

import com.moamap.app.feature.mypage.domain.model.BlockedUser
import com.moamap.app.feature.mypage.domain.repository.BlockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 내가 차단한 사람. 앱 전체가 하나를 같이 본다.
 *
 * 서버도 목록에서 걸러 주지만, 차단하면 다시 읽기를 기다리지 않고 지금 화면에서 바로 지워야 한다(심사 요건).
 * 서버가 차단 목록을 못 읽은 채 거르지 않고 준 목록도 여기서 한 번 더 거른다(백엔드 #175).
 */
@Singleton
class BlockedUserStore @Inject constructor(
    private val repository: BlockRepository,
) {
    private val _ids = MutableStateFlow<Set<Long>>(emptySet())
    val ids: StateFlow<Set<Long>> = _ids.asStateFlow()

    /** 차단·해제마다 늘린다. 그 사이에 늦게 온 목록이 방금 한 차단을 덮지 않게 한다. */
    private var revision = 0

    /**
     * 서버 목록으로 맞추고 그 목록을 돌려준다.
     *
     * shortcut: 로그아웃 때 비우지 않는다 - 지도를 열 때마다 다시 읽어 다음 계정 것으로 바뀐다. 다른 화면에서도
     * 거르게 되면 로그아웃 흐름에서 비울 것.
     */
    suspend fun reload(): List<BlockedUser> {
        val started = revision
        val users = repository.getBlockedUsers()
        if (revision == started) _ids.value = users.mapTo(HashSet()) { user -> user.userId }
        return users
    }

    suspend fun block(userId: Long) {
        repository.block(userId)
        revision++
        _ids.update { ids -> ids + userId }
    }

    suspend fun unblock(userId: Long) {
        repository.unblock(userId)
        revision++
        _ids.update { ids -> ids - userId }
    }
}
