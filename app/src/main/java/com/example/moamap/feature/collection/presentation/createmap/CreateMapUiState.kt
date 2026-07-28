package com.example.moamap.feature.collection.presentation.createmap

import androidx.compose.runtime.Immutable

/** 서버 `MapCreateRequest` 의 `@Size` 제약. 입력 단계에서 미리 막아 400 을 만들지 않는다. */
internal const val NAME_MAX_LENGTH = 100
internal const val DESCRIPTION_MAX_LENGTH = 500
internal const val TAG_MAX_LENGTH = 30

/** 서버 `MapVisibility` 와 1:1 로 대응한다. */
internal enum class MapVisibility {
    Public,
    Private,
}

/**
 * 새 지도 만들기 화면 상태.
 *
 * 사진은 아직 업로드할 곳이 없어 로컬 URI 만 들고 있다가 미리보기에만 쓴다.
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
) {
    /** 이름과 공개 범위는 서버 필수값이라 둘 다 채워야 만들 수 있다. */
    val canSubmit: Boolean get() = name.isNotBlank() && visibility != null
}
