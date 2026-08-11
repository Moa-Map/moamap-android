package com.example.moamap.feature.mypage.presentation

import com.example.moamap.core.common.upload.ImageUploadException
import com.example.moamap.feature.mypage.domain.model.MyProfile
import com.example.moamap.feature.mypage.domain.repository.UserRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

private class FakeUserRepository : UserRepository {

    var profile = MyProfile(
        id = 1,
        nickname = "모아",
        email = "moa@example.com",
        profileImageUrl = null,
        introduction = "안녕하세요",
    )

    var loadFailure: Throwable? = null
    var updateFailure: Throwable? = null
    var uploadFailure: Throwable? = null

    var updatedNickname: String? = null
        private set
    var updatedIntroduction: String? = null
        private set
    var updatedProfileImageUrl: String? = null
        private set

    /** 올린 URI 를 순서대로 담는다. 크기가 곧 업로드 횟수다. */
    val uploadedUris = mutableListOf<String>()

    /** 업로드가 성공했을 때 돌려줄 주소. 호출 순서대로 뒤에 번호가 붙는다. */
    var issuedFileUrl = "https://cdn.example.com/profile.jpg"

    override suspend fun getMyProfile(): MyProfile {
        loadFailure?.let { throw it }
        return profile
    }

    override suspend fun uploadProfileImage(imageUri: String): String {
        uploadFailure?.let { throw it }
        uploadedUris += imageUri
        return "$issuedFileUrl?v=${uploadedUris.size}"
    }

    override suspend fun updateMyProfile(
        nickname: String,
        introduction: String,
        profileImageUrl: String?,
    ): MyProfile {
        updatedNickname = nickname
        updatedIntroduction = introduction
        updatedProfileImageUrl = profileImageUrl
        updateFailure?.let { throw it }
        return profile.copy(nickname = nickname, introduction = introduction)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileEditViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeUserRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ProfileEditViewModel(repository)

    @Test
    fun `조회에 성공하면 편집 필드가 응답 값으로 채워진다`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("모아", state.nickname)
        assertEquals("안녕하세요", state.introduction)
        assertEquals(
            ProfileLoadState.Success(email = "moa@example.com", profileImageUrl = null),
            state.load,
        )
    }

    @Test
    fun `조회에 실패하면 오류 상태가 된다`() = runTest(dispatcher) {
        repository.loadFailure = IOException("네트워크 끊김")

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.load is ProfileLoadState.Error)
    }

    @Test
    fun `이름이 비면 저장할 수 없다`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNicknameChange("   ")

        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `이름이 서른 자를 넘으면 저장할 수 없다`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNicknameChange("가".repeat(NICKNAME_MAX_LENGTH + 1))

        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `저장에 성공하면 화면을 닫으라고 알린다`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNicknameChange("새이름")
        viewModel.onIntroductionChange("새소개")
        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.saved)
        assertFalse(state.saving)
        assertEquals("새이름", repository.updatedNickname)
        assertEquals("새소개", repository.updatedIntroduction)
    }

    @Test
    fun `이름 앞뒤 공백은 잘라서 보낸다`() = runTest(dispatcher) {
        // 서버 규칙이 `\S.*` 라 앞에 공백이 있으면 400 이 온다. 사용자에게 지우라고 하는 대신 잘라 보낸다.
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNicknameChange("  모아맵  ")
        viewModel.save()
        advanceUntilIdle()

        assertEquals("모아맵", repository.updatedNickname)
    }

    @Test
    fun `저장에 실패하면 화면에 머물고 메시지를 낸다`() = runTest(dispatcher) {
        repository.updateFailure = IOException("전송 실패")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.saved)
        assertFalse(state.saving)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun `에러를 소비하면 메시지가 사라진다`() = runTest(dispatcher) {
        repository.updateFailure = IOException("전송 실패")

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.save()
        advanceUntilIdle()

        viewModel.consumeError()

        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `조회에 실패한 상태에서는 저장하지 않는다`() = runTest(dispatcher) {
        // 서버 값을 못 받았는데 저장하면 빈 이름으로 덮어쓰게 된다.
        repository.loadFailure = IOException("네트워크 끊김")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNicknameChange("모아맵")
        viewModel.save()
        advanceUntilIdle()

        assertNull(repository.updatedNickname)
    }

    @Test
    fun `사진을 고르지 않으면 업로드하지 않는다`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        assertTrue(repository.uploadedUris.isEmpty())
        assertNull(repository.updatedProfileImageUrl)
        // updatedProfileImageUrl 의 초깃값도 null 이라 이 값만으로는 PATCH 가 아예 안 나간 것과
        // 사진 없이 나간 것을 구별할 수 없다. 실제로 PATCH 가 나갔다는 것을 이 값으로 고정한다.
        assertEquals("모아", repository.updatedNickname)
    }

    @Test
    fun `사진을 고르면 올린 주소를 저장 요청에 담는다`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onImageSelected("content://media/1")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(listOf("content://media/1"), repository.uploadedUris)
        assertEquals("https://cdn.example.com/profile.jpg?v=1", repository.updatedProfileImageUrl)
        assertTrue(viewModel.uiState.value.saved)
    }

    /** 올린 파일을 지우는 API 가 없다. 재시도마다 새로 올리면 고아 파일이 쌓인다. */
    @Test
    fun `저장에 실패해 다시 눌러도 같은 사진을 두 번 올리지 않는다`() = runTest(dispatcher) {
        repository.updateFailure = IOException("전송 실패")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onImageSelected("content://media/1")
        viewModel.save()
        advanceUntilIdle()

        repository.updateFailure = null
        viewModel.save()
        advanceUntilIdle()

        assertEquals(listOf("content://media/1"), repository.uploadedUris)
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `사진을 바꿔 고르면 다시 올린다`() = runTest(dispatcher) {
        repository.updateFailure = IOException("전송 실패")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onImageSelected("content://media/1")
        viewModel.save()
        advanceUntilIdle()

        repository.updateFailure = null
        viewModel.onImageSelected("content://media/2")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(listOf("content://media/1", "content://media/2"), repository.uploadedUris)
        assertEquals("https://cdn.example.com/profile.jpg?v=2", repository.updatedProfileImageUrl)
    }

    /** 무엇이 문제인지는 예외가 이미 문구로 들고 있다. 사진을 바꾸라고 알려줘야 한다. */
    @Test
    fun `사진이 너무 크면 그 이유를 알리고 저장 요청을 보내지 않는다`() = runTest(dispatcher) {
        repository.uploadFailure = ImageUploadException.TooLarge()

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onImageSelected("content://media/1")
        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("사진 크기는 10MB 이하여야 해요", state.errorMessage)
        assertFalse(state.saved)
        assertFalse(state.saving)
        assertNull(repository.updatedNickname)
    }

    @Test
    fun `업로드가 네트워크로 실패하면 일반 저장 실패로 알린다`() = runTest(dispatcher) {
        repository.uploadFailure = IOException("네트워크 끊김")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onImageSelected("content://media/1")
        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("저장하지 못했어요. 잠시 후 다시 시도해주세요.", state.errorMessage)
        assertFalse(state.saved)
    }

    /**
     * 저장은 이미 골라둔 사진의 스냅샷을 들고 시작한다. 그 사이에 사진을 바꿔치기 허용하면,
     * 서버에는 먼저 고른 사진이 저장되는데 화면은 나중에 고른 사진을 보여주며 저장됐다고 알리게 된다.
     */
    @Test
    fun `저장하는 동안에는 사진을 바꿔도 무시한다`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onImageSelected("content://media/1")
        viewModel.save()
        viewModel.onImageSelected("content://media/2")
        advanceUntilIdle()

        assertEquals("content://media/1", viewModel.uiState.value.pickedImageUri)
        assertEquals(listOf("content://media/1"), repository.uploadedUris)
        assertEquals("https://cdn.example.com/profile.jpg?v=1", repository.updatedProfileImageUrl)
    }
}
