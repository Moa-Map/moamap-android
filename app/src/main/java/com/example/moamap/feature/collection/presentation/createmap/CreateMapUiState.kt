package com.example.moamap.feature.collection.presentation.createmap

import androidx.compose.runtime.Immutable
import com.example.moamap.feature.collection.domain.model.MapVisibility

/** 서버 `MapCreateRequest` 의 `@Size` 제약. 입력 단계에서 미리 막아 400 을 만들지 않는다. */
internal const val NAME_MAX_LENGTH = 100
internal const val DESCRIPTION_MAX_LENGTH = 500
internal const val TAG_MAX_LENGTH = 30

/** 만들기 진행 상태. */
internal sealed interface SubmitState {
    data object Idle : SubmitState
    data object Submitting : SubmitState

    /** 프라이빗 지도라 초대 코드를 보여줄 차례다. 지도는 이미 만들어졌다. */
    data class ShowingInviteCode(val mapId: Long, val inviteCode: String) : SubmitState

    data class Done(val mapId: Long) : SubmitState
}

/**
 * 새 지도 만들기 화면 상태.
 *
 * TODO: [imageUri] 는 미리보기에만 쓰고 서버로 보내지 않는다. 지도 커버 이미지 업로드
 *  엔드포인트가 서버에 없어서 넣을 URL 을 만들 수 없다. 생기면 여기서 함께 올린다.
 */
@Immutable
internal data class CreateMapUiState(
    val imageUri: String? = null,
    val name: String = "",
    val description: String = "",
    val visibility: MapVisibility? = null,
    val tags: List<String> = emptyList(),
    /** 아직 확정되지 않은 태그 입력값. */
    val tagInput: String = "",
    val submit: SubmitState = SubmitState.Idle,
    val errorMessage: String? = null,
) {
    val isSubmitting: Boolean
        get() = submit is SubmitState.Submitting

    /** 이름과 공개 범위는 서버 필수값이라 둘 다 채워야 만들 수 있다. */
    val canSubmit: Boolean
        get() = name.isNotBlank() && visibility != null && !isSubmitting
}
