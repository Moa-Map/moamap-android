package com.example.moamap.feature.mypage.presentation

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

    var updatedNickname: String? = null
        private set
    var updatedIntroduction: String? = null
        private set

    override suspend fun getMyProfile(): MyProfile {
        loadFailure?.let { throw it }
        return profile
    }

    override suspend fun updateMyProfile(nickname: String, introduction: String): MyProfile {
        updatedNickname = nickname
        updatedIntroduction = introduction
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
}
