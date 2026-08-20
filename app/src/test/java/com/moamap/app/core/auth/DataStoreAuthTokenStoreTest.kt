package com.moamap.app.core.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 파일 I/O 대신 메모리 저장소를 끼워 캐시 동작까지 확인한다.
 * 읽은 횟수를 세서 "매 요청마다 디스크를 읽지 않는다"는 계약을 검증한다.
 */
private class InMemoryPreferencesDataStore : DataStore<Preferences> {

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

class DataStoreAuthTokenStoreTest {

    private val dataStore = InMemoryPreferencesDataStore()
    private val store = DataStoreAuthTokenStore(dataStore)

    private val token = AuthToken(accessToken = "access-1", refreshToken = "refresh-1")

    @Test
    fun `저장한 토큰을 그대로 돌려준다`() = runTest {
        store.save(token)

        assertEquals(token, store.load())
    }

    @Test
    fun `저장한 적이 없으면 세션이 없다`() = runTest {
        assertNull(store.load())
    }

    @Test
    fun `액세스 토큰만 남아 있으면 세션이 없는 것으로 본다`() = runTest {
        // 한쪽만 있으면 갱신도 재로그인도 못 하므로 세션으로 취급하면 안 된다.
        dataStore.updateData {
            mutablePreferencesOf(
                androidx.datastore.preferences.core.stringPreferencesKey("access_token") to "access-1"
            )
        }

        assertNull(store.load())
        // 캐시에도 남으면 인터셉터는 헤더를 붙이는데 갱신 경로는 세션이 없다고 보는 모순이 생긴다.
        assertNull(store.blockingAccessToken())
    }

    @Test
    fun `clear 하면 토큰이 사라진다`() = runTest {
        store.save(token)

        store.clear()

        assertNull(store.load())
    }

    @Test
    fun `blockingAccessToken은 첫 호출에만 저장소를 읽고 이후에는 캐시를 쓴다`() = runTest {
        store.save(token)
        val readsAfterSave = dataStore.readCount

        assertEquals("access-1", store.blockingAccessToken())
        assertEquals("access-1", store.blockingAccessToken())

        // save 가 캐시를 채워두므로 추가 읽기가 없어야 한다.
        assertEquals(readsAfterSave, dataStore.readCount)
    }

    @Test
    fun `토큰이 없다는 사실도 캐시해서 매번 저장소를 읽지 않는다`() = runTest {
        assertNull(store.blockingAccessToken())
        val readsAfterFirst = dataStore.readCount

        assertNull(store.blockingAccessToken())

        assertEquals(readsAfterFirst, dataStore.readCount)
    }

    @Test
    fun `clear 하면 캐시된 액세스 토큰도 비워진다`() = runTest {
        store.save(token)
        assertEquals("access-1", store.blockingAccessToken())

        store.clear()

        assertNull(store.blockingAccessToken())
    }
}
