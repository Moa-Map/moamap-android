package com.moamap.app.feature.explore.presentation

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.feature.explore.domain.model.CommunityMap
import com.moamap.app.feature.explore.domain.model.CommunityMapSort
import com.moamap.app.feature.explore.domain.repository.CommunityMapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CommunityMapListVM"

/** 한 번에 받는 수. 서버 기본값과 같다. */
internal const val COMMUNITY_MAP_PAGE_SIZE = 20

/**
 * 칩으로 만드는 태그 수의 상한.
 *
 * 첫 페이지 20곳에 태그가 3개씩이면 60개까지 나와 칩 줄이 끝없이 길어진다. 많이 쓰인 것부터 자른다.
 */
internal const val MAX_TAG_CHIPS = 10

private const val LOAD_FAILED_MESSAGE = "지도 목록을 불러오지 못했어요"

/**
 * 커뮤니티 지도 전체보기 상태.
 *
 * 첫 페이지 실패([errorMessage])와 다음 페이지 실패([loadMoreFailed])를 나눈다. 로그 탭 게시물 목록과 같다.
 */
@Immutable
data class CommunityMapListUiState(
    /**
     * 칩으로 보여 줄 태그. 「전체」는 화면이 앞에 붙인다.
     *
     * 서버에 태그 목록 API가 없어, 거르지 않은 첫 페이지에 달린 태그를 많이 쓰인 순으로 모은다.
     * 한 번 만들면 그대로 둔다 - 칩을 고를 때마다 새로 만들면 고른 태그만 남는다.
     */
    val tags: List<String> = emptyList(),
    /** null 이면 「전체」. */
    val selectedTag: String? = null,
    /** 처음에는 최신순(10-04 사용자 결정). */
    val sort: CommunityMapSort = CommunityMapSort.LATEST,
    val loading: Boolean = true,
    val maps: List<CommunityMap> = emptyList(),
    val errorMessage: String? = null,
    val loadingMore: Boolean = false,
    val loadMoreFailed: Boolean = false,
    /** 마지막 페이지까지 받았다. */
    val endReached: Boolean = false,
)

@HiltViewModel
class CommunityMapListViewModel @Inject constructor(
    private val repository: CommunityMapRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityMapListUiState())
    val uiState: StateFlow<CommunityMapListUiState> = _uiState.asStateFlow()

    /**
     * 진행 중인 조회. 첫 페이지·다음 페이지·다시 읽기가 한 자리를 쓴다.
     *
     * 칩이나 정렬을 바꾸면 이전 조건의 응답이 늦게 도착해 새 목록을 덮을 수 있다. 새 조회가
     * 이전 것을 취소하게 해 막는다.
     */
    private var loadJob: Job? = null

    /** 다음에 받을 페이지 번호. 0 이면 아직 한 페이지도 받지 못했다. */
    private var nextPage = 0

    /**
     * 「사용자 맞춤」으로 받아 둔 추천 전체. 칩을 고르면 다시 받지 않고 이 안에서 거른다 -
     * 추천 API 에는 태그 거르기가 없다(10-04 사용자 결정).
     */
    private var recommendations: List<CommunityMap> = emptyList()

    /**
     * 화면이 보일 때 부른다.
     *
     * 처음이면 첫 페이지를 읽는다. 지도에 들어갔다 돌아왔으면 참여 여부가 바뀌었을 수 있어,
     * 받아 둔 만큼을 요청 한 번으로 다시 읽는다. 목록을 비우지 않아 스크롤 자리가 그대로다.
     */
    fun refresh() {
        if (nextPage == 0) {
            // 첫 페이지를 받는 중이면 같은 요청을 또 보내지 않는다.
            if (loadJob?.isActive == true) return
            loadFirstPage()
            return
        }

        val state = _uiState.value
        if (state.sort == CommunityMapSort.RECOMMENDED) {
            refreshRecommendations()
            return
        }
        val loadedPages = nextPage
        _uiState.update { current -> current.copy(loadingMore = false) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val result = repository.getCommunityMaps(
                    tag = state.selectedTag,
                    sort = state.sort,
                    page = 0,
                    size = loadedPages * COMMUNITY_MAP_PAGE_SIZE,
                )
                _uiState.update { current ->
                    current.copy(maps = result.maps, endReached = result.isLast, loadMoreFailed = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 보던 목록이 있으니 지우지 않는다. 참여 여부만 낡은 채로 남는다.
                Log.w(TAG, "커뮤니티 지도 다시 읽기 실패", e)
            }
        }
    }

    fun retry() = loadFirstPage()

    /** 같은 칩을 다시 누르면 아무것도 하지 않는다. 「사용자 맞춤」은 받아 둔 추천 안에서 거른다. */
    fun selectTag(tag: String?) {
        val state = _uiState.value
        if (tag == state.selectedTag) return
        if (state.sort == CommunityMapSort.RECOMMENDED && recommendationsLoaded(state)) {
            _uiState.update { current -> current.copy(selectedTag = tag, maps = recommendations.withTag(tag)) }
            return
        }
        _uiState.update { current -> current.copy(selectedTag = tag) }
        loadFirstPage()
    }

    fun selectSort(sort: CommunityMapSort) {
        if (sort == _uiState.value.sort) return
        _uiState.update { state -> state.copy(sort = sort) }
        loadFirstPage()
    }

    /**
     * 스크롤이 끝에 닿았다.
     *
     * 첫 페이지를 받는 중이거나 실패했으면 부르지 않는다. 그때 다음 페이지를 받으면 첫 페이지 없이
     * 둘째 페이지만 화면에 붙는다.
     */
    fun loadMore() {
        val state = _uiState.value
        if (state.loading || state.loadingMore || state.endReached || state.errorMessage != null) return

        _uiState.update { current -> current.copy(loadingMore = true, loadMoreFailed = false) }
        val page = nextPage

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val result = repository.getCommunityMaps(
                    tag = state.selectedTag,
                    sort = state.sort,
                    page = page,
                    size = COMMUNITY_MAP_PAGE_SIZE,
                )
                nextPage = page + 1
                _uiState.update { current ->
                    // 받는 사이 새 지도가 생기면 페이지 경계가 밀려 앞 페이지의 지도가 한 번 더 온다.
                    val known = current.maps.mapTo(HashSet()) { map -> map.id }
                    current.copy(
                        maps = current.maps + result.maps.filterNot { map -> map.id in known },
                        loadingMore = false,
                        endReached = result.isLast,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "커뮤니티 지도 다음 페이지 조회 실패 (page=$page)", e)
                _uiState.update { current -> current.copy(loadingMore = false, loadMoreFailed = true) }
            }
        }
    }

    /**
     * 「사용자 맞춤」을 처음부터 받는다. 추천은 페이지가 없어 한 번에 다 받고, 더 불러오지 않는다.
     */
    private fun loadRecommendations() {
        _uiState.update { current ->
            current.copy(
                loading = true,
                maps = emptyList(),
                errorMessage = null,
                loadingMore = false,
                loadMoreFailed = false,
                endReached = true,
            )
        }
        nextPage = 0

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val maps = repository.getRecommendedMaps()
                recommendations = maps
                // 0 이 아니어야 돌아왔을 때 처음부터가 아니라 다시 읽기로 간다.
                nextPage = 1
                _uiState.update { current ->
                    current.copy(loading = false, maps = maps.withTag(current.selectedTag))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "사용자 맞춤 지도 조회 실패", e)
                _uiState.update { current ->
                    current.copy(loading = false, errorMessage = LOAD_FAILED_MESSAGE)
                }
            }
        }
    }

    /** 지도에 들어갔다 돌아왔다. 참여한 지도는 추천에서 빠지므로 다시 받되, 보던 목록은 지우지 않는다. */
    private fun refreshRecommendations() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val maps = repository.getRecommendedMaps()
                recommendations = maps
                _uiState.update { current -> current.copy(maps = maps.withTag(current.selectedTag)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "사용자 맞춤 지도 다시 읽기 실패", e)
            }
        }
    }

    private fun loadFirstPage() {
        val state = _uiState.value
        if (state.sort == CommunityMapSort.RECOMMENDED) {
            loadRecommendations()
            return
        }
        val tag = state.selectedTag
        val sort = state.sort
        _uiState.update { current ->
            current.copy(
                loading = true,
                maps = emptyList(),
                errorMessage = null,
                loadingMore = false,
                loadMoreFailed = false,
                endReached = false,
            )
        }
        nextPage = 0

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val result = repository.getCommunityMaps(
                    tag = tag,
                    sort = sort,
                    page = 0,
                    size = COMMUNITY_MAP_PAGE_SIZE,
                )
                nextPage = 1
                _uiState.update { current ->
                    current.copy(
                        loading = false,
                        maps = result.maps,
                        endReached = result.isLast,
                        tags = if (current.tags.isEmpty() && tag == null) {
                            tagsByFrequency(result.maps)
                        } else {
                            current.tags
                        },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "커뮤니티 지도 조회 실패 (tag=$tag, sort=$sort)", e)
                _uiState.update { current ->
                    current.copy(loading = false, errorMessage = LOAD_FAILED_MESSAGE)
                }
            }
        }
    }
}

/** 추천을 다 받았는지. 받는 중이거나 실패했으면 칩을 골라도 거를 것이 없어 다시 받는다. */
private fun recommendationsLoaded(state: CommunityMapListUiState): Boolean =
    !state.loading && state.errorMessage == null

/** 이 태그가 달린 지도만. null 이면 전부. */
private fun List<CommunityMap>.withTag(tag: String?): List<CommunityMap> =
    if (tag == null) this else filter { map -> tag in map.hashtags }

/** 지도들에 달린 태그를 많이 쓰인 순으로. 같은 수면 먼저 나온 태그가 앞이다. */
internal fun tagsByFrequency(
    maps: List<CommunityMap>,
    limit: Int = MAX_TAG_CHIPS,
): List<String> = maps
    .flatMap { map -> map.hashtags }
    .groupingBy { tag -> tag }
    .eachCount()
    .entries
    .sortedByDescending { entry -> entry.value }
    .take(limit)
    .map { entry -> entry.key }
