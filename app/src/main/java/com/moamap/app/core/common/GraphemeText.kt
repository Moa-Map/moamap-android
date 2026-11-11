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
