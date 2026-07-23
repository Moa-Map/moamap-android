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

        // 모호한 실패 뒤 워치가 같은 세션을 재전송하면 두 번째 저장은 여기서 막혀야 한다 -
        // 그렇지 않으면 같은 산책이 다른 타임스탬프로 두 번 저장되어 목록/업로드에 중복으로 보인다.
        // 이미 저장된 파일이 있으면 새로 쓰지 않고 원래 파일(원래 receivedAtEpochMillis)을 그대로 돌려준다.
        existingFileFor(safeClientSessionId)?.let { return it }

        val file = File(rootDir, "walk-session-$receivedAtEpochMillis-$safeClientSessionId.json")
        file.writeText(WalkSessionJson.encodeToString(payload))
        return file
    }

    /**
     * 파일명 전체를 앵커링해서 비교한다.
     *
     * `endsWith("-$id.json")` 로 찾으면 id 가 다른 id 의 하이픈 뒤 접미사와 겹칠 때
     * (기존 `a-x` 세션이 있는데 새 `x` 세션이 오는 경우) 서로 다른 세션을 같은 것으로 보고
     * 새 세션을 저장하지 않고 버린다. 중복을 막으려다 데이터를 잃는 셈이라 정규식으로 고정한다.
     */
    private fun existingFileFor(safeClientSessionId: String): File? {
        val pattern = Regex("""^walk-session-\d+-${Regex.escape(safeClientSessionId)}\.json$""")
        return rootDir.listFiles()?.firstOrNull { pattern.matches(it.name) }
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
