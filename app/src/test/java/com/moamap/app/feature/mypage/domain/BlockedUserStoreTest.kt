package com.moamap.app.feature.mypage.domain

import com.moamap.app.feature.mypage.domain.model.BlockedUser
import com.moamap.app.feature.mypage.domain.repository.BlockRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BlockedUserStoreTest {

    private class FakeRepository : BlockRepository {
        var users = listOf(BlockedUser(1L, "가", null))
        var pending: CompletableDeferred<Unit>? = null

        override suspend fun getBlockedUsers(): List<BlockedUser> {
            pending?.await()
            return users
        }

        override suspend fun block(userId: Long) = Unit
        override suspend fun unblock(userId: Long) = Unit
    }

    @Test
    fun `서버 목록으로 맞추고 차단·해제는 바로 반영한다`() = runTest {
        val store = BlockedUserStore(FakeRepository())

        store.reload()
        assertEquals(setOf(1L), store.ids.value)

        store.block(2L)
        assertEquals(setOf(1L, 2L), store.ids.value)

        store.unblock(1L)
        assertEquals(setOf(2L), store.ids.value)
    }

    @Test
    fun `차단하는 사이에 늦게 온 목록은 방금 한 차단을 덮지 않는다`() = runTest {
        val repository = FakeRepository().apply { pending = CompletableDeferred() }
        val store = BlockedUserStore(repository)

        val reloading = async { store.reload() }
        testScheduler.runCurrent()
        store.block(2L)
        repository.pending?.complete(Unit)
        reloading.await()

        assertEquals(setOf(2L), store.ids.value)
    }
}
