package com.moamap.app.core.common.upload

/** 줄인 사진은 언제나 이 형식으로 올린다. presign 서명에 들어가므로 발급 요청과 PUT 이 같은 값을 쓴다. */
internal const val COMPRESSED_IMAGE_CONTENT_TYPE = "image/jpeg"

/**
 * 올리기 전에 줄이는 한 단계. 긴 변을 [maxLongEdge] 이하로 맞추고 JPEG 품질 [quality] 로 저장한다.
 *
 * @param quality `Bitmap.compress` 가 받는 0~100 값.
 */
internal data class CompressionStep(
    val maxLongEdge: Int,
    val quality: Int,
)

/**
 * 줄이는 순서. 첫 단계는 모든 사진에 한 번 적용하고, 결과가 한도를 넘을 때만 다음 단계로 간다.
 *
 * 화질을 먼저 낮추고 해상도는 마지막에 낮춘다. 지도에서 사진을 크게 보므로 해상도가 더 아깝다.
 * iOS 와 같은 순서다. 한쪽만 바꾸면 같은 사진이 플랫폼마다 다르게 올라간다.
 */
internal val COMPRESSION_STEPS = listOf(
    CompressionStep(maxLongEdge = 2048, quality = 80),
    CompressionStep(maxLongEdge = 2048, quality = 70),
    CompressionStep(maxLongEdge = 2048, quality = 60),
    CompressionStep(maxLongEdge = 2048, quality = 50),
    CompressionStep(maxLongEdge = 1600, quality = 50),
    CompressionStep(maxLongEdge = 1280, quality = 50),
)

/**
 * [current] 로 줄인 결과가 [encodedSize] 일 때 다음에 할 일.
 *
 * - 한도 안이면 `null` - 이 결과를 쓴다.
 * - 넘으면 다음 단계.
 * - 마지막 단계로도 넘으면 [ImageUploadException.TooLarge].
 *
 * 크기가 0 이하인 결과는 여기서 거르지 않는다. 한도 안으로 보고 넘기면 마지막 검사(`validateImageUpload`)가 막는다.
 */
internal fun nextCompressionStep(
    current: CompressionStep,
    encodedSize: Long,
    maxFileSize: Long,
): CompressionStep? {
    if (encodedSize <= maxFileSize) return null

    val index = COMPRESSION_STEPS.indexOf(current)
    require(index >= 0) { "정해진 단계가 아닙니다: $current" }
    return COMPRESSION_STEPS.getOrNull(index + 1) ?: throw ImageUploadException.TooLarge(maxFileSize)
}

/** 긴 변이 [maxLongEdge] 이하가 되게 비율을 지켜 줄인 크기. 이미 작으면 그대로다 - 키우지 않는다. */
internal fun scaledSize(width: Int, height: Int, maxLongEdge: Int): Pair<Int, Int> {
    val longEdge = maxOf(width, height)
    if (longEdge <= maxLongEdge) return width to height

    val scale = maxLongEdge.toDouble() / longEdge
    return (width * scale).toInt().coerceAtLeast(1) to (height * scale).toInt().coerceAtLeast(1)
}

/**
 * `BitmapFactory` 로 처음부터 줄여 읽을 때의 `inSampleSize`.
 *
 * 읽은 결과의 긴 변이 [maxLongEdge] 보다 작아지지 않는 가장 큰 2의 거듭제곱이다. 더 줄이면 정해진 해상도보다
 * 흐려지고, 덜 줄이면 원본을 통째로 올리게 된다. 남는 만큼은 읽은 뒤에 맞춘다.
 */
internal fun decodeSampleSize(width: Int, height: Int, maxLongEdge: Int): Int {
    val longEdge = maxOf(width, height)
    var sampleSize = 1
    while (longEdge / (sampleSize * 2) >= maxLongEdge) {
        sampleSize *= 2
    }
    return sampleSize
}
