package com.moamap.app.core.common

import java.text.BreakIterator

/*
 * 입력값을 「사람이 보는 글자」(grapheme) 단위로 다룬다.
 *
 * 글자 경계는 java.text.BreakIterator 로 찾는다. 안드로이드(API 24+)에서는 이 클래스가
 * android.icu.text.BreakIterator 를 그대로 감싸므로 ICU 와 같은 경계가 나온다. android.icu 를 직접
 * 쓰지 않은 것은 JVM 유닛 테스트 때문이다 - android.jar 스텁에서는 null 이 돌아와 실제 경계를 검증할 수 없다.
 * JVM(JDK 20+)의 구현도 국기·피부색·ZWJ 결합 이모지를 한 글자로 본다.
 *
 * iOS 도 같은 규칙으로 자른다. 기준 예시는 GraphemeTextTest 에 있다.
 */

/**
 * UTF-16 길이 합이 [maxLength] 를 넘지 않는 데까지, 글자 경계에서만 잘라 남긴다.
 *
 * 서버의 길이 제한(`@Size`)은 UTF-16 단위로 센다. `take(n)` 로 자르면 이모지가 반쪽만 남고, 그 반쪽은
 * 보낼 때 `?` 로 바뀌어 서버에 `?` 로 저장된다(이슈 #418). 결합 이모지(🇰🇷·👍🏽·👨‍👩‍👧)는 통째로 빠진다.
 */
internal fun String.takeWithinUtf16(maxLength: Int): String {
    require(maxLength >= 0) { "maxLength 는 0 이상이어야 한다: $maxLength" }
    if (length <= maxLength) return this

    val boundaries = BreakIterator.getCharacterInstance().apply { setText(this@takeWithinUtf16) }
    // maxLength 이하인 마지막 경계. length > maxLength 이므로 maxLength + 1 은 범위 안이다.
    val end = boundaries.preceding(maxLength + 1)
    return substring(0, end.coerceAtLeast(0))
}

/**
 * 이모지로 그려지는 글자를 통째로 뺀다. 외부 지도 URL·문의 답변 이메일처럼 이모지가 들어갈 수 없는 칸에 쓴다.
 *
 * 국기·피부색·ZWJ 결합·키캡(1️⃣)은 한 글자로 묶여 함께 빠진다. 단독 숫자·`#`·`*` 는 이모지 속성이
 * 있어도 글자로 그려지므로 남긴다.
 */
internal fun String.removeEmoji(): String {
    if (isEmpty()) return this

    val boundaries = BreakIterator.getCharacterInstance().apply { setText(this@removeEmoji) }
    val result = StringBuilder(length)
    var start = boundaries.first()
    var end = boundaries.next()
    while (end != BreakIterator.DONE) {
        val grapheme = substring(start, end)
        if (!grapheme.isEmojiGrapheme()) result.append(grapheme)
        start = end
        end = boundaries.next()
    }
    return result.toString()
}

private const val VARIATION_SELECTOR_EMOJI = 0xFE0F
private const val COMBINING_ENCLOSING_KEYCAP = 0x20E3

/** 이모지 블록(마작·카드·국기 글자·피부색·그림 문자 등). 이 범위의 기본 표시는 이모지다. */
private val SupplementaryEmojiRange = 0x1F000..0x1FAFF

/**
 * BMP 안에서 기본 표시가 이모지인 글자(Unicode emoji-data 의 Emoji_Presentation).
 * ☺·❤ 처럼 기본이 글자인 것은 FE0F 가 붙을 때만 이모지로 그려지므로 여기에 넣지 않는다.
 */
private val BmpEmojiPresentationRanges = listOf(
    0x231A..0x231B, 0x23E9..0x23EC, 0x23F0..0x23F0, 0x23F3..0x23F3,
    0x25FD..0x25FE, 0x2614..0x2615, 0x2648..0x2653, 0x267F..0x267F,
    0x2693..0x2693, 0x26A1..0x26A1, 0x26AA..0x26AB, 0x26BD..0x26BE,
    0x26C4..0x26C5, 0x26CE..0x26CE, 0x26D4..0x26D4, 0x26EA..0x26EA,
    0x26F2..0x26F3, 0x26F5..0x26F5, 0x26FA..0x26FA, 0x26FD..0x26FD,
    0x2705..0x2705, 0x270A..0x270B, 0x2728..0x2728, 0x274C..0x274C,
    0x274E..0x274E, 0x2753..0x2755, 0x2757..0x2757, 0x2795..0x2797,
    0x27B0..0x27B0, 0x27BF..0x27BF, 0x2B1B..0x2B1C, 0x2B50..0x2B50,
    0x2B55..0x2B55,
)

private fun String.isEmojiGrapheme(): Boolean {
    var index = 0
    while (index < length) {
        val codePoint = codePointAt(index)
        if (codePoint.isEmojiCodePoint()) return true
        index += Character.charCount(codePoint)
    }
    return false
}

private fun Int.isEmojiCodePoint(): Boolean =
    this == VARIATION_SELECTOR_EMOJI ||
        this == COMBINING_ENCLOSING_KEYCAP ||
        this in SupplementaryEmojiRange ||
        BmpEmojiPresentationRanges.any { range -> this in range }
