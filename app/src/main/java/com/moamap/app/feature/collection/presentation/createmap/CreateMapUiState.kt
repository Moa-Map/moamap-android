package com.moamap.app.feature.collection.presentation.createmap

import androidx.compose.runtime.Immutable
import com.moamap.app.feature.collection.domain.model.MapVisibility

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
 * 올려둔 커버.
 *
 * [sourceUri] 를 함께 들고 있어야 사진을 바꿔 고른 뒤에도 옛 주소를 쓰는 일이 없다.
 */
@Immutable
internal data class UploadedCover(val sourceUri: String, val fileUrl: String)

/** 새 지도 만들기 화면 상태. */
@Immutable
internal data class CreateMapUiState(
    /** 고른 사진의 `content://` URI. 미리보기에 쓰고, 제출할 때 이 사진을 올린다. */
    val imageUri: String? = null,
    /** 이미 올려둔 커버. 재시도할 때 다시 올리지 않으려고 붙잡는다. */
    val uploadedCover: UploadedCover? = null,
    val name: String = "",
    val description: String = "",
    val visibility: MapVisibility? = null,
    val tags: List<String> = emptyList(),
    /** 아직 확정되지 않은 태그 입력값. */
    val tagInput: String = "",
    val submit: SubmitState = SubmitState.Idle,
    val errorMessage: String? = null,
) {
    /** 지금 고른 사진에 대해 이미 받아둔 주소. 사진을 바꿨으면 쓰지 않는다. */
    val reusableCoverUrl: String?
        get() = uploadedCover?.takeIf { it.sourceUri == imageUri }?.fileUrl

    val isSubmitting: Boolean
        get() = submit is SubmitState.Submitting

    /** 이름과 공개 범위는 서버 필수값이라 둘 다 채워야 만들 수 있다. */
    val canSubmit: Boolean
        get() = name.isNotBlank() && visibility != null && !isSubmitting
}
