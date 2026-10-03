package com.moamap.app.feature.explore.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.feature.explore.domain.model.CommunityMap
import com.moamap.app.feature.explore.domain.model.CommunityMapSort
import com.moamap.app.feature.explore.domain.repository.CommunityMapRepository
import com.moamap.app.feature.officialmap.domain.repository.OfficialMapRepository
import com.moamap.app.feature.officialmap.presentation.OfficialMapsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 탐색 탭에 보여 주는 커뮤니티 지도 수. 인기순 앞에서부터 받고, 나머지는 「전체보기」에서 본다. */
internal const val EXPLORE_COMMUNITY_MAP_COUNT = 3

/** 탐색 탭에 보여 주는 공식지도 수. 나머지는 「전체보기」(공식지도 목록)에서 본다. */
internal const val EXPLORE_OFFICIAL_MAP_COUNT = 5

/** 목록 영역의 상태. 공식지도와 따로 실패할 수 있어 바깥 상태와 분리한다. */
sealed interface CommunityMapsState {
    data object Loading : CommunityMapsState
    data class Success(val maps: List<CommunityMap>) : CommunityMapsState
    data class Error(val message: String) : CommunityMapsState
}

data class ExploreUiState(
    val communityMaps: CommunityMapsState = CommunityMapsState.Loading,
    val officialMaps: OfficialMapsState = OfficialMapsState.Loading,
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repository: CommunityMapRepository,
    private val officialMapRepository: OfficialMapRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    /** 화면을 빠르게 오가면 이전 요청은 버린다. 늦게 도착한 응답이 최신 결과를 덮지 않게 한다. */
    private var communityJob: Job? = null
    private var officialJob: Job? = null

    private companion object {
        const val TAG = "ExploreViewModel"
        const val COMMUNITY_LOAD_FAILED_MESSAGE = "지도 목록을 불러오지 못했어요"
        const val OFFICIAL_LOAD_FAILED_MESSAGE = "공식 지도를 불러오지 못했어요"
    }

    /**
     * 화면이 보일 때 두 목록을 읽는다. 첫 조회도 이 경로가 겸한다.
     *
     * 지도에 참여하거나 나가고 돌아오는 경로가 여기다. 탭 전환이 상태를 복원하므로 이
     * ViewModel 은 살아남고, 다시 읽지 않으면 카드의 참여 여부가 낡아 진입 분기가 틀어진다.
     *
     * `init` 에서 첫 조회를 하지 않는 이유는, 화면이 처음 뜰 때 이 함수도 함께 불려
     * 같은 요청이 두 번 나가기 때문이다.
     */
    fun refresh() {
        loadCommunityMaps(keepCurrent = _uiState.value.communityMaps is CommunityMapsState.Success)
        loadOfficialMaps(keepCurrent = _uiState.value.officialMaps is OfficialMapsState.Success)
    }

    /** 커뮤니티 목록만 다시 읽는다. 멀쩡한 공식지도까지 다시 부르지 않는다. */
    fun retryCommunityMaps() = loadCommunityMaps()

    fun retryOfficialMaps() = loadOfficialMaps()

    /**
     * @param keepCurrent true 면 보고 있던 목록을 지우지 않는다. 돌아올 때마다 목록이
     *  사라졌다 나타나면 화면이 깜빡인다.
     */
    private fun loadCommunityMaps(keepCurrent: Boolean = false) {
        communityJob?.cancel()
        if (!keepCurrent) {
            _uiState.update { it.copy(communityMaps = CommunityMapsState.Loading) }
        }
        communityJob = viewModelScope.launch {
            try {
                val maps = repository.getCommunityMaps(
                    tag = null,
                    sort = CommunityMapSort.POPULAR,
                    page = 0,
                    size = EXPLORE_COMMUNITY_MAP_COUNT,
                ).maps
                _uiState.update { it.copy(communityMaps = CommunityMapsState.Success(maps)) }
            } catch (e: CancellationException) {
                // 다음 조회가 이미 로딩을 시작했다. 이 요청의 결과로 상태를 건드리면 안 된다.
                throw e
            } catch (e: Exception) {
                // 예외 메시지는 그대로 노출하지 않는다. ApiException 은 "[500] COMMON_005: ..."
                // 처럼 사용자에게 보여줄 수 없는 형태다.
                Log.w(TAG, "커뮤니티 지도 목록 조회 실패", e)
                // 새로고침이 실패했는데 이미 보여줄 목록이 있으면 지우지 않는다.
                if (keepCurrent) return@launch
                _uiState.update {
                    it.copy(communityMaps = CommunityMapsState.Error(COMMUNITY_LOAD_FAILED_MESSAGE))
                }
            }
        }
    }

    /** 공식지도는 서버가 페이지로 주지만 몇 개 안 된다. 받은 첫 페이지에서 앞 몇 개만 쓴다. */
    private fun loadOfficialMaps(keepCurrent: Boolean = false) {
        officialJob?.cancel()
        if (!keepCurrent) {
            _uiState.update { it.copy(officialMaps = OfficialMapsState.Loading) }
        }
        officialJob = viewModelScope.launch {
            try {
                val maps = officialMapRepository.getOfficialMaps().take(EXPLORE_OFFICIAL_MAP_COUNT)
                _uiState.update { it.copy(officialMaps = OfficialMapsState.Success(maps)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "공식지도 목록 조회 실패", e)
                if (keepCurrent) return@launch
                _uiState.update {
                    it.copy(officialMaps = OfficialMapsState.Error(OFFICIAL_LOAD_FAILED_MESSAGE))
                }
            }
        }
    }
}
