package com.example.moamap.feature.collection.presentation.createmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateMapViewModelTest {

    private val viewModel = CreateMapViewModel()

    private val state get() = viewModel.uiState.value

    @Test
    fun `이름과 공개 범위가 모두 채워져야 지도를 만들 수 있다`() {
        assertFalse(state.canSubmit)

        viewModel.updateName("성수 카페 투어")
        assertFalse(state.canSubmit)

        viewModel.selectVisibility(MapVisibility.Public)
        assertTrue(state.canSubmit)
    }

    @Test
    fun `공백뿐인 이름은 이름으로 치지 않는다`() {
        viewModel.updateName("   ")
        viewModel.selectVisibility(MapVisibility.Private)

        assertFalse(state.canSubmit)
    }

    @Test
    fun `이름은 서버 제한인 100자까지만 받는다`() {
        viewModel.updateName("가".repeat(150))

        assertEquals(100, state.name.length)
    }

    @Test
    fun `설명은 서버 제한인 500자까지만 받는다`() {
        viewModel.updateDescription("나".repeat(600))

        assertEquals(500, state.description.length)
    }

    @Test
    fun `스페이스를 입력하면 태그로 확정된다`() {
        viewModel.updateTagInput("맛집 ")

        assertEquals(listOf("맛집"), state.tags)
        assertEquals("", state.tagInput)
    }

    @Test
    fun `확정되지 않은 입력은 그대로 남는다`() {
        viewModel.updateTagInput("맛집 카페")

        assertEquals(listOf("맛집"), state.tags)
        assertEquals("카페", state.tagInput)
    }

    @Test
    fun `엔터로도 태그가 확정된다`() {
        viewModel.updateTagInput("카페")
        viewModel.commitTag()

        assertEquals(listOf("카페"), state.tags)
        assertEquals("", state.tagInput)
    }

    @Test
    fun `이미 담은 태그는 다시 담기지 않는다`() {
        viewModel.updateTagInput("카페 카페 ")

        assertEquals(listOf("카페"), state.tags)
    }

    @Test
    fun `빈 입력은 태그가 되지 않는다`() {
        viewModel.commitTag()
        viewModel.updateTagInput("   ")

        assertTrue(state.tags.isEmpty())
    }

    @Test
    fun `태그는 서버 제한인 30자까지만 받는다`() {
        viewModel.updateTagInput("다".repeat(40))

        assertEquals(30, state.tagInput.length)
    }

    @Test
    fun `태그를 지우면 목록에서 빠진다`() {
        viewModel.updateTagInput("카페 맛집 ")

        viewModel.removeTag("카페")

        assertEquals(listOf("맛집"), state.tags)
    }

    @Test
    fun `사진을 고르면 미리보기 URI 가 남는다`() {
        assertNull(state.imageUri)

        viewModel.selectImage("content://map/photo")

        assertEquals("content://map/photo", state.imageUri)
    }
}
