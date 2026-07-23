package com.example.moamap.core.walksession

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class WalkSessionJsonTest {

    private val payload = WalkSessionPayload(
        clientSessionId = "session-1",
        startedAtEpochMillis = 1_700_000_000_000,
        endedAtEpochMillis = 1_700_000_600_000,
        samples = listOf(
            WalkSample(tsEpochMillis = 1_700_000_001_000, lat = 37.5665, lng = 126.9780, accuracyMeters = 12.5),
            WalkSample(tsEpochMillis = 1_700_000_002_000, hr = 78.0),
        ),
        watchMarks = listOf(1_700_000_300_000),
    )

    @Test
    fun `gzip으로 인코딩한 세션은 원본과 동일하게 복원된다`() {
        val restored = WalkSessionJson.decodeFromGzip(WalkSessionJson.encodeToGzip(payload))

        assertEquals(payload, restored)
    }

    @Test
    fun `위치 샘플은 심박이 없고 심박 샘플은 위치가 없다`() {
        val restored = WalkSessionJson.decodeFromGzip(WalkSessionJson.encodeToGzip(payload))

        assertNull(restored.samples[0].hr)
        assertNull(restored.samples[1].lat)
        assertEquals(78.0, restored.samples[1].hr!!, 0.001)
    }

    @Test
    fun `gzip 인코딩 결과는 원본 JSON보다 작다`() {
        val raw = WalkSessionJson.encodeToPrettyString(payload).toByteArray()

        assertTrue(WalkSessionJson.encodeToGzip(payload).size < raw.size)
    }

    @Test
    fun `모르는 필드가 있어도 파싱에 실패하지 않는다`() {
        val json = """
            {"clientSessionId":"s","startedAtEpochMillis":1,"endedAtEpochMillis":2,
             "samples":[],"watchMarks":[],"unknownField":"ignored"}
        """.trimIndent()

        val restored = WalkSessionJson.decodeFromString(json)

        assertEquals("s", restored.clientSessionId)
    }

    @Test
    fun `한도 안의 정상 페이로드는 그대로 왕복한다`() {
        // MAX_INFLATED_BYTES/MAX_COMPRESSED_BYTES 한도가 실제 세션에는 전혀 걸리지 않음을 확인한다.
        val restored = WalkSessionJson.decodeFromGzip(WalkSessionJson.encodeToGzip(payload))

        assertEquals(payload, restored)
    }

    @Test
    fun `압축 해제 결과가 한도를 넘으면 거부된다`() {
        // 압축 폭탄 재현: 반복 바이트라 압축은 잘 되지만(수 KB), 풀면 한도(32MB)를 넘는다.
        // 이 테스트에서 수백 MB를 실제로 할당하지 않도록 한도보다 살짝 큰 크기만 사용한다.
        val oversizedRepeatingBytes = ByteArray(WalkSessionJson.MAX_INFLATED_BYTES + 1024)
        val gzippedBytes = ByteArrayOutputStream().also { out ->
            GZIPOutputStream(out).use { it.write(oversizedRepeatingBytes) }
        }.toByteArray()

        // 압축은 매우 작으므로 압축 입력 한도에는 걸리지 않는다 — 순수하게 인플레이트 한도만 검증한다.
        assertTrue(gzippedBytes.size < WalkSessionJson.MAX_COMPRESSED_BYTES)

        assertThrows(WalkSessionPayloadTooLargeException::class.java) {
            WalkSessionJson.decodeFromGzip(gzippedBytes)
        }
    }

    @Test
    fun `압축 입력이 한도를 넘으면 풀기 전에 거부된다`() {
        // 인플레이트 한도와 별개로, 들어온 압축 바이트 자체가 한도를 넘으면
        // GZIP 파싱을 시도하기도 전에 막아야 한다. gzip 형식이 아닌 바이트를 쓰는 이유는
        // 크기 검사가 파싱보다 먼저 일어난다는 것 자체를 드러내기 위해서다.
        val oversizedInput = ByteArray(WalkSessionJson.MAX_COMPRESSED_BYTES + 1)

        assertThrows(WalkSessionPayloadTooLargeException::class.java) {
            WalkSessionJson.decodeFromGzip(oversizedInput)
        }
    }
}
