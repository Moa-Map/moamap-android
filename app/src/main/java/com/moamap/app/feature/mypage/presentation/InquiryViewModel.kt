package com.moamap.app.feature.mypage.presentation

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.common.actionableMessage
import com.moamap.app.core.common.toUserMessage
import com.moamap.app.core.network.ApiException
import com.moamap.app.feature.mypage.domain.model.InquiryType
import com.moamap.app.feature.mypage.domain.repository.InquiryRepository
import com.moamap.app.feature.mypage.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "InquiryVM"

internal const val INQUIRY_SEND_FAILED_MESSAGE = "문의를 보내지 못했어요"

@Immutable
data class InquiryUiState(
    val type: InquiryType? = null,
    val email: String = "",
    val content: String = "",
    val submitting: Boolean = false,
    val errorMessage: String? = null,
    /** 접수됐다. 화면이 접수 팝업을 띄우고, 닫으면 문의 화면을 나간다. */
    val submitted: Boolean = false,
) {
    val canSubmit: Boolean get() = !submitting && isInquiryComplete(type = type, email = email, content = content)
}

/**
 * 설정 「문의하기」.
 *
 * 답변 받을 이메일은 처음 열 때 내 계정 이메일로 한 번 채운다(백엔드 요청, 10-20 사용자 결정). 사용자가 먼저 적었거나
 * 지웠으면 다시 채우지 않는다.
 */
@HiltViewModel
class InquiryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val inquiryRepository: InquiryRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(savedStateHandle.toInquiryUiState())
    val uiState: StateFlow<InquiryUiState> = _uiState.asStateFlow()

    init {
        if (savedStateHandle.get<Boolean>(KEY_EMAIL_PREFILLED) != true) prefillEmail()
    }

    fun selectType(type: InquiryType) = updateState { state -> state.copy(type = type) }

    fun updateEmail(email: String) {
        // 사용자가 손댄 칸은 늦게 도착한 계정 이메일로 덮지 않는다.
        savedStateHandle[KEY_EMAIL_PREFILLED] = true
        updateState { state -> state.copy(email = email.take(INQUIRY_EMAIL_MAX_LENGTH)) }
    }

    fun updateContent(content: String) =
        updateState { state -> state.copy(content = content.take(INQUIRY_CONTENT_MAX_LENGTH)) }

    fun submit() {
        val current = _uiState.value
        val type = current.type
        if (!current.canSubmit || type == null) return

        // 코루틴이 시작되기 전에 잠근다. 두 번 눌러도 문의가 한 번만 간다.
        updateState { state -> state.copy(submitting = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                inquiryRepository.sendInquiry(type = type, replyEmail = current.email, content = current.content)
                updateState { state -> state.copy(submitting = false, submitted = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 이메일·내용은 로그에 남기지 않는다.
                Log.w(TAG, "문의 보내기 실패 (type=$type)", e)
                // 하루 한도를 넘긴 것(429)처럼 사용자가 알아야 할 서버 안내는 그대로 보여 준다.
                val message = (e as? ApiException)?.actionableMessage() ?: e.toUserMessage(INQUIRY_SEND_FAILED_MESSAGE)
                updateState { state -> state.copy(submitting = false, errorMessage = message) }
            }
        }
    }

    /** 안내를 보여준 뒤 호출한다. 같은 메시지가 다시 뜨지 않게 한다. */
    fun consumeError() = updateState { state -> state.copy(errorMessage = null) }

    private fun prefillEmail() {
        viewModelScope.launch {
            val email = try {
                userRepository.getMyProfile().email.trim()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 못 받아도 빈칸으로 두면 된다. 사용자가 적으면 그만이다.
                Log.w(TAG, "계정 이메일을 불러오지 못함", e)
                return@launch
            }
            if (savedStateHandle.get<Boolean>(KEY_EMAIL_PREFILLED) == true) return@launch
            savedStateHandle[KEY_EMAIL_PREFILLED] = true
            if (_uiState.value.email.isEmpty()) {
                updateState { state -> state.copy(email = email.take(INQUIRY_EMAIL_MAX_LENGTH)) }
            }
        }
    }

    /** 상태 변경과 저장을 한 자리에 묶어, 저장을 빠뜨린 경로가 생기지 않게 한다. */
    private fun updateState(transform: (InquiryUiState) -> InquiryUiState) {
        val next = transform(_uiState.value)
        _uiState.value = next
        savedStateHandle.save(next)
    }
}

private const val KEY_TYPE = "inquiry.type"
private const val KEY_EMAIL = "inquiry.email"
private const val KEY_CONTENT = "inquiry.content"
private const val KEY_SUBMITTED = "inquiry.submitted"
private const val KEY_EMAIL_PREFILLED = "inquiry.emailPrefilled"

/** 보내는 중은 되살리지 않는다. 요청은 프로세스와 함께 사라졌는데 잠긴 채 되살아나면 다시 보낼 수 없다. */
private fun SavedStateHandle.toInquiryUiState() = InquiryUiState(
    type = get<String>(KEY_TYPE)?.let { saved -> InquiryType.entries.firstOrNull { type -> type.name == saved } },
    email = get<String>(KEY_EMAIL).orEmpty(),
    content = get<String>(KEY_CONTENT).orEmpty(),
    submitted = get<Boolean>(KEY_SUBMITTED) == true,
)

private fun SavedStateHandle.save(state: InquiryUiState) {
    this[KEY_TYPE] = state.type?.name
    this[KEY_EMAIL] = state.email
    this[KEY_CONTENT] = state.content
    this[KEY_SUBMITTED] = state.submitted
}
