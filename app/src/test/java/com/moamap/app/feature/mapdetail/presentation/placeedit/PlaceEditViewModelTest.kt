package com.moamap.app.feature.mapdetail.presentation.placeedit

import com.moamap.app.core.common.PLACE_UPDATED_TOAST
import com.moamap.app.feature.mapdetail.presentation.FakeMapDetailRepository
import com.moamap.app.feature.mapdetail.presentation.testPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 장소 수정·삭제(이슈 #376). 시안 「장소 수정 관련」 메모의 저장 버튼·뒤로가기 규칙을 본다. */
@OptIn(ExperimentalCoroutinesApi::class)
class PlaceEditViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val place = testPlace(3L).copy(description = "창가 자리", tags = listOf("카페"))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun opened(repository: FakeMapDetailRepository = FakeMapDetailRepository()) =
        PlaceEditViewModel(repository).apply { open(place) }

    @Test
    fun `열면 지금 메모·태그로 시작하고 바뀐 게 없어 저장할 수 없다`() {
        val viewModel = opened()

        val form = viewModel.uiState.value.form!!
        assertEquals("창가 자리", form.memo)
        assertEquals(listOf("카페"), form.tags)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `고쳤다가 되돌리면 다시 저장할 수 없다`() {
        val viewModel = opened()

        viewModel.updateMemo("바뀐 메모")
        assertTrue(viewModel.uiState.value.canSave)

        viewModel.updateMemo("창가 자리")
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `확정하지 않은 태그 입력도 바뀐 것으로 본다`() {
        val viewModel = opened()

        viewModel.updateTagInput("데이트")

        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `바뀐 게 없으면 뒤로 가기에 바로 닫힌다`() {
        val viewModel = opened()

        viewModel.requestClose()

        assertNull(viewModel.uiState.value.form)
        assertFalse(viewModel.uiState.value.discardConfirmVisible)
    }

    @Test
    fun `고친 채로 뒤로 가면 그만둘지 묻고, 계속 수정하면 그대로 남는다`() {
        val viewModel = opened()
        viewModel.removeTag("카페")

        viewModel.requestClose()
        assertTrue(viewModel.uiState.value.discardConfirmVisible)
        assertNotNull(viewModel.uiState.value.form)

        viewModel.keepEditing()
        assertFalse(viewModel.uiState.value.discardConfirmVisible)
        assertEquals(emptyList<String>(), viewModel.uiState.value.form!!.tags)
    }

    @Test
    fun `그만두기를 고르면 저장하지 않고 닫는다`() = runTest {
        val repository = FakeMapDetailRepository()
        val viewModel = opened(repository)
        viewModel.updateMemo("바뀐 메모")
        viewModel.requestClose()

        viewModel.close()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.form)
        assertFalse(viewModel.uiState.value.discardConfirmVisible)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `저장하면 입력 중인 태그까지 확정해 보내고 닫은 뒤 토스트를 띄운다`() = runTest {
        val repository = FakeMapDetailRepository()
        val viewModel = opened(repository)
        viewModel.updateMemo("  바뀐 메모 ")
        viewModel.updateTagInput("데이트")

        viewModel.save()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("바뀐 메모" to listOf("카페", "데이트"), repository.lastUpdate)
        val state = viewModel.uiState.value
        assertNull(state.form)
        assertEquals(PLACE_UPDATED_TOAST, state.toast)
        assertEquals(listOf("카페", "데이트"), state.savedPlace!!.tags)
    }

    @Test
    fun `메모와 태그를 모두 비우면 빈 값으로 보낸다`() = runTest {
        val repository = FakeMapDetailRepository()
        val viewModel = opened(repository)
        viewModel.updateMemo("")
        viewModel.removeTag("카페")

        viewModel.save()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("" to emptyList<String>(), repository.lastUpdate)
    }

    @Test
    fun `저장에 실패하면 입력을 그대로 두고 안내한다`() = runTest {
        val repository = FakeMapDetailRepository().apply { placeChangeFailure = IllegalStateException() }
        val viewModel = opened(repository)
        viewModel.updateMemo("바뀐 메모")

        viewModel.save()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("바뀐 메모", state.form!!.memo)
        assertEquals("장소 정보를 수정하지 못했어요", state.errorMessage)
        assertFalse(state.saving)
        assertNull(state.savedPlace)
    }

    @Test
    fun `저장 중에는 두 번 보내지 않고 닫히지도 않는다`() = runTest {
        val repository = FakeMapDetailRepository(responseDelayMillis = 100L)
        val viewModel = opened(repository)
        viewModel.updateMemo("바뀐 메모")

        viewModel.save()
        viewModel.save()
        viewModel.requestClose()
        dispatcher.scheduler.runCurrent()

        assertNotNull(viewModel.uiState.value.form)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf("updatePlace"), repository.calls)
    }

    @Test
    fun `삭제하면 지운 장소를 알린다`() = runTest {
        val repository = FakeMapDetailRepository()
        val viewModel = PlaceEditViewModel(repository)

        viewModel.delete(3L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(3L, viewModel.uiState.value.deletedPlaceId)
        assertEquals(listOf("deletePlace"), repository.calls)
    }

    @Test
    fun `삭제에 실패하면 안내만 하고 지운 것으로 보지 않는다`() = runTest {
        val repository = FakeMapDetailRepository().apply { placeChangeFailure = IllegalStateException() }
        val viewModel = PlaceEditViewModel(repository)

        viewModel.delete(3L)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.deletedPlaceId)
        assertEquals("장소를 삭제하지 못했어요", state.deleteErrorMessage)
        assertFalse(state.deleting)
    }
}
