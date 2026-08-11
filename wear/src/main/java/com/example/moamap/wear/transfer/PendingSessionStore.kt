package com.example.moamap.wear.transfer

import android.content.Context
import com.example.moamap.core.walksession.WalkSessionJson
import com.example.moamap.core.walksession.WalkSessionPayload
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 폰이 수신을 확인하기 전까지 세션을 워치에 붙들어 둔다.
 *
 * 몇 시간짜리 기록이 전송 한 번 실패로 사라지면 안 된다.
 * 전송에 성공했을 때만 [clear] 를 호출한다.
 */
@Singleton
class PendingSessionStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val file: File
        get() = File(context.filesDir, "pending-walk-session.json")

    suspend fun save(payload: WalkSessionPayload) = withContext(Dispatchers.IO) {
        file.writeText(WalkSessionJson.encodeToString(payload))
    }

    suspend fun load(): WalkSessionPayload? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        runCatching { WalkSessionJson.decodeFromString(file.readText()) }.getOrNull()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        file.delete()
        Unit
    }
}
