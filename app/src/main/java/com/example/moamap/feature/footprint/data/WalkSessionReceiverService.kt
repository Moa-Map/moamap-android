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
import java.io.ByteArrayOutputStream
import java.io.InputStream
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
                val bytes = channelClient.getInputStream(channel).await()
                    .use { readBytesUpTo(it, WalkSessionJson.MAX_COMPRESSED_BYTES) }
                val payload = WalkSessionJson.decodeFromGzip(bytes)
                fileStore.save(payload, receivedAtEpochMillis = System.currentTimeMillis())
                Log.i(TAG, "세션 수신 완료: ${payload.clientSessionId}, 샘플 ${payload.samples.size}개")
            }.onFailure { throwable ->
                Log.e(TAG, "세션 수신 실패", throwable)
            }
            runCatching { channelClient.close(channel).await() }
        }
    }

    /**
     * [InputStream.readBytes] 는 전체를 다 읽을 때까지 무제한으로 버퍼를 키운다.
     * 채널 상대(워치)가 비정상적으로 큰 데이터를 보내면 그 자체로 힙을 고갈시킬 수 있으므로,
     * 조금씩 읽으면서 누적 크기가 한도를 넘는 즉시 중단한다(다 읽은 뒤 크기를 검사하지 않는다).
     */
    private fun readBytesUpTo(input: InputStream, limitBytes: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var totalRead = 0
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            totalRead += read
            check(totalRead <= limitBytes) {
                "세션 채널 입력이 한도를 초과했습니다 (한도 $limitBytes bytes)"
            }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    companion object {
        private const val TAG = "WalkSessionReceiver"
        const val WALK_SESSION_PATH = "/walk-session"
    }
}
