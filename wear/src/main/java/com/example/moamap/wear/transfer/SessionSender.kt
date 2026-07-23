package com.example.moamap.wear.transfer

import android.content.Context
import com.example.moamap.core.walksession.WalkSessionJson
import com.example.moamap.core.walksession.WalkSessionPayload
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** 워치와 폰이 합의한 채널 경로. 폰 쪽 수신 서비스의 intent-filter 와 반드시 같아야 한다. */
const val WALK_SESSION_CHANNEL_PATH = "/walk-session"

/**
 * 세션을 gzip JSON 으로 압축해 연결된 폰에 보낸다.
 *
 * 샘플이 수천 개가 되므로 MessageClient(최대 100KB) 대신 ChannelClient 스트림을 쓴다.
 */
@Singleton
class SessionSender @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    suspend fun send(payload: WalkSessionPayload): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val nodeClient = Wearable.getNodeClient(context)
            val nodes = nodeClient.connectedNodes.await()
            val node = nodes.firstOrNull { it.isNearby }
                ?: nodes.firstOrNull()
                ?: error("연결된 폰이 없습니다")

            val channelClient = Wearable.getChannelClient(context)
            val channel = channelClient.openChannel(node.id, WALK_SESSION_CHANNEL_PATH).await()

            try {
                channelClient.getOutputStream(channel).await().use { output ->
                    output.write(WalkSessionJson.encodeToGzip(payload))
                    output.flush()
                }
            } finally {
                // 채널을 닫다가 던지더라도 이미 성공한 전송을 실패로 뒤집으면 안 된다.
                runCatching { channelClient.close(channel) }
            }
        }
    }
}
