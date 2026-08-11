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

    /**
     * 같은 세션이 두 번 도착해도 파일은 하나만 남긴다.
     *
     * 채널이 동시에 두 개 열리면 두 호출이 나란히 중복 검사를 통과해 각자 파일을 쓸 수 있으므로,
     * 조회와 생성을 한 덩어리로 묶는다. 저장은 세션당 한 번뿐이라 직렬화 비용은 무시할 수 있다.
     */
    @Synchronized
    fun save(payload: WalkSessionPayload, receivedAtEpochMillis: Long): File {
        rootDir.mkdirs()
        // clientSessionId 를 파일명에 넣어야 같은 밀리초에 도착한 두 세션이 서로 덮어쓰지 않는다.
        // 워치 쪽 UUID 는 이미 안전하지만, 이 값을 만든 쪽을 신뢰하지 않고 방어적으로 걸러낸다.
        val safeClientSessionId = payload.clientSessionId.replace(Regex("[^A-Za-z0-9_-]"), "_")

        // 모호한 실패 뒤 워치가 같은 세션을 재전송하면 두 번째 저장은 여기서 막혀야 한다 -
        // 그렇지 않으면 같은 산책이 다른 타임스탬프로 두 번 저장되어 목록에 중복으로 보인다.
        // 이미 저장된 파일이 있으면 새로 쓰지 않고 원래 파일(원래 receivedAtEpochMillis)을 그대로 돌려준다.
        existingFileFor(payload.clientSessionId, safeClientSessionId)?.let { return it }

        val file = File(rootDir, "walk-session-$receivedAtEpochMillis-$safeClientSessionId.json")
        // 곧바로 최종 파일에 쓰다가 중간에 죽으면 반쪽짜리 JSON 이 남고, loadAll() 이 그걸
        // 조용히 건너뛰어 세션이 영영 사라진다. 임시 파일에 다 쓴 뒤 원자적으로 rename 해서
        // 최종 파일은 항상 완전한 상태이거나 아예 없거나 둘 중 하나가 되게 한다.
        val temp = File(rootDir, "${file.name}.tmp")
        try {
            temp.writeText(WalkSessionJson.encodeToString(payload))
            if (!temp.renameTo(file)) {
                temp.delete()
                error("세션 파일 교체 실패: ${file.name}")
            }
        } catch (e: Exception) {
            temp.delete()
            throw e
        }
        return file
    }

    /**
     * 같은 세션이 이미 저장돼 있으면 그 파일을 돌려준다.
     *
     * 파일명은 정규화를 거치므로 신원 판정에 쓸 수 없다 - `a/b` 와 `a?b` 가 똑같이 `a_b` 가 되어
     * 서로 다른 세션이 하나로 뭉개진다. 파일명은 후보를 좁히는 용도로만 쓰고,
     * 최종 판정은 저장된 payload 안의 **원본** clientSessionId 로 한다.
     * 정규화 결과가 겹치는 후보가 여럿이면 원본이 일치하는 것만 같은 세션이다.
     */
    private fun existingFileFor(clientSessionId: String, safeClientSessionId: String): File? {
        val pattern = Regex("""^walk-session-\d+-${Regex.escape(safeClientSessionId)}\.json$""")
        return rootDir.listFiles()
            ?.filter { pattern.matches(it.name) }  // `.tmp` 는 여기서도 걸러진다
            ?.firstOrNull { file ->
                val stored = runCatching { WalkSessionJson.decodeFromString(file.readText()) }.getOrNull()
                stored?.clientSessionId == clientSessionId
            }
    }

    /** 최근에 받은 것부터 돌려준다. 깨진 파일은 조용히 건너뛴다. */
    fun loadAll(): List<ReceivedWalkSession> {
        // 완성된 세션 파일만 읽는다. 저장 도중 남은 `.tmp` 파일은 이 패턴에 걸리지 않으므로,
        // 반쪽짜리 임시 파일이 깨진 세션으로 잘못 집계되지 않는다.
        val files = rootDir.listFiles()?.filter { COMPLETED_FILE.matches(it.name) } ?: return emptyList()

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

    /**
     * 세션 파일 하나를 지운다. 이미 없으면 지운 것으로 본다.
     *
     * 되돌릴 수 없다. 백엔드 업로드가 붙기 전까지 이 파일이 그 기록의 유일한 원본이다.
     *
     * 이름을 그대로 믿지 않는다. `..` 이 섞인 이름이 오면 저장소 바깥 파일을 지우게 된다 -
     * 지금은 [loadAll] 이 준 이름만 들어오지만, 지우는 일은 되돌릴 수 없어 호출부가
     * 지켰겠거니 하고 넘기지 않는다.
     */
    @Synchronized
    fun delete(fileName: String): Boolean {
        if (!COMPLETED_FILE.matches(fileName)) return false

        val file = File(rootDir, fileName)
        if (file.parentFile != rootDir) return false

        return !file.exists() || file.delete()
    }

    private fun receivedAtFromName(fileName: String): Long {
        val withoutPrefixAndSuffix = fileName.removePrefix("walk-session-").removeSuffix(".json")
        val timestampSegment = withoutPrefixAndSuffix.substringBefore("-")
        return timestampSegment.toLongOrNull() ?: 0L
    }

    private companion object {
        /** 완성된 세션 파일명. 저장 중간의 `.tmp` 파일은 이 패턴에 걸리지 않는다. */
        val COMPLETED_FILE = Regex("""^walk-session-\d+-.+\.json$""")
    }
}
