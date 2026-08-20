package com.moamap.app.core.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 파일 I/O 대신 메모리 저장소를 끼운다. */
private class InMemoryPreferences : DataStore<Preferences> {

    private var stored: Preferences = emptyPreferences()

    var readCount: Int = 0
        private set

    override val data: Flow<Preferences> = flow {
        readCount++
        emit(stored)
    }

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences,
    ): Preferences {
        stored = transform(stored)
        return stored
    }
}

class DataStoreCurrentUserStoreTest {

    private val dataStore = InMemoryPreferences()
    private val store = DataStoreCurrentUserStore(dataStore)

    @Test
    fun `저장한 식별자를 다시 읽는다`() = runTest {
        store.save(42L)

        assertEquals(42L, store.load())
    }

    @Test
    fun `저장한 적 없으면 비어 있다`() = runTest {
        assertNull(store.load())
    }

    @Test
    fun `지우면 비어 있다`() = runTest {
        store.save(42L)

        store.clear()

        assertNull(store.load())
    }

    /** 다른 계정으로 로그인하면 새 값으로 덮어써야 한다. */
    @Test
    fun `다시 저장하면 새 값으로 바뀐다`() = runTest {
        store.save(42L)

        store.save(7L)

        assertEquals(7L, store.load())
    }

    /**
     * 0 은 서버가 식별자를 주지 않았다는 뜻이라 저장하지 않는다. 그대로 두면 아무에게도
     * 속하지 않는 값이 내 식별자 행세를 한다.
     */
    @Test
    fun `0 은 저장하지 않는다`() = runTest {
        store.save(0L)

        assertNull(store.load())
    }

    /** 매번 디스크를 때리지 않는다. 후기 목록처럼 자주 부르는 자리에서 쓰인다. */
    @Test
    fun `한 번 읽고 나면 디스크를 다시 읽지 않는다`() = runTest {
        store.load()
        store.load()

        assertEquals(1, dataStore.readCount)
    }

    /** 저장하면서 캐시가 채워지므로 그 뒤로는 디스크를 볼 이유가 없다. */
    @Test
    fun `저장한 뒤에는 디스크를 읽지 않는다`() = runTest {
        store.save(42L)

        assertEquals(42L, store.load())
        assertEquals(0, dataStore.readCount)
    }
}
