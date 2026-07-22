package com.example.moamap.feature.footprint.data

import android.util.Log
import com.example.moamap.core.walksession.WalkSessionJson
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * 워치가 연 채널에서 세션 JSON 을 읽어 저장한다.
 *
 * 매니페스트의 intent-filter pathPrefix 와 워치의 WALK_SESSION_CHANNEL_PATH 가
 * 같아야 이 콜백이 불린다.
 */
@AndroidEntryPoint
class WalkSessionReceiverService : WearableListenerService() {

    @Inject
    lateinit var fileStore: WalkSessionFileStore

    // 서비스 생명주기와 일부러 묶지 않는다 — 시스템이 언제든 서비스를 파괴할 수 있는데,
    // 그 시점에 세션 수신이 진행 중이라면 취소되지 않고 끝까지 써야 유실이 없다.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        if (channel.path != WALK_SESSION_PATH) return

        scope.launch {
            val channelClient = Wearable.getChannelClient(applicationContext)
            runCatching {
                val bytes = channelClient.getInputStream(channel).await().use { it.readBytes() }
                val payload = WalkSessionJson.decodeFromGzip(bytes)
                fileStore.save(payload, receivedAtEpochMillis = System.currentTimeMillis())
                Log.i(TAG, "세션 수신 완료: ${payload.clientSessionId}, 샘플 ${payload.samples.size}개")
            }.onFailure { throwable ->
                Log.e(TAG, "세션 수신 실패", throwable)
            }
            runCatching { channelClient.close(channel).await() }
        }
    }

    companion object {
        private const val TAG = "WalkSessionReceiver"
        const val WALK_SESSION_PATH = "/walk-session"
    }
}
