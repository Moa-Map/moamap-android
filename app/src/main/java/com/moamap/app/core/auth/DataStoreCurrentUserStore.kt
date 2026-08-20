package com.moamap.app.core.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.moamap.app.core.auth.di.AuthPreferences
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 토큰과 같은 DataStore 를 쓴다. 세션에 딸린 값이라 함께 지워지는 편이 자연스럽다.
 *
 * 다만 [DataStoreAuthTokenStore.clear] 가 저장소 전체를 비우는 데 기대지 않는다. 그 구현이
 * 나중에 토큰 키만 지우도록 좁혀지면 식별자만 살아남아, 다른 계정으로 로그인했을 때 남의 글이
 * 내 것으로 보인다. 지우는 쪽에서 이 저장소도 함께 지운다.
 */
@Singleton
class DataStoreCurrentUserStore @Inject constructor(
    @param:AuthPreferences private val dataStore: DataStore<Preferences>,
) : CurrentUserStore {

    /** 디스크 편집과 캐시 전이를 하나의 임계 구역으로 묶는다. */
    private val mutex = Mutex()

    @Volatile
    private var cached: Long? = null

    /** 디스크를 한 번이라도 읽었는지. 없다는 사실도 캐시해야 매번 디스크를 때리지 않는다. */
    @Volatile
    private var hydrated = false

    override suspend fun load(): Long? = mutex.withLock {
        if (hydrated) return@withLock cached

        val preferences = dataStore.data
            // 파일이 깨졌을 때 앱이 죽는 대신 모르는 것으로 취급한다.
            .catch { throwable ->
                if (throwable is IOException) emit(emptyPreferences()) else throw throwable
            }
            .first()

        cached = preferences[USER_ID]?.takeIf { id -> id > 0 }
        hydrated = true

        cached
    }

    override suspend fun save(userId: Long) = mutex.withLock {
        if (userId <= 0) return@withLock

        dataStore.edit { preferences -> preferences[USER_ID] = userId }
        cached = userId
        hydrated = true
    }

    override suspend fun clear() = mutex.withLock {
        dataStore.edit { preferences -> preferences.remove(USER_ID) }
        cached = null
        hydrated = true
    }

    private companion object {
        val USER_ID = longPreferencesKey("current_user_id")
    }
}
