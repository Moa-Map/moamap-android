package com.example.moamap.core.walksession

import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** 세션 페이로드의 직렬화 규약. 워치와 폰이 같은 설정을 쓰도록 한 곳에 모은다. */
object WalkSessionJson {

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

    fun decodeFromGzip(bytes: ByteArray): WalkSessionPayload =
        GZIPInputStream(bytes.inputStream()).use { stream ->
            decodeFromString(stream.readBytes().toString(Charsets.UTF_8))
        }
}
