package com.moamap.app.feature.explore.presentation

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.feature.explore.domain.model.CommunityMap
import com.moamap.app.feature.explore.domain.model.CommunityMapPage
import com.moamap.app.feature.explore.domain.model.CommunityMapSort
import com.moamap.app.feature.explore.domain.repository.CommunityMapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

/** 서버가 받는 검색어 길이. 넘으면 400 이라 입력에서 자른다. */
internal const val SEARCH_KEYWORD_MAX_LENGTH = 50

/** 입력을 멈추고 이만큼 기다렸다 찾는다. 장소 추가 검색과 같다. */
internal const val SEARCH_DEBOUNCE_MILLIS = 300L

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
    /** 처음에는 인기순(10-06 사용자 결정). */
    val sort: CommunityMapSort = CommunityMapSort.POPULAR,
    val loading: Boolean = true,
    val maps: List<CommunityMap> = emptyList(),
    val errorMessage: String? = null,
    val loadingMore: Boolean = false,
    val loadMoreFailed: Boolean = false,
    /** 마지막 페이지까지 받았다. */
    val endReached: Boolean = false,
    /** 검색창에 적힌 글자. */
    val query: String = "",
    /**
     * 지금 목록이 검색 결과면 그 검색어, 태그·정렬 목록이면 null.
     *
     * 검색 중에는 칩·정렬을 숨기고 전체에서 찾는다(10-10 사용자 결정). [selectedTag]·[sort] 는 그대로 두어,
     * 검색어를 지우면 그 조건의 목록으로 돌아간다.
     */
    val searchKeyword: String? = null,
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

    /** 입력을 멈추길 기다리는 검색. 글자가 바뀌면 버리고 다시 기다린다. */
    private var searchJob: Job? = null

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
        val loadedPages = nextPage
        _uiState.update { current -> current.copy(loadingMore = false) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val result = fetch(state, page = 0, size = loadedPages * COMMUNITY_MAP_PAGE_SIZE)
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

    /** 같은 칩을 다시 누르면 아무것도 하지 않는다. */
    fun selectTag(tag: String?) {
        if (tag == _uiState.value.selectedTag) return
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
                val result = fetch(state, page = page, size = COMMUNITY_MAP_PAGE_SIZE)
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
     * 검색창 글자가 바뀌었다. 입력을 멈추고 [SEARCH_DEBOUNCE_MILLIS] 뒤에 찾는다.
     *
     * 다 지우면 기다리지 않고 검색 전 태그·정렬 목록으로 돌아간다.
     */
    fun updateQuery(text: String) {
        val query = text.take(SEARCH_KEYWORD_MAX_LENGTH)
        _uiState.update { state -> state.copy(query = query) }
        searchJob?.cancel()

        val keyword = query.trim()
        if (keyword.isEmpty()) {
            applySearchKeyword(null)
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            applySearchKeyword(keyword)
        }
    }

    /** 키보드 검색 버튼. 기다리지 않고 바로 찾는다. */
    fun searchNow() {
        searchJob?.cancel()
        val keyword = _uiState.value.query.trim()
        if (keyword.isNotEmpty()) applySearchKeyword(keyword)
    }

    /** 같은 조건이면 다시 읽지 않는다. 자동 검색 뒤 검색 버튼을 눌러도 요청이 한 번이다. */
    private fun applySearchKeyword(keyword: String?) {
        if (keyword == _uiState.value.searchKeyword) return
        _uiState.update { state -> state.copy(searchKeyword = keyword) }
        loadFirstPage()
    }

    /**
     * 검색어가 있으면 검색, 없으면 태그·정렬 목록.
     *
     * 목록은 참여한 지도도 함께 받는다(10-10 사용자 결정). 홈의 커뮤니티 지도 3장은 그대로 참여한 지도를 뺀다.
     */
    private suspend fun fetch(state: CommunityMapListUiState, page: Int, size: Int): CommunityMapPage {
        val keyword = state.searchKeyword
        return if (keyword != null) {
            repository.searchMaps(keyword = keyword, page = page, size = size)
        } else {
            repository.getAllCommunityMaps(tag = state.selectedTag, sort = state.sort, page = page, size = size)
        }
    }

    private fun loadFirstPage() {
        val state = _uiState.value
        val tag = state.selectedTag
        val sort = state.sort
        val searching = state.searchKeyword != null
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
                val result = fetch(state, page = 0, size = COMMUNITY_MAP_PAGE_SIZE)
                nextPage = 1
                _uiState.update { current ->
                    current.copy(
                        loading = false,
                        maps = result.maps,
                        endReached = result.isLast,
                        // 칩은 거르지 않은 목록에서만 모은다. 검색 결과로 만들면 검색어에 걸린 태그만 남는다.
                        tags = if (current.tags.isEmpty() && tag == null && !searching) {
                            tagsByFrequency(result.maps)
                        } else {
                            current.tags
                        },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 검색어는 남기지 않는다. 사용자가 적은 글이라 로그가 수집·보관되는 경로를 타면 안 된다.
                Log.w(TAG, "커뮤니티 지도 조회 실패 (search=$searching, tag=$tag, sort=$sort)", e)
                _uiState.update { current ->
                    current.copy(loading = false, errorMessage = LOAD_FAILED_MESSAGE)
                }
            }
        }
    }
}

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
