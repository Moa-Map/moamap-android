package com.example.moamap.feature.footprint.data

import com.example.moamap.core.walksession.WalkSample
import com.example.moamap.core.walksession.WalkSessionPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

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
        store.save(payload("s1", 1_700_000_000_000), receivedAtEpochMillis = 1_700_000_700_000)

        val loaded = store.loadAll().single()

        assertEquals(1_700_000_700_000, loaded.receivedAtEpochMillis)
    }
}
