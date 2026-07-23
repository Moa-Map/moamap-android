package com.example.moamap.core.walksession

import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** gzip 입력/출력이 한도를 넘었을 때 던지는 예외. 압축 폭탄이나 손상된 페이로드를 구분해서 로깅할 수 있게 별도 타입으로 둔다. */
class WalkSessionPayloadTooLargeException(message: String) : RuntimeException(message)

/** 세션 페이로드의 직렬화 규약. 워치와 폰이 같은 설정을 쓰도록 한 곳에 모은다. */
object WalkSessionJson {

    // 측정 기준: 341초 세션(666 샘플) = 66KB 원본 = 6.4KB gzip. 8시간 ≈ 5.6MB 원본 ≈ 0.54MB gzip.
    // 45시간 분량까지 여유를 두고 압축 입력 한도를 4MB로 잡는다 — 정상 세션은 절대 이 근처에도 못 간다.
    const val MAX_COMPRESSED_BYTES: Int = 4 * 1024 * 1024

    // 위와 같은 기준으로 45시간(≈28,000 * 8 ≈ 224,000 샘플) 분량 원본 JSON도 32MB 안에 들어온다.
    // 이 한도를 압축 해제 도중에 강제해야 gzip 폭탄(작은 압축 입력이 거대하게 부풀어오르는 경우)을 막을 수 있다.
    const val MAX_INFLATED_BYTES: Int = 32 * 1024 * 1024

    private const val INFLATE_BUFFER_SIZE = 8192

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val prettyJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun encodeToString(payload: WalkSessionPayload): String =
        json.encodeToString(WalkSessionPayload.serializer(), payload)

    fun decodeFromString(text: String): WalkSessionPayload =
        json.decodeFromString(WalkSessionPayload.serializer(), text)

    /** 사람이 읽을 수 있는 형태. 디버그 화면의 JSON 공유에 쓴다. */
    fun encodeToPrettyString(payload: WalkSessionPayload): String =
        prettyJson.encodeToString(WalkSessionPayload.serializer(), payload)

    /** 전송용. 샘플이 수천 개가 되므로 압축해서 보낸다. */
    fun encodeToGzip(payload: WalkSessionPayload): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(encodeToString(payload).toByteArray(Charsets.UTF_8)) }
        return out.toByteArray()
    }

    fun decodeFromGzip(bytes: ByteArray): WalkSessionPayload {
        if (bytes.size > MAX_COMPRESSED_BYTES) {
            throw WalkSessionPayloadTooLargeException(
                "gzip 입력이 한도를 초과했습니다: ${bytes.size} bytes (한도 $MAX_COMPRESSED_BYTES bytes)"
            )
        }

        // 압축 해제 결과를 다 모은 뒤 크기를 검사하면 이미 메모리를 다 써버린 뒤다 -
        // 폭탄을 막는 의미가 없으므로, 조금씩 읽으면서 누적 크기가 한도를 넘는 즉시 중단한다.
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(INFLATE_BUFFER_SIZE)
        GZIPInputStream(bytes.inputStream()).use { stream ->
            var totalRead = 0
            while (true) {
                val read = stream.read(buffer)
                if (read == -1) break
                totalRead += read
                if (totalRead > MAX_INFLATED_BYTES) {
                    throw WalkSessionPayloadTooLargeException(
                        "gzip 압축 해제 결과가 한도를 초과했습니다 (한도 $MAX_INFLATED_BYTES bytes)"
                    )
                }
                output.write(buffer, 0, read)
            }
        }
        return decodeFromString(output.toByteArray().toString(Charsets.UTF_8))
    }
}
