package com.moamap.app.core.network

import com.moamap.app.core.network.model.ApiResponse
import com.moamap.app.core.network.model.UNKNOWN_ERROR_CODE
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

/**
 * 서버가 모든 성공 응답을 {"success", "data", "error"} 로 감싸므로
 * 여기서 한 번만 벗겨 각 Service 가 페이로드 타입만 선언하게 한다.
 *
 * Retrofit 에 kotlinx 컨버터보다 **먼저** 등록해야 한다.
 * 요청 본문 변환은 다루지 않으므로(상위 구현이 null 반환) kotlinx 컨버터가 처리한다.
 */
class EnvelopeConverterFactory(
    private val json: Json,
) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<ResponseBody, *> {
        val isUnit = type == Unit::class.java
        // Unit 은 페이로드가 없으므로 자리만 채운다. 아래에서 data 를 읽지 않는다.
        val payloadSerializer = json.serializersModule.serializer(
            if (isUnit) String::class.java else type
        )
        val envelopeSerializer = ApiResponse.serializer(payloadSerializer)

        return Converter { body ->
            val envelope = json.decodeFromString(envelopeSerializer, body.string())
            when {
                !envelope.success -> throw ApiException(
                    code = envelope.error?.code ?: UNKNOWN_ERROR_CODE,
                    status = envelope.error?.status?.takeIf { it != 0 } ?: HTTP_OK,
                    serverMessage = envelope.error?.message.orEmpty(),
                )

                isUnit -> Unit

                envelope.data == null -> throw ApiException(
                    code = EMPTY_DATA_CODE,
                    status = HTTP_OK,
                    serverMessage = "성공 응답에 data 가 없습니다.",
                )

                else -> envelope.data
            }
        }
    }

    private companion object {
        const val EMPTY_DATA_CODE = "EMPTY_DATA"
        const val HTTP_OK = 200
    }
}
