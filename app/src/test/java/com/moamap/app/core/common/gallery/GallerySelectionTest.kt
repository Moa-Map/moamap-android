package com.moamap.app.core.common.gallery

import org.junit.Assert.assertEquals
import org.junit.Test

/** `Uri` 는 JVM 테스트에서 만들 수 없어 문자열로 같은 규칙을 검증한다. */
class GallerySelectionTest {

    @Test
    fun `고른 순서를 유지하며 쌓는다`() {
        val selected = toggleSelection(toggleSelection(emptyList(), "a", max = 5), "b", max = 5)

        assertEquals(listOf("a", "b"), selected)
    }

    @Test
    fun `이미 고른 것을 누르면 뺀다`() {
        assertEquals(listOf("a", "c"), toggleSelection(listOf("a", "b", "c"), "b", max = 5))
    }

    @Test
    fun `상한을 채우면 새로 고르는 건 무시한다`() {
        val selected = listOf("a", "b")

        assertEquals(selected, toggleSelection(selected, "c", max = 2))
    }

    @Test
    fun `상한을 채웠어도 빼는 건 된다`() {
        assertEquals(listOf("b"), toggleSelection(listOf("a", "b"), "a", max = 2))
    }
}
