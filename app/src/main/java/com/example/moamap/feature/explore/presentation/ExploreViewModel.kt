package com.example.moamap.feature.explore.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moamap.feature.explore.domain.model.CommunityMap
import com.example.moamap.feature.explore.domain.model.CommunityMapSort
import com.example.moamap.feature.explore.domain.repository.CommunityMapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** "전체" 는 태그 필터를 걸지 않는다는 뜻이라 서버로 보내지 않는다. */
const val ALL_CATEGORY: String = "전체"

/**
 * 카테고리 칩 목록.
 *
 * 서버에 태그 사전이 없어 라벨이 곧 태그 문자열이고,
 * 지도에 달린 태그와 정확히 일치해야 필터가 걸린다.
 */
val ExploreCategories: List<String> = listOf(ALL_CATEGORY, "카페", "데이트", "산책", "힙플")

/** 목록 영역의 상태. 칩과 정렬은 재조회 중에도 눌러야 하므로 바깥 상태와 분리한다. */
sealed interface CommunityMapsState {
    data object Loading : CommunityMapsState
    data class Success(val maps: List<CommunityMap>) : CommunityMapsState
    data class Error(val message: String) : CommunityMapsState
}

data class ExploreUiState(
    val selectedCategory: String = ALL_CATEGORY,
    val sort: CommunityMapSort = CommunityMapSort.POPULAR,
    val communityMaps: CommunityMapsState = CommunityMapsState.Loading,
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repository: CommunityMapRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    /** 칩을 연달아 누르면 이전 요청은 버린다. 늦게 도착한 응답이 최신 선택을 덮지 않게 한다. */
    private var loadJob: Job? = null

    private companion object {
        const val TAG = "ExploreViewModel"
        const val LOAD_FAILED_MESSAGE = "지도 목록을 불러오지 못했어요"
    }

    /**
     * 화면이 보일 때 목록을 읽는다. 첫 조회도 이 경로가 겸한다.
     *
     * 지도에 참여하거나 나가고 돌아오는 경로가 여기다. 탭 전환이 상태를 복원하므로 이
     * ViewModel 은 살아남고, 다시 읽지 않으면 카드의 참여 여부가 낡아 진입 분기가 틀어진다.
     *
     * `init` 에서 첫 조회를 하지 않는 이유는, 화면이 처음 뜰 때 이 함수도 함께 불려
     * 같은 요청이 두 번 나가기 때문이다.
     */
    fun refresh() = load(keepCurrent = _uiState.value.communityMaps is CommunityMapsState.Success)

    fun retry() = load()

    fun selectCategory(category: String) {
        if (_uiState.value.selectedCategory == category) return
        _uiState.update { it.copy(selectedCategory = category) }
        load()
    }

    fun selectSort(sort: CommunityMapSort) {
        if (_uiState.value.sort == sort) return
        _uiState.update { it.copy(sort = sort) }
        load()
    }

    /**
     * @param keepCurrent true 면 보고 있던 목록을 지우지 않는다. 돌아올 때마다 목록이
     *  사라졌다 나타나면 화면이 깜빡인다.
     */
    private fun load(keepCurrent: Boolean = false) {
        val (category, sort) = _uiState.value.let { it.selectedCategory to it.sort }
        val tag = category.takeIf { it != ALL_CATEGORY }

        loadJob?.cancel()
        if (!keepCurrent) {
            _uiState.update { it.copy(communityMaps = CommunityMapsState.Loading) }
        }
        loadJob = viewModelScope.launch {
            try {
                val maps = repository.getCommunityMaps(tag = tag, sort = sort)
                _uiState.update { it.copy(communityMaps = CommunityMapsState.Success(maps)) }
            } catch (e: CancellationException) {
                // 다음 선택이 이미 로딩을 시작했다. 이 요청의 결과로 상태를 건드리면 안 된다.
                throw e
            } catch (e: Exception) {
                // 예외 메시지는 그대로 노출하지 않는다. ApiException 은 "[500] COMMON_005: ..."
                // 처럼 사용자에게 보여줄 수 없는 형태다.
                Log.w(TAG, "커뮤니티 지도 목록 조회 실패 (tag=$tag, sort=$sort)", e)
                // 새로고침이 실패했는데 이미 보여줄 목록이 있으면 지우지 않는다.
                if (keepCurrent) return@launch
                _uiState.update {
                    it.copy(communityMaps = CommunityMapsState.Error(LOAD_FAILED_MESSAGE))
                }
            }
        }
    }
}
