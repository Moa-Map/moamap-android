package com.example.moamap.feature.collection.presentation

import androidx.compose.runtime.Immutable
import com.example.moamap.feature.collection.domain.model.MapType
import com.example.moamap.feature.collection.domain.model.MyMap

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
) {
    val currentMaps: MyMapsState
        get() = stateOf(selectedTab)

    fun stateOf(type: MapType): MyMapsState = when (type) {
        MapType.Community -> community
        MapType.Private -> private
    }

    fun withState(type: MapType, state: MyMapsState): CollectionUiState = when (type) {
        MapType.Community -> copy(community = state)
        MapType.Private -> copy(private = state)
    }
}
