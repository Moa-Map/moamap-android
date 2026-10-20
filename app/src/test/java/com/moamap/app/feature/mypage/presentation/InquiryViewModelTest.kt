package com.moamap.app.feature.mypage.presentation

import androidx.lifecycle.SavedStateHandle
import com.moamap.app.core.network.ApiException
import com.moamap.app.feature.mypage.data.remote.InquiryRequestDto
import com.moamap.app.feature.mypage.data.remote.InquiryService
import com.moamap.app.feature.mypage.data.remote.InquiryCreatedDto
import com.moamap.app.feature.mypage.data.repository.InquiryRepositoryImpl
import com.moamap.app.feature.mypage.domain.model.InquiryType
import com.moamap.app.feature.mypage.domain.model.MyProfile
import com.moamap.app.feature.mypage.domain.repository.UserRepository
import kotlinx.coroutines.CompletableDeferred
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
class InquiryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 계정 이메일을 [profileEmail] 이 끝날 때 돌려준다. 늦게 도착하는 경우를 흉내 낼 수 있다. */
    private class FakeUserRepository(
        val profileEmail: CompletableDeferred<String> = CompletableDeferred("me@moamap.com"),
    ) : UserRepository {
        override suspend fun getMyProfile() = MyProfile(
            id = 1L,
            nickname = "모아",
            email = profileEmail.await(),
            profileImageUrl = null,
            introduction = "",
        )

        override suspend fun uploadProfileImage(imageUri: String): String = error("쓰지 않는다")

        override suspend fun updateMyProfile(nickname: String, introduction: String, profileImageUrl: String?) =
            error("쓰지 않는다")
    }

    /** 실제 저장소를 거쳐 서버로 나가는 요청을 기록한다. 앞뒤 공백 떼기까지 함께 본다. */
    private class FakeInquiryService(var result: () -> InquiryCreatedDto = { InquiryCreatedDto(inquiryId = 1L) }) :
        InquiryService {
        val requests = mutableListOf<InquiryRequestDto>()

        override suspend fun createInquiry(request: InquiryRequestDto): InquiryCreatedDto {
            requests += request
            return result()
        }
    }

    private val service = FakeInquiryService()

    private fun viewModel(
        userRepository: UserRepository = FakeUserRepository(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) = InquiryViewModel(savedStateHandle, InquiryRepositoryImpl(service), userRepository)

    private fun InquiryViewModel.fill() {
        selectType(InquiryType.TECHNICAL)
        updateEmail("  me@moamap.com ")
        updateContent("\n지도가 안 열려요  ")
    }

    @Test
    fun `처음 열면 계정 이메일로 이메일 칸을 채운다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals("me@moamap.com", viewModel.uiState.value.email)
    }

    @Test
    fun `먼저 적은 이메일은 늦게 도착한 계정 이메일로 덮지 않는다`() = runTest(dispatcher) {
        val late = CompletableDeferred<String>()
        val viewModel = viewModel(userRepository = FakeUserRepository(profileEmail = late))
        advanceUntilIdle()

        viewModel.updateEmail("other@moamap.com")
        late.complete("me@moamap.com")
        advanceUntilIdle()

        assertEquals("other@moamap.com", viewModel.uiState.value.email)
    }

    @Test
    fun `지운 이메일은 다시 열어도 채우지 않는다`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val first = viewModel(savedStateHandle = handle)
        advanceUntilIdle()
        first.updateEmail("")

        // 프로세스가 죽었다 살아난 경우다.
        val restored = viewModel(savedStateHandle = handle)
        advanceUntilIdle()

        assertEquals("", restored.uiState.value.email)
    }

    @Test
    fun `내용은 2000자까지만 받는다`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.updateContent("가".repeat(2001))

        assertEquals(2000, viewModel.uiState.value.content.length)
    }

    @Test
    fun `보내면 앞뒤 공백을 떼고 한 번만 보낸 뒤 접수 상태가 된다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.fill()

        viewModel.submit()
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(
            listOf(InquiryRequestDto(type = "TECHNICAL", replyEmail = "me@moamap.com", content = "지도가 안 열려요")),
            service.requests,
        )
        assertTrue(viewModel.uiState.value.submitted)
    }

    @Test
    fun `하루 한도를 넘기면 서버 안내를 보여 주고 입력은 그대로 둔다`() = runTest(dispatcher) {
        val limitMessage = "문의는 하루 10건까지 보낼 수 있습니다. 잠시 후 다시 시도해 주세요."
        service.result = { throw ApiException(code = "USER_013", status = 429, serverMessage = limitMessage) }
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.fill()

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(limitMessage, state.errorMessage)
        assertFalse(state.submitted)
        assertFalse(state.submitting)
        assertEquals("\n지도가 안 열려요  ", state.content)
    }

    @Test
    fun `서버 내부 오류는 정해 둔 안내로 바꾼다`() = runTest(dispatcher) {
        service.result = { throw ApiException(code = "COMMON_500", status = 500, serverMessage = "Internal") }
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.fill()

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(INQUIRY_SEND_FAILED_MESSAGE, viewModel.uiState.value.errorMessage)
        viewModel.consumeError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `계정 이메일을 못 받아도 빈칸으로 두고 넘어간다`() = runTest(dispatcher) {
        val failed = CompletableDeferred<String>().apply { completeExceptionally(IOException("연결 실패")) }
        val viewModel = viewModel(userRepository = FakeUserRepository(profileEmail = failed))
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.email)
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
