package com.moamap.app.core.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.moamap.app.core.auth.di.AuthPreferences
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 토큰과 같은 DataStore 를 쓴다. 세션에 딸린 값이라 로그아웃하면 함께 지워진다.
 *
 * 다만 그 동작에 기대지 않고 로그인할 때 직접 지운다([AgreedTermsVersionStore]). 앱을 켤 때 한 번만
 * 읽어서 [DataStoreCurrentUserStore] 처럼 캐시하지 않는다.
 */
@Singleton
class DataStoreAgreedTermsVersionStore @Inject constructor(
    @param:AuthPreferences private val dataStore: DataStore<Preferences>,
) : AgreedTermsVersionStore {

    override suspend fun load(): String? = dataStore.data
        // 파일이 깨졌을 때 앱이 죽는 대신 동의하지 않은 것으로 취급한다. 다시 동의를 받으면 된다.
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .first()[AGREED_VERSION]

    override suspend fun save(version: String) {
        dataStore.edit { preferences -> preferences[AGREED_VERSION] = version }
    }

    override suspend fun clear() {
        dataStore.edit { preferences -> preferences.remove(AGREED_VERSION) }
    }

    private companion object {
        val AGREED_VERSION = stringPreferencesKey("agreed_terms_version")
    }
}
