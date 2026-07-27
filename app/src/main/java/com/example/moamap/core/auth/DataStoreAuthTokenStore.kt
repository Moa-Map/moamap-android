package com.example.moamap.core.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.moamap.core.auth.di.AuthPreferences
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore Preferences 로 토큰을 보관한다.
 *
 * 저장 파일은 백업·기기 이전 대상에서 제외한다(`backup_rules.xml`, `data_extraction_rules.xml`).
 * 제외하지 않으면 인증 토큰이 클라우드 백업으로 빠져나간다.
 *
 * 파일 자체의 암호화는 아직 적용하지 않았다. 루팅되지 않은 기기에서는 앱 전용 저장소라
 * 다른 앱이 읽을 수 없다. Keystore 기반 암호화(`androidx.datastore:datastore-tink`)는 별도 이슈로 다룬다.
 */
@Singleton
class DataStoreAuthTokenStore @Inject constructor(
    @param:AuthPreferences private val dataStore: DataStore<Preferences>,
) : AuthTokenStore {

    /**
     * 디스크 편집과 캐시 전이를 하나의 임계 구역으로 묶는다.
     *
     * `@Volatile` 은 가시성만 보장할 뿐 상호 배제를 하지 않는다. 갱신([save])과 로그아웃([clear])이
     * 겹치면 clear 가 캐시를 비운 뒤 지연된 save 가 옛 토큰을 되살려, 로그아웃 후에도 그 토큰이
     * 요청에 실리게 된다.
     */
    private val mutex = Mutex()

    @Volatile
    private var cachedAccessToken: String? = null

    /** 디스크를 한 번이라도 읽었는지. 토큰이 없다는 사실도 캐시해야 매번 디스크를 때리지 않는다. */
    @Volatile
    private var hydrated = false

    override fun blockingAccessToken(): String? {
        if (!hydrated) {
            runBlocking { load() }
        }
        return cachedAccessToken
    }

    override suspend fun load(): AuthToken? = mutex.withLock {
        val preferences = dataStore.data
            // 파일이 깨졌을 때 앱이 죽는 대신 세션이 없는 것으로 취급한다.
            .catch { throwable ->
                if (throwable is IOException) emit(emptyPreferences()) else throw throwable
            }
            .first()

        val accessToken = preferences[ACCESS_TOKEN].orEmpty()
        val refreshToken = preferences[REFRESH_TOKEN].orEmpty()

        // 한쪽만 남아 있으면 갱신도 재로그인도 못 하므로 세션이 없는 것으로 본다.
        // 이때 액세스 토큰을 캐시에 넣으면 인터셉터는 헤더를 붙이는데 갱신 경로는 세션이 없다고
        // 판단하는 모순이 생기므로, 온전한 세션일 때만 캐시를 채운다.
        val token = if (accessToken.isEmpty() || refreshToken.isEmpty()) {
            null
        } else {
            AuthToken(accessToken = accessToken, refreshToken = refreshToken)
        }

        cachedAccessToken = token?.accessToken
        hydrated = true

        token
    }

    override suspend fun save(token: AuthToken) = mutex.withLock {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = token.accessToken
            preferences[REFRESH_TOKEN] = token.refreshToken
        }
        cachedAccessToken = token.accessToken
        hydrated = true
    }

    override suspend fun clear() = mutex.withLock {
        dataStore.edit { preferences -> preferences.clear() }
        cachedAccessToken = null
        hydrated = true
    }

    private companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }
}
