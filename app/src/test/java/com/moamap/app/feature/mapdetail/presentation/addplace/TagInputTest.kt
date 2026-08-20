package com.moamap.app.feature.mapdetail.presentation.addplace

import org.junit.Assert.assertEquals
import org.junit.Test

class TagInputTest {

    @Test
    fun `구분자가 없으면 입력만 이어진다`() {
        val result = applyTagInput(tags = emptyList(), rawInput = "성수")

        assertEquals(emptyList<String>(), result.tags)
        assertEquals("성수", result.input)
    }

    @Test
    fun `스페이스로 태그를 확정한다`() {
        val result = applyTagInput(tags = emptyList(), rawInput = "성수 ")

        assertEquals(listOf("성수"), result.tags)
        assertEquals("", result.input)
    }

    @Test
    fun `엔터로도 확정한다`() {
        val result = applyTagInput(tags = emptyList(), rawInput = "카페\n")

        assertEquals(listOf("카페"), result.tags)
        assertEquals("", result.input)
    }

    @Test
    fun `구분자 뒤에 남은 글자는 계속 입력 중이다`() {
        val result = applyTagInput(tags = emptyList(), rawInput = "성수 카")

        assertEquals(listOf("성수"), result.tags)
        assertEquals("카", result.input)
    }

    @Test
    fun `붙여넣기로 여러 개가 한 번에 들어와도 나눠 담는다`() {
        val result = applyTagInput(tags = emptyList(), rawInput = "성수 카페 데이트 ")

        assertEquals(listOf("성수", "카페", "데이트"), result.tags)
        assertEquals("", result.input)
    }

    @Test
    fun `공백뿐이면 태그로 만들지 않는다`() {
        val result = applyTagInput(tags = emptyList(), rawInput = "   ")

        assertEquals(emptyList<String>(), result.tags)
    }

    @Test
    fun `이미 있는 태그는 다시 넣지 않는다`() {
        val result = applyTagInput(tags = listOf("성수"), rawInput = "성수 ")

        assertEquals(listOf("성수"), result.tags)
    }

    @Test
    fun `서버 제한을 넘는 태그는 자른다`() {
        val long = "가".repeat(MAX_TAG_LENGTH + 10)

        val result = applyTagInput(tags = emptyList(), rawInput = "$long ")

        assertEquals(MAX_TAG_LENGTH, result.tags.single().length)
    }

    @Test
    fun `기존 태그 뒤에 이어 붙인다`() {
        val result = applyTagInput(tags = listOf("성수"), rawInput = "카페 ")

        assertEquals(listOf("성수", "카페"), result.tags)
    }

    @Test
    fun `마지막 태그를 지운다`() {
        assertEquals(listOf("성수"), removeLastTag(listOf("성수", "카페")))
    }

    @Test
    fun `지울 태그가 없으면 그대로 둔다`() {
        assertEquals(emptyList<String>(), removeLastTag(emptyList()))
    }
}
