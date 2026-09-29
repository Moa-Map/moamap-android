package com.moamap.app.feature.explore.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.feature.explore.domain.model.CommunityMap
import com.moamap.app.feature.explore.domain.model.CommunityMapSort
import com.moamap.app.feature.explore.domain.repository.CommunityMapRepository
import com.moamap.app.feature.mypage.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 탐색 탭에 보여 주는 커뮤니티 지도 수. 고른 정렬로 앞에서부터 받고, 나머지는 「전체보기」에서 본다. */
internal const val EXPLORE_COMMUNITY_MAP_COUNT = 5

/** 목록 영역의 상태. 추천·이름과 따로 실패할 수 있어 바깥 상태와 분리한다. */
sealed interface CommunityMapsState {
    data object Loading : CommunityMapsState
    data class Success(val maps: List<CommunityMap>) : CommunityMapsState
    data class Error(val message: String) : CommunityMapsState
}

data class ExploreUiState(
    val sort: CommunityMapSort = CommunityMapSort.POPULAR,
    val communityMaps: CommunityMapsState = CommunityMapsState.Loading,
    /** 추천 섹션 제목에 넣을 내 이름. 못 읽었으면 비어 있고, 화면이 대체 말을 쓴다. */
    val nickname: String = "",
    /**
     * 추천 지도. 비어 있으면 화면이 섹션을 그리지 않는다.
     *
     * 아직 못 읽음·실패·서버가 준 빈 배열이 모두 "그릴 것이 없다" 로 모여서 목록처럼
     * 상태 타입을 따로 두지 않는다. 보조 섹션이라 로딩도 오류도 그리지 않는다.
     */
    val recommendedMaps: List<CommunityMap> = emptyList(),
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repository: CommunityMapRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    /** 화면을 빠르게 오가면 이전 요청은 버린다. 늦게 도착한 응답이 최신 결과를 덮지 않게 한다. */
    private var loadJob: Job? = null

    /** 추천은 목록과 따로 오간다. 화면을 빠르게 오갈 때 이전 요청을 버리는 용도다. */
    private var recommendationJob: Job? = null

    private companion object {
        const val TAG = "ExploreViewModel"
        const val LOAD_FAILED_MESSAGE = "지도 목록을 불러오지 못했어요"
    }

    init {
        loadNickname()
    }

    /**
     * 제목에 넣을 내 이름을 한 번만 읽는다.
     *
     * 화면이 보일 때마다 읽지 않는다 - 이름은 프로필 편집에서만 바뀌는데, 그 드문 경우를
     * 위해 탭을 오갈 때마다 요청을 하나 더 내보내게 된다. 편집 후 제목이 낡으면 다음 실행에
     * 맞춰진다.
     *
     * 실패하면 조용히 넘긴다. 이름 하나 때문에 화면에 오류를 띄울 일은 아니고, 화면은
     * 빈 이름을 대체 말로 메운다.
     */
    private fun loadNickname() {
        viewModelScope.launch {
            try {
                val nickname = userRepository.getMyProfile().nickname
                _uiState.update { it.copy(nickname = nickname) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "내 프로필 조회 실패", e)
            }
        }
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
    fun refresh() {
        load(keepCurrent = _uiState.value.communityMaps is CommunityMapsState.Success)
        loadRecommendations()
    }

    fun retry() = load()

    /**
     * 추천 목록을 읽는다.
     *
     * 목록 재시도([retry])에 얹지 않는다 - 목록만 실패했을 때 멀쩡한 추천까지 다시 부르게 된다.
     *
     * 실패하면 조용히 넘긴다. 보조 섹션이라 오류를 띄우지 않고, 이미 그린 카드도 지우지
     * 않는다 - 돌아올 때마다 섹션이 사라지면 안 된다.
     */
    private fun loadRecommendations() {
        recommendationJob?.cancel()
        recommendationJob = viewModelScope.launch {
            try {
                val maps = repository.getRecommendedMaps()
                _uiState.update { it.copy(recommendedMaps = maps) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "추천 지도 조회 실패", e)
            }
        }
    }

    /** 같은 정렬을 다시 누르면 아무것도 하지 않는다. 추천은 정렬과 무관해 다시 읽지 않는다. */
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
        val sort = _uiState.value.sort
        loadJob?.cancel()
        if (!keepCurrent) {
            _uiState.update { it.copy(communityMaps = CommunityMapsState.Loading) }
        }
        loadJob = viewModelScope.launch {
            try {
                val maps = repository.getCommunityMaps(
                    tag = null,
                    sort = sort,
                    page = 0,
                    size = EXPLORE_COMMUNITY_MAP_COUNT,
                ).maps
                _uiState.update { it.copy(communityMaps = CommunityMapsState.Success(maps)) }
            } catch (e: CancellationException) {
                // 다음 선택이 이미 로딩을 시작했다. 이 요청의 결과로 상태를 건드리면 안 된다.
                throw e
            } catch (e: Exception) {
                // 예외 메시지는 그대로 노출하지 않는다. ApiException 은 "[500] COMMON_005: ..."
                // 처럼 사용자에게 보여줄 수 없는 형태다.
                Log.w(TAG, "커뮤니티 지도 목록 조회 실패 (sort=$sort)", e)
                // 새로고침이 실패했는데 이미 보여줄 목록이 있으면 지우지 않는다.
                if (keepCurrent) return@launch
                _uiState.update {
                    it.copy(communityMaps = CommunityMapsState.Error(LOAD_FAILED_MESSAGE))
                }
            }
        }
    }
}
