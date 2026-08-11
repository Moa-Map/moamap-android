package com.example.moamap.feature.collection.presentation.createmap

import androidx.lifecycle.SavedStateHandle
import com.example.moamap.core.common.upload.ImageUploadException
import com.example.moamap.core.network.ConnectionException
import com.example.moamap.feature.collection.domain.model.CreatedMap
import com.example.moamap.feature.collection.domain.model.MapType
import com.example.moamap.feature.collection.domain.model.MapVisibility
import com.example.moamap.feature.collection.domain.model.NewMap
import com.example.moamap.feature.collection.domain.repository.MapRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CreateMapViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val savedStateHandle = SavedStateHandle()

    private val repository = FakeMapRepository()

    private val viewModel = CreateMapViewModel(savedStateHandle, repository)

    private val state get() = viewModel.uiState.value

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 프로세스가 죽었다 살아나는 상황. 저장된 값만 들고 ViewModel 을 새로 만든다. */
    private fun recreateViewModel() = CreateMapViewModel(savedStateHandle, repository)

    /** 만들 수 있는 최소 입력을 채운다. */
    private fun fillRequiredInput() {
        viewModel.updateName("성수 카페 투어")
        viewModel.selectVisibility(MapVisibility.Public)
    }

    private class FakeMapRepository(
        var createResult: () -> CreatedMap = { CreatedMap(CREATED_MAP_ID, inviteCode = null) },
        var uploadResult: () -> String = { UPLOADED_COVER_URL },
    ) : MapRepository {

        val createdMaps = mutableListOf<NewMap>()

        /** 어떤 URI 로 몇 번 불렸는지. 재업로드 방지를 확인하는 데 쓴다. */
        val uploadedUris = mutableListOf<String>()

        override suspend fun uploadCoverImage(imageUri: String): String {
            uploadedUris += imageUri
            return uploadResult()
        }

        override suspend fun createMap(newMap: NewMap): CreatedMap {
            createdMaps += newMap
            return createResult()
        }

        override suspend fun getMyMaps(type: MapType) = TODO("사용하지 않음")
        override suspend fun joinByInviteCode(inviteCode: String) = TODO("사용하지 않음")
    }

    private companion object {
        const val CREATED_MAP_ID = 42L
        const val PICKED_IMAGE_URI = "content://media/picked.jpg"
        const val UPLOADED_COVER_URL = "https://cdn/cover.jpg"
    }

    // ---------- 입력 ----------

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

    // ---------- 제출 ----------

    @Test
    fun `입력한 값 그대로 지도를 만든다`() = runTest(dispatcher) {
        fillRequiredInput()
        viewModel.updateDescription("주말에 다녀온 곳")
        viewModel.updateTagInput("카페 ")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(
            NewMap(
                name = "성수 카페 투어",
                description = "주말에 다녀온 곳",
                visibility = MapVisibility.Public,
                tags = listOf("카페"),
            ),
            repository.createdMaps.single(),
        )
        assertEquals(SubmitState.Done(CREATED_MAP_ID), state.submit)
    }

    @Test
    fun `구분자 없이 입력만 해둔 태그도 함께 저장된다`() = runTest(dispatcher) {
        fillRequiredInput()
        viewModel.updateTagInput("카페 ")
        // 스페이스도 엔터도 누르지 않고 바로 만들기를 누르는 경우.
        viewModel.updateTagInput("성수")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(listOf("카페", "성수"), repository.createdMaps.single().tags)
        // 화면에 보이는 칩과 보낸 값이 같아야 한다.
        assertEquals(listOf("카페", "성수"), state.tags)
        assertEquals("", state.tagInput)
    }

    @Test
    fun `이미 담긴 태그를 다시 입력한 채 저장해도 중복되지 않는다`() = runTest(dispatcher) {
        fillRequiredInput()
        viewModel.updateTagInput("카페 ")
        viewModel.updateTagInput("카페")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(listOf("카페"), repository.createdMaps.single().tags)
    }

    @Test
    fun `사진을 골라도 나머지 입력은 그대로 실려 나간다`() = runTest(dispatcher) {
        fillRequiredInput()
        viewModel.selectImage(PICKED_IMAGE_URI)

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(MapVisibility.Public, repository.createdMaps.single().visibility)
        // 미리보기는 계속 그 사진을 가리킨다. 올린 주소로 바뀌지 않는다.
        assertEquals(PICKED_IMAGE_URI, state.imageUri)
        assertEquals(SubmitState.Done(CREATED_MAP_ID), state.submit)
    }

    @Test
    fun `제출하는 동안에는 다시 누를 수 없다`() = runTest(dispatcher) {
        fillRequiredInput()

        viewModel.submit()

        assertFalse(state.canSubmit)
        assertTrue(state.isSubmitting)

        // 코루틴이 시작되기 전에 한 번 더 눌러도 지도가 두 개 만들어지면 안 된다.
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(1, repository.createdMaps.size)
    }

    @Test
    fun `프라이빗 지도를 만들면 초대 코드를 보여준다`() = runTest(dispatcher) {
        repository.createResult = { CreatedMap(CREATED_MAP_ID, inviteCode = "A1B2C3") }
        viewModel.updateName("우리끼리 지도")
        viewModel.selectVisibility(MapVisibility.Private)

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(
            SubmitState.ShowingInviteCode(CREATED_MAP_ID, "A1B2C3"),
            state.submit,
        )
    }

    @Test
    fun `초대 코드를 닫으면 화면을 빠져나간다`() = runTest(dispatcher) {
        repository.createResult = { CreatedMap(CREATED_MAP_ID, inviteCode = "A1B2C3") }
        viewModel.updateName("우리끼리 지도")
        viewModel.selectVisibility(MapVisibility.Private)
        viewModel.submit()
        advanceUntilIdle()

        viewModel.dismissInviteCode()

        assertEquals(SubmitState.Done(CREATED_MAP_ID), state.submit)
    }

    @Test
    fun `초대 코드가 없으면 모달 없이 바로 끝난다`() = runTest(dispatcher) {
        repository.createResult = { CreatedMap(CREATED_MAP_ID, inviteCode = null) }
        fillRequiredInput()

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(SubmitState.Done(CREATED_MAP_ID), state.submit)
    }

    @Test
    fun `초대 코드는 프로세스가 죽었다 살아나도 남는다`() = runTest(dispatcher) {
        repository.createResult = { CreatedMap(CREATED_MAP_ID, inviteCode = "A1B2C3") }
        viewModel.updateName("우리끼리 지도")
        viewModel.selectVisibility(MapVisibility.Private)
        viewModel.submit()
        advanceUntilIdle()

        // 이 화면을 벗어나면 코드를 다시 볼 방법이 없다.
        val restored = recreateViewModel().uiState.value

        assertEquals(
            SubmitState.ShowingInviteCode(CREATED_MAP_ID, "A1B2C3"),
            restored.submit,
        )
    }

    @Test
    fun `지도 생성에 실패하면 입력값을 남긴 채 되돌아온다`() = runTest(dispatcher) {
        repository.createResult = { throw IOException("서버 오류") }
        fillRequiredInput()
        viewModel.updateTagInput("카페 ")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(SubmitState.Idle, state.submit)
        assertEquals("지도를 만들지 못했어요", state.errorMessage)
        // 다시 칠 필요 없이 그대로 재시도할 수 있어야 한다.
        assertEquals("성수 카페 투어", state.name)
        assertEquals(listOf("카페"), state.tags)
        assertTrue(state.canSubmit)
    }

    @Test
    fun `연결에 실패하면 네트워크 안내를 보여준다`() = runTest(dispatcher) {
        repository.createResult = { throw ConnectionException(IOException()) }
        fillRequiredInput()

        viewModel.submit()
        advanceUntilIdle()

        assertEquals("네트워크에 연결할 수 없어요", state.errorMessage)
    }

    @Test
    fun `진행 중이던 상태는 프로세스가 죽으면 되살리지 않는다`() = runTest(dispatcher) {
        fillRequiredInput()
        viewModel.submit()

        // 요청은 프로세스와 함께 사라졌다. "만드는 중" 으로 되살아나면 버튼이 영영 잠긴다.
        val restored = recreateViewModel().uiState.value

        assertEquals(SubmitState.Idle, restored.submit)
        assertTrue(restored.canSubmit)
    }

    @Test
    fun `안내를 한 번 보여준 뒤에는 지운다`() = runTest(dispatcher) {
        repository.createResult = { throw IOException("서버 오류") }
        fillRequiredInput()
        viewModel.submit()
        advanceUntilIdle()

        viewModel.consumeError()

        assertNull(state.errorMessage)
    }

    // ---------- 커버 이미지 ----------

    @Test
    fun `사진을 안 골랐으면 업로드하지 않는다`() = runTest(dispatcher) {
        fillRequiredInput()
        viewModel.submit()
        advanceUntilIdle()

        assertTrue(repository.uploadedUris.isEmpty())
        assertNull(repository.createdMaps.single().imageUrl)
    }

    @Test
    fun `고른 사진을 올리고 그 주소로 지도를 만든다`() = runTest(dispatcher) {
        fillRequiredInput()
        viewModel.selectImage(PICKED_IMAGE_URI)
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(listOf(PICKED_IMAGE_URI), repository.uploadedUris)
        assertEquals(UPLOADED_COVER_URL, repository.createdMaps.single().imageUrl)
    }

    /** 커버가 빠진 채로 만들어지면 사용자가 알 수 없고, 지도 수정 화면도 없다. */
    @Test
    fun `업로드가 실패하면 지도를 만들지 않는다`() = runTest(dispatcher) {
        repository.uploadResult = { throw IOException("스토리지 오류") }
        fillRequiredInput()
        viewModel.selectImage(PICKED_IMAGE_URI)
        viewModel.submit()
        advanceUntilIdle()

        assertTrue(repository.createdMaps.isEmpty())
        assertEquals("사진을 올리지 못했어요", state.errorMessage)
    }

    @Test
    fun `업로드가 실패해도 입력값과 고른 사진은 그대로 둔다`() = runTest(dispatcher) {
        repository.uploadResult = { throw IOException("스토리지 오류") }
        fillRequiredInput()
        viewModel.selectImage(PICKED_IMAGE_URI)
        viewModel.updateDescription("주말에 다녀온 곳")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(SubmitState.Idle, state.submit)
        assertEquals("성수 카페 투어", state.name)
        assertEquals("주말에 다녀온 곳", state.description)
        assertEquals(PICKED_IMAGE_URI, state.imageUri)
        assertTrue(state.canSubmit)
    }

    /** 무엇이 문제인지 알려야 사용자가 사진을 바꿀 수 있다. */
    @Test
    fun `형식이나 크기 문제는 그 이유를 그대로 안내한다`() = runTest(dispatcher) {
        repository.uploadResult = { throw ImageUploadException.TooLarge() }
        fillRequiredInput()
        viewModel.selectImage(PICKED_IMAGE_URI)
        viewModel.submit()
        advanceUntilIdle()

        assertEquals("사진 크기는 10MB 이하여야 해요", state.errorMessage)
    }

    @Test
    fun `업로드 중 통신이 끊기면 네트워크 안내를 한다`() = runTest(dispatcher) {
        repository.uploadResult = { throw ConnectionException(IOException("끊김")) }
        fillRequiredInput()
        viewModel.selectImage(PICKED_IMAGE_URI)
        viewModel.submit()
        advanceUntilIdle()

        assertEquals("네트워크에 연결할 수 없어요", state.errorMessage)
    }

    /** 매번 새로 올리면 지울 수 없는 사진이 시도할 때마다 쌓인다. */
    @Test
    fun `생성만 실패해 다시 눌러도 사진은 한 번만 올린다`() = runTest(dispatcher) {
        repository.createResult = { throw IOException("서버 오류") }
        fillRequiredInput()
        viewModel.selectImage(PICKED_IMAGE_URI)
        viewModel.submit()
        advanceUntilIdle()

        repository.createResult = { CreatedMap(CREATED_MAP_ID, inviteCode = null) }
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(listOf(PICKED_IMAGE_URI), repository.uploadedUris)
        assertEquals(UPLOADED_COVER_URL, repository.createdMaps.last().imageUrl)
    }

    @Test
    fun `사진을 바꿔 고르면 다시 올린다`() = runTest(dispatcher) {
        repository.createResult = { throw IOException("서버 오류") }
        fillRequiredInput()
        viewModel.selectImage(PICKED_IMAGE_URI)
        viewModel.submit()
        advanceUntilIdle()

        val changed = "content://media/another.jpg"
        repository.createResult = { CreatedMap(CREATED_MAP_ID, inviteCode = null) }
        viewModel.selectImage(changed)
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(listOf(PICKED_IMAGE_URI, changed), repository.uploadedUris)
    }

    /**
     * 이미 시작한 업로드는 그대로 끝난다. 바꾸게 두면 미리보기에는 새 사진이 뜨는데 지도에는
     * 앞의 사진이 저장된다.
     */
    @Test
    fun `올리는 중에는 사진을 바꿀 수 없다`() = runTest(dispatcher) {
        fillRequiredInput()
        viewModel.selectImage(PICKED_IMAGE_URI)
        viewModel.submit()

        viewModel.selectImage("content://media/another.jpg")

        assertEquals(PICKED_IMAGE_URI, state.imageUri)

        advanceUntilIdle()
        assertEquals(listOf(PICKED_IMAGE_URI), repository.uploadedUris)
        assertEquals(UPLOADED_COVER_URL, repository.createdMaps.single().imageUrl)
    }

    /** 스토리지에 이미 올라간 사진이다. 잊어버리면 같은 사진이 하나 더 올라간다. */
    @Test
    fun `프로세스가 죽었다 살아나도 올려둔 사진을 다시 올리지 않는다`() = runTest(dispatcher) {
        repository.createResult = { throw IOException("서버 오류") }
        fillRequiredInput()
        viewModel.selectImage(PICKED_IMAGE_URI)
        viewModel.submit()
        advanceUntilIdle()

        repository.createResult = { CreatedMap(CREATED_MAP_ID, inviteCode = null) }
        val restored = recreateViewModel()
        restored.submit()
        advanceUntilIdle()

        assertEquals(listOf(PICKED_IMAGE_URI), repository.uploadedUris)
        assertEquals(UPLOADED_COVER_URL, repository.createdMaps.last().imageUrl)
    }
}
