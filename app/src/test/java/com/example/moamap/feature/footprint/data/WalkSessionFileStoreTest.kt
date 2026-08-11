package com.example.moamap.feature.footprint.data

import com.example.moamap.core.walksession.WalkSample
import com.example.moamap.core.walksession.WalkSessionPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class WalkSessionFileStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun payload(id: String, startedAt: Long) = WalkSessionPayload(
        clientSessionId = id,
        startedAtEpochMillis = startedAt,
        endedAtEpochMillis = startedAt + 600_000,
        samples = listOf(
            WalkSample(tsEpochMillis = startedAt + 1_000, lat = 37.5, lng = 127.0),
            WalkSample(tsEpochMillis = startedAt + 2_000, hr = 80.0),
        ),
    )

    @Test
    fun `저장한 세션을 다시 읽을 수 있다`() {
        val store = WalkSessionFileStore(tempFolder.root)

        store.save(payload("s1", 1_700_000_000_000), receivedAtEpochMillis = 1_700_000_700_000)

        val loaded = store.loadAll()
        assertEquals(1, loaded.size)
        assertEquals("s1", loaded.single().payload.clientSessionId)
    }

    @Test
    fun `읽어온 세션에는 통계가 함께 계산되어 있다`() {
        val store = WalkSessionFileStore(tempFolder.root)
        store.save(payload("s1", 1_700_000_000_000), receivedAtEpochMillis = 1_700_000_700_000)

        val stats = store.loadAll().single().stats

        assertEquals(2, stats.sampleCount)
        assertEquals(1, stats.locationSampleCount)
        assertEquals(1, stats.heartRateSampleCount)
    }

    @Test
    fun `최근에 받은 세션이 먼저 온다`() {
        val store = WalkSessionFileStore(tempFolder.root)
        store.save(payload("older", 1_700_000_000_000), receivedAtEpochMillis = 1_000)
        store.save(payload("newer", 1_700_000_000_000), receivedAtEpochMillis = 2_000)

        assertEquals("newer", store.loadAll().first().payload.clientSessionId)
    }

    @Test
    fun `저장 디렉터리가 없으면 만들어서 저장한다`() {
        val store = WalkSessionFileStore(tempFolder.root.resolve("not-created-yet"))

        val file = store.save(payload("s1", 1), receivedAtEpochMillis = 1)

        assertTrue(file.exists())
    }

    @Test
    fun `읽을 수 없는 파일은 건너뛴다`() {
        val store = WalkSessionFileStore(tempFolder.root)
        store.save(payload("s1", 1_700_000_000_000), receivedAtEpochMillis = 1_000)
        tempFolder.root.resolve("walk-session-broken.json").writeText("이건 JSON이 아니다")

        assertEquals(1, store.loadAll().size)
    }

    @Test
    fun `같은 밀리초에 도착한 서로 다른 세션은 둘 다 살아남는다`() {
        val store = WalkSessionFileStore(tempFolder.root)
        store.save(payload("session-a", 1_700_000_000_000), receivedAtEpochMillis = 5_000)
        store.save(payload("session-b", 1_700_000_000_000), receivedAtEpochMillis = 5_000)

        val loaded = store.loadAll()

        assertEquals(2, loaded.size)
        val ids = loaded.map { it.payload.clientSessionId }.toSet()
        assertEquals(setOf("session-a", "session-b"), ids)
    }

    @Test
    fun `파일명이 바뀌어도 receivedAtEpochMillis 는 그대로 복원된다`() {
        val store = WalkSessionFileStore(tempFolder.root)
        val saved = store.save(payload("s1", 1_700_000_000_000), receivedAtEpochMillis = 1_700_000_700_000)

        // walk-session-<millis>-... 접두 규약은 유지한 채 나머지 부분만 바꿔, 파일명이 그대로
        // receivedAtEpochMillis 를 복원한다는 사실을 실제로 검증한다(저장된 시점 값이 아니라).
        val renamed = File(saved.parentFile, "walk-session-1700000700000-renamed-by-test.json")
        assertTrue(saved.renameTo(renamed))

        val loaded = store.loadAll().single()

        assertEquals(1_700_000_700_000, loaded.receivedAtEpochMillis)
    }

    @Test
    fun `같은 clientSessionId 를 다시 저장하면 파일이 하나만 남고 처음 받은 시각이 유지된다`() {
        val store = WalkSessionFileStore(tempFolder.root)

        store.save(payload("dup-session", 1_700_000_000_000), receivedAtEpochMillis = 1_000)
        store.save(payload("dup-session", 1_700_000_000_000), receivedAtEpochMillis = 2_000)

        val loaded = store.loadAll()

        assertEquals(1, loaded.size)
        assertEquals("dup-session", loaded.single().payload.clientSessionId)
        assertEquals(1_000, loaded.single().receivedAtEpochMillis)
    }

    @Test
    fun `한 세션 id 가 다른 id 의 접미사여도 서로 다른 세션으로 저장된다`() {
        // "a-x" 가 먼저 저장되면 파일명이 walk-session-1000-a-x.json 이 된다.
        // 중복 검사를 파일명 끝 일치로 하면 전혀 다른 "x" 세션이 이 파일에 걸려
        // 저장되지 않고 사라진다. 중복을 막으려다 데이터를 잃는 경로다.
        val store = WalkSessionFileStore(tempFolder.root)

        store.save(payload("a-x", 1_700_000_000_000), receivedAtEpochMillis = 1_000)
        store.save(payload("x", 1_700_000_100_000), receivedAtEpochMillis = 2_000)

        val ids = store.loadAll().map { it.payload.clientSessionId }.toSet()

        assertEquals(setOf("a-x", "x"), ids)
    }

    @Test
    fun `정규화 결과가 같은 서로 다른 세션 id 도 각각 저장된다`() {
        // 파일명에는 안전한 문자만 남기므로 "a/b" 와 "a?b" 가 똑같이 "a_b" 로 정규화된다.
        // 정규화된 값으로 같은 세션인지 판정하면 두 번째 세션이 첫 번째로 오인되어 사라진다.
        val store = WalkSessionFileStore(tempFolder.root)

        store.save(payload("a/b", 1_700_000_000_000), receivedAtEpochMillis = 1_000)
        store.save(payload("a?b", 1_700_000_100_000), receivedAtEpochMillis = 2_000)

        val ids = store.loadAll().map { it.payload.clientSessionId }.toSet()

        assertEquals(setOf("a/b", "a?b"), ids)
    }

    @Test
    fun `저장 도중 남은 tmp 파일은 세션으로 읽지 않는다`() {
        // 저장 중간에 프로세스가 죽으면 반쪽짜리 `.tmp` 파일이 남을 수 있다.
        // loadAll 이 이걸 깨진 세션으로 집계하면 안 된다.
        val store = WalkSessionFileStore(tempFolder.root)
        store.save(payload("real-session", 1_700_000_000_000), receivedAtEpochMillis = 1_000)
        tempFolder.root.resolve("walk-session-2000-half.json.tmp").writeText("{\"clientSess")

        val loaded = store.loadAll()

        assertEquals(1, loaded.size)
        assertEquals("real-session", loaded.single().payload.clientSessionId)
    }

    // ---------- 삭제 ----------

    @Test
    fun `지운 세션은 목록에서 빠진다`() {
        val store = WalkSessionFileStore(tempFolder.root)
        store.save(payload("s1", 1_700_000_000_000), receivedAtEpochMillis = 1_700_000_700_000)
        store.save(payload("s2", 1_700_000_100_000), receivedAtEpochMillis = 1_700_000_800_000)

        val target = store.loadAll().first { it.payload.clientSessionId == "s1" }
        assertTrue(store.delete(target.fileName))

        assertEquals(listOf("s2"), store.loadAll().map { it.payload.clientSessionId })
    }

    @Test
    fun `없는 파일을 지워도 지운 것으로 본다`() {
        // 같은 카드를 두 번 눌러 삭제가 두 번 나가도 두 번째가 실패로 보이면 안 된다.
        val store = WalkSessionFileStore(tempFolder.root)

        assertTrue(store.delete("walk-session-1700000700000-s1.json"))
    }

    @Test
    fun `저장소 바깥을 가리키는 이름은 지우지 않는다`() {
        // 지우는 일은 되돌릴 수 없다. 호출부가 이름을 지켰겠거니 하고 넘기지 않는다.
        val store = WalkSessionFileStore(tempFolder.root)
        val outside = File(tempFolder.root.parentFile, "walk-session-1-outside.json")
        outside.writeText("{}")

        assertFalse(store.delete("../${outside.name}"))
        assertTrue(outside.exists())

        outside.delete()
    }

    @Test
    fun `이름 규칙을 통과해도 저장소를 벗어나면 지우지 않는다`() {
        // 파일명 패턴의 `.+` 는 슬래시도 먹는다. `walk-session-1-../evil.json` 은 규칙을
        // 통과하면서 상위 폴더를 가리킨다. 패턴 검사만으로는 못 막는 자리다.
        val store = WalkSessionFileStore(tempFolder.root)
        val outside = File(tempFolder.root.parentFile, "evil.json").apply { writeText("{}") }

        assertFalse(store.delete("walk-session-1-../${outside.name}"))
        assertTrue(outside.exists())

        outside.delete()
    }

    @Test
    fun `세션 파일이 아닌 이름은 지우지 않는다`() {
        val store = WalkSessionFileStore(tempFolder.root)
        val other = File(tempFolder.root, "secrets.json").apply { writeText("{}") }

        assertFalse(store.delete("secrets.json"))
        assertTrue(other.exists())
    }
}
