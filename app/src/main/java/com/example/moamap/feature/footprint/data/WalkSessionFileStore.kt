package com.example.moamap.feature.footprint.data

import com.example.moamap.core.walksession.WalkSessionJson
import com.example.moamap.core.walksession.WalkSessionPayload
import com.example.moamap.core.walksession.computeStats
import com.example.moamap.feature.footprint.domain.model.ReceivedWalkSession
import java.io.File

/**
 * 수신한 세션을 앱 내부 저장소에 파일로 쌓아둔다.
 *
 * 생성자에 `@Inject` 를 붙이지 않는다 — [rootDir] 는 Hilt 가 알 수 없는 값이라
 * `FootprintModule` 의 `@Provides` 로만 만든다.
 *
 * 백엔드 업로드가 붙기 전까지는 이 파일들이 유일한 원본이고,
 * 디버그 화면의 JSON 공유도 여기서 꺼낸다.
 */
class WalkSessionFileStore(
    private val rootDir: File,
) {

    fun save(payload: WalkSessionPayload, receivedAtEpochMillis: Long): File {
        rootDir.mkdirs()
        // clientSessionId 를 파일명에 넣어야 같은 밀리초에 도착한 두 세션이 서로 덮어쓰지 않는다.
        // 워치 쪽 UUID 이미 안전하지만, 이 값을 만든 쪽을 신뢰하지 않고 방어적으로 걸러낸다.
        val safeClientSessionId = payload.clientSessionId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(rootDir, "walk-session-$receivedAtEpochMillis-$safeClientSessionId.json")
        file.writeText(WalkSessionJson.encodeToString(payload))
        return file
    }

    /** 최근에 받은 것부터 돌려준다. 깨진 파일은 조용히 건너뛴다. */
    fun loadAll(): List<ReceivedWalkSession> {
        val files = rootDir.listFiles()?.filter { it.name.startsWith("walk-session-") } ?: return emptyList()

        return files.mapNotNull { file ->
            val payload = runCatching { WalkSessionJson.decodeFromString(file.readText()) }.getOrNull()
                ?: return@mapNotNull null

            ReceivedWalkSession(
                payload = payload,
                stats = payload.computeStats(),
                receivedAtEpochMillis = receivedAtFromName(file.name),
                fileName = file.name,
            )
        }.sortedByDescending { it.receivedAtEpochMillis }
    }

    fun fileFor(fileName: String): File = File(rootDir, fileName)

    private fun receivedAtFromName(fileName: String): Long {
        val withoutPrefixAndSuffix = fileName.removePrefix("walk-session-").removeSuffix(".json")
        val timestampSegment = withoutPrefixAndSuffix.substringBefore("-")
        return timestampSegment.toLongOrNull() ?: 0L
    }
}
