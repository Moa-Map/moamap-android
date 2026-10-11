package com.moamap.app.core.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

/**
 * 저장된 값을 한 번 읽는다. 파일이 깨졌으면(IOException) 앱이 죽는 대신 빈 값으로 읽는다.
 *
 * 빈 값이 무슨 뜻인지(세션 없음·동의 안 함·모르는 사용자)는 부르는 쪽이 정한다.
 */
internal suspend fun DataStore<Preferences>.readOrEmpty(): Preferences = data
    .catch { throwable ->
        if (throwable is IOException) emit(emptyPreferences()) else throw throwable
    }
    .first()
