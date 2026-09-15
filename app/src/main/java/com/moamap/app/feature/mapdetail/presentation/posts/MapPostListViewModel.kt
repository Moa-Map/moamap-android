package com.moamap.app.feature.mapdetail.presentation.posts

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.navigation.MoaMapRoute
import com.moamap.app.feature.mapdetail.domain.model.MapPost
import com.moamap.app.feature.mapdetail.domain.model.MapPostSort
import com.moamap.app.feature.mapdetail.domain.repository.MapPostRepository
import com.moamap.app.feature.mapdetail.presentation.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MapPostListViewModel"

internal const val POST_LOAD_FAILED_MESSAGE = "게시물을 불러오지 못했어요"

/**
 * 로그 탭 게시물 목록 상태.
 *
 * 첫 페이지 실패([errorMessage])와 다음 페이지 실패([loadMoreFailed])를 나눈다. 첫 페이지를
 * 못 읽으면 목록 자리 전체가 안내로 바뀌어야 하고, 다음 페이지를 못 읽으면 이미 받은 카드는
 * 그대로 두고 끝에만 다시 시도를 붙여야 한다.
 *
 * 조회를 시작하기 전에도 [loading] 이 참이다. 탭을 여는 순간 "없음" 이 한 프레임 스쳐 가지 않게 한다.
 */
@Immutable
data class MapPostListUiState(
    val sort: MapPostSort = MapPostSort.Latest,
    val loading: Boolean = true,
    val posts: List<MapPost> = emptyList(),
    val errorMessage: String? = null,
    val loadingMore: Boolean = false,
    val loadMoreFailed: Boolean = false,
    /** 마지막 페이지까지 받았다. */
    val endReached: Boolean = false,
)

/**
 * 지도 로그 탭의 게시물 목록.
 *
 * 지도 상태([com.moamap.app.feature.mapdetail.presentation.MapDetailViewModel])에 얹지 않는다.
 * 로그 탭을 열 때만 필요한 값이라, 섞으면 장소 탭만 보는 사용자도 게시물을 함께 받게 된다.
 */
@HiltViewModel
class MapPostListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MapPostRepository,
) : ViewModel() {

    private val mapId: Long = checkNotNull(savedStateHandle[MoaMapRoute.MapDetail.ARG_MAP_ID]) {
        "지도 상세는 ${MoaMapRoute.MapDetail.ARG_MAP_ID} 없이 열 수 없다"
    }

    private val _uiState = MutableStateFlow(MapPostListUiState())
    val uiState: StateFlow<MapPostListUiState> = _uiState.asStateFlow()

    /**
     * 진행 중인 조회. 첫 페이지와 다음 페이지가 같은 자리를 쓴다.
     *
     * 정렬을 바꾸면 이전 정렬의 다음 페이지가 늦게 도착해 새 목록 뒤에 붙을 수 있다. 한 자리에
     * 두고 새 조회가 이전 것을 취소하게 해 막는다.
     */
    private var loadJob: Job? = null

    /** 다음에 받을 페이지 번호. */
    private var nextPage = 0

    /** 한 번이라도 읽기 시작했는가. 탭을 오갈 때마다 다시 받지 않으려고 본다. */
    private var started = false

    /**
     * 로그 탭이 처음 열렸다.
     *
     * `init` 에서 읽지 않는다. 이 ViewModel 은 지도 상세와 함께 만들어져서, 거기서 시작하면
     * 장소 탭만 보고 나가는 사용자도 게시물을 받게 된다.
     */
    fun loadOnce() {
        if (started) return
        started = true
        loadFirstPage()
    }

    fun retry() = loadFirstPage()

    /** 같은 정렬을 다시 누르면 아무것도 하지 않는다. 받아 둔 목록을 버리고 다시 받을 이유가 없다. */
    fun selectSort(sort: MapPostSort) {
        if (sort == _uiState.value.sort) return
        _uiState.update { state -> state.copy(sort = sort) }
        started = true
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
        val sort = state.sort
        val page = nextPage

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val result = repository.getPosts(mapId, page, sort)
                nextPage = page + 1
                _uiState.update { current ->
                    // 받는 사이 새 글이 올라오면 페이지 경계가 밀려 앞 페이지의 글이 한 번 더 온다.
                    val known = current.posts.mapTo(HashSet()) { post -> post.id }
                    current.copy(
                        posts = current.posts + result.posts.filterNot { post -> post.id in known },
                        loadingMore = false,
                        endReached = result.isLast,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "게시물 다음 페이지 조회 실패 (mapId=$mapId, page=$page)", e)
                _uiState.update { current -> current.copy(loadingMore = false, loadMoreFailed = true) }
            }
        }
    }

    private fun loadFirstPage() {
        val sort = _uiState.value.sort
        _uiState.value = MapPostListUiState(sort = sort)
        nextPage = 0

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val result = repository.getPosts(mapId, 0, sort)
                nextPage = 1
                _uiState.value = MapPostListUiState(
                    sort = sort,
                    loading = false,
                    posts = result.posts,
                    endReached = result.isLast,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "게시물 조회 실패 (mapId=$mapId)", e)
                _uiState.value = MapPostListUiState(
                    sort = sort,
                    loading = false,
                    errorMessage = e.toUserMessage(POST_LOAD_FAILED_MESSAGE),
                )
            }
        }
    }
}
