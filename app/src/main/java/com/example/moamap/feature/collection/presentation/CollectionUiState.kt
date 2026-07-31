package com.example.moamap.feature.collection.presentation

import androidx.compose.runtime.Immutable
import com.example.moamap.feature.collection.domain.model.MapType
import com.example.moamap.feature.collection.domain.model.MyMap

/** 초대 코드 입력 모달. */
sealed interface JoinState {
    data object Hidden : JoinState

    data class Editing(
        val code: String = "",
        val submitting: Boolean = false,
        val errorMessage: String? = null,
    ) : JoinState {
        val canSubmit: Boolean get() = code.isNotBlank() && !submitting
    }
}

/** 목록 영역의 상태. 탭 전환은 목록 바깥이므로 바뀜 상태와 분리한다. */
sealed interface MyMapsState {
    data object Loading : MyMapsState
    data class Success(val maps: List<MyMap>) : MyMapsState
    data class Error(val message: String) : MyMapsState
}

/**
 * 모음 화면 상태.
 *
 * 탭마다 목록을 따로 들고 있는다. 한 슬롯에 덮어쓰면 탭을 오갈 때마다 이미 받은 목록이
 * 사라져 매번 로딩이 뜬다.
 */
@Immutable
data class CollectionUiState(
    val selectedTab: MapType = MapType.Community,
    val community: MyMapsState = MyMapsState.Loading,
    val private: MyMapsState = MyMapsState.Loading,
    val join: JoinState = JoinState.Hidden,
) {
    val currentMaps: MyMapsState
        get() = stateOf(selectedTab)

    /**
     * 공식지도는 이 화면이 다루지 않는 종류다. 탭으로 고를 수 없어 [selectedTab] 이 될 일이
     * 없고, 슬롯도 없다. 목록이 빈 것으로 답한다 - 모음에 걸리는 공식지도가 없다는 뜻이라
     * 사실과 어긋나지 않고, 예외를 던져 화면을 죽이는 것보다 낫다.
     */
    fun stateOf(type: MapType): MyMapsState = when (type) {
        MapType.Community -> community
        MapType.Private -> private
        MapType.Official -> MyMapsState.Success(emptyList())
    }

    /** 공식지도는 담아 둘 슬롯이 없다. 그대로 둔다 - [stateOf] 참고. */
    fun withState(type: MapType, state: MyMapsState): CollectionUiState = when (type) {
        MapType.Community -> copy(community = state)
        MapType.Private -> copy(private = state)
        MapType.Official -> this
    }
}
