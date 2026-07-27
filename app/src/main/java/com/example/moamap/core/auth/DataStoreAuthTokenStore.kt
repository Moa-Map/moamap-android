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
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore Preferences 로 토큰을 보관한다.
 *
 * 암호화하지 않는다 - 앱 전용 저장소라 루팅되지 않은 기기에서는 다른 앱이 읽을 수 없고,
 * `androidx.security-crypto` 는 deprecated 라 지금 도입하면 곧 다시 걷어내야 한다.
 * 배포 전에 다시 판단한다.
 */
@Singleton
class DataStoreAuthTokenStore @Inject constructor(
    @param:AuthPreferences private val dataStore: DataStore<Preferences>,
) : AuthTokenStore {

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

    override suspend fun load(): AuthToken? {
        val preferences = dataStore.data
            // 파일이 깨졌을 때 앱이 죽는 대신 세션이 없는 것으로 취급한다.
            .catch { throwable ->
                if (throwable is IOException) emit(emptyPreferences()) else throw throwable
            }
            .first()

        val accessToken = preferences[ACCESS_TOKEN].orEmpty()
        val refreshToken = preferences[REFRESH_TOKEN].orEmpty()

        cachedAccessToken = accessToken.ifEmpty { null }
        hydrated = true

        // 한쪽만 남아 있으면 갱신도 재로그인도 못 하므로 세션이 없는 것으로 본다.
        if (accessToken.isEmpty() || refreshToken.isEmpty()) return null
        return AuthToken(accessToken = accessToken, refreshToken = refreshToken)
    }

    override suspend fun save(token: AuthToken) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = token.accessToken
            preferences[REFRESH_TOKEN] = token.refreshToken
        }
        cachedAccessToken = token.accessToken
        hydrated = true
    }

    override suspend fun clear() {
        dataStore.edit { preferences -> preferences.clear() }
        cachedAccessToken = null
        hydrated = true
    }

    private companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }
}
