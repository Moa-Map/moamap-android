package com.example.moamap.feature.collection.presentation.createmap

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateMapViewModelTest {

    private val savedStateHandle = SavedStateHandle()

    private val viewModel = CreateMapViewModel(savedStateHandle)

    private val state get() = viewModel.uiState.value

    /** 프로세스가 죽었다 살아나는 상황. 저장된 값만 들고 ViewModel 을 새로 만든다. */
    private fun recreateViewModel() = CreateMapViewModel(savedStateHandle)

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

    @Test
    fun `프로세스가 죽었다 살아나도 입력한 값이 남는다`() {
        viewModel.selectImage("content://map/photo")
        viewModel.updateName("성수 카페 투어")
        viewModel.updateDescription("주말에 다녀온 곳")
        viewModel.selectVisibility(MapVisibility.Private)
        viewModel.updateTagInput("카페 성수 ")
        viewModel.updateTagInput("데이")

        val restored = recreateViewModel().uiState.value

        assertEquals("content://map/photo", restored.imageUri)
        assertEquals("성수 카페 투어", restored.name)
        assertEquals("주말에 다녀온 곳", restored.description)
        assertEquals(MapVisibility.Private, restored.visibility)
        assertEquals(listOf("카페", "성수"), restored.tags)
        assertEquals("데이", restored.tagInput)
    }

    @Test
    fun `아무것도 입력하지 않았으면 빈 상태로 시작한다`() {
        val restored = recreateViewModel().uiState.value

        assertEquals(CreateMapUiState(), restored)
    }

    @Test
    fun `엔터로 줄바꿈이 들어와도 태그로 확정된다`() {
        viewModel.updateTagInput("카페\n")

        assertEquals(listOf("카페"), state.tags)
        assertEquals("", state.tagInput)
    }

    @Test
    fun `이미 담은 태그는 나중에 다시 입력해도 늘지 않는다`() {
        viewModel.updateTagInput("카페 ")
        viewModel.updateTagInput("카페 ")

        assertEquals(listOf("카페"), state.tags)
    }
}
