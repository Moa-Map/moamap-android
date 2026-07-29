package com.example.moamap.feature.mapdetail.presentation.addplace

/** 태그를 확정하는 글자. 스페이스나 줄바꿈이 들어오면 그 앞까지를 태그로 만든다. */
private val TagDelimiters = charArrayOf(' ', '\n')

/** 태그 입력 한 번의 결과. */
data class TagInputResult(
    val tags: List<String>,
    val input: String,
)

/**
 * 태그 입력을 처리한다.
 *
 * 스페이스나 엔터가 들어오면 그 앞부분을 태그로 확정한다. 붙여넣기로 여러 개가 한 번에
 * 들어올 수 있어 구분자마다 끊는다.
 *
 * - 앞뒤 공백을 걷어내고, 남는 게 없으면 버린다
 * - 이미 있는 태그는 다시 넣지 않는다
 * - 서버가 태그 하나를 [MAX_TAG_LENGTH] 자로 제한하므로 넘으면 자른다
 */
fun applyTagInput(
    tags: List<String>,
    rawInput: String,
): TagInputResult {
    if (rawInput.none { char -> char in TagDelimiters }) {
        return TagInputResult(tags = tags, input = rawInput)
    }

    val pieces = rawInput.split(*TagDelimiters)
    // 마지막 조각은 구분자 뒤에 남은 부분이라 아직 입력 중이다. 확정하지 않는다.
    val confirmed = pieces.dropLast(1)
    val remaining = pieces.last()

    val next = tags.toMutableList()
    confirmed.forEach { piece ->
        val tag = piece.trim().take(MAX_TAG_LENGTH)
        if (tag.isNotEmpty() && tag !in next) next += tag
    }

    return TagInputResult(tags = next, input = remaining)
}

/**
 * 입력창이 빈 상태에서 백스페이스를 눌렀을 때 마지막 태그를 지운다.
 *
 * @return 지울 게 없으면 받은 목록 그대로
 */
fun removeLastTag(tags: List<String>): List<String> = tags.dropLast(1)
