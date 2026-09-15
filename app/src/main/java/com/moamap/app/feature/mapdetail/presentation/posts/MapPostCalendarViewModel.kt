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
import java.util.TimeZone
import javax.inject.Inject

private const val TAG = "MapPostCalendarViewModel"

/**
 * 한 번에 이어 받는 페이지 수의 상한. 서버가 `last` 를 잘못 내려도 끝없이 받지 않게 한다.
 *
 * 한 페이지가 20개라 1000개까지다. 한 지도에 게시물이 그보다 많아지면 오래된 달이 덜 차 보일 수
 * 있는데, 그때는 서버에 기간 조회를 요청하는 편이 맞다.
 */
private const val MAX_CALENDAR_PAGES = 50

/**
 * 로그 탭 달력 형식 상태.
 *
 * [posts] 는 지금까지 받은 게시물 전부이고 최신순이다. 달마다 따로 들고 있지 않는다 - 서버에 월별
 * 조회가 없어 최신 글부터 이어 받으니, 앞 달을 보려면 어차피 뒤 달 글을 모두 받게 된다.
 */
@Immutable
data class MapPostCalendarUiState(
    val month: CalendarMonth? = null,
    val selectedDay: CalendarDay? = null,
    val posts: List<MapPost> = emptyList(),
    val loading: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * 지도 로그 탭의 달력 형식.
 *
 * 카드 형식([MapPostListViewModel])과 목록을 나눠 갖지 않는다. 카드 형식은 정렬을 바꿀 수 있고
 * 한 페이지씩 받지만, 달력은 늘 최신순이고 달의 시작까지 이어 받아야 해서 받는 방식이 다르다.
 */
@HiltViewModel
class MapPostCalendarViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MapPostRepository,
) : ViewModel() {

    private val mapId: Long = checkNotNull(savedStateHandle[MoaMapRoute.MapDetail.ARG_MAP_ID]) {
        "지도 상세는 ${MoaMapRoute.MapDetail.ARG_MAP_ID} 없이 열 수 없다"
    }

    private val _uiState = MutableStateFlow(MapPostCalendarUiState())
    val uiState: StateFlow<MapPostCalendarUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var nextPage = 0
    private var endReached = false
    private var started = false

    /**
     * 달력 형식을 처음 열었다. [today] 가 있는 달을 보여주고 그날을 고른다.
     *
     * 오늘을 밖에서 받는다. 안에서 시계를 읽으면 날짜가 걸린 동작을 테스트로 고정할 수 없다.
     */
    fun open(today: CalendarDay) {
        if (started) return
        started = true
        _uiState.update { state -> state.copy(month = today.calendarMonth, selectedDay = today) }
        loadUntilCovered()
    }

    fun showPreviousMonth() {
        val month = _uiState.value.month ?: return
        changeMonth(month.previous())
    }

    fun showNextMonth() {
        val month = _uiState.value.month ?: return
        changeMonth(month.next())
    }

    fun selectDay(day: CalendarDay) {
        _uiState.update { state -> state.copy(selectedDay = day) }
    }

    fun retry() = loadUntilCovered()

    /**
     * 게시물이 새로 올라왔다. 받아 둔 것을 버리고 보고 있던 달부터 다시 받는다.
     *
     * 새 글은 목록 맨 앞에 붙어야 하는데, 이어 받던 페이지 번호가 한 칸씩 밀려 끝에서 같은 글이
     * 한 번 더 온다. 처음부터 다시 받는 편이 단순하다. 아직 달력을 열지 않았으면 할 일이 없다.
     */
    fun refresh() {
        if (!started) return
        loadJob?.cancel()
        nextPage = 0
        endReached = false
        _uiState.update { state -> state.copy(posts = emptyList(), errorMessage = null) }
        loadUntilCovered()
    }

    /** 달을 넘기면 그 달 1일을 고른다. 이전 달의 날짜를 들고 가면 아래 목록이 다른 달의 글을 보여준다. */
    private fun changeMonth(month: CalendarMonth) {
        _uiState.update { state -> state.copy(month = month, selectedDay = month.day(1)) }
        loadUntilCovered()
    }

    /**
     * 보고 있는 달의 1일보다 오래된 글이 나올 때까지 이어 받는다.
     *
     * 이미 그만큼 받아 뒀으면 부르지 않는다. 앞 달을 보다가 뒤 달로 돌아오면 조회 없이 바로 그린다.
     */
    private fun loadUntilCovered() {
        val month = _uiState.value.month ?: return
        if (isCovered(month)) {
            _uiState.update { state -> state.copy(loading = false, errorMessage = null) }
            return
        }

        loadJob?.cancel()
        _uiState.update { state -> state.copy(loading = true, errorMessage = null) }
        loadJob = viewModelScope.launch {
            try {
                var pages = 0
                while (!isCovered(month) && pages < MAX_CALENDAR_PAGES) {
                    val page = repository.getPosts(mapId, nextPage, MapPostSort.Latest)
                    nextPage++
                    pages++
                    endReached = page.isLast
                    _uiState.update { state ->
                        // 받는 사이 새 글이 올라오면 페이지 경계가 밀려 앞 페이지의 글이 한 번 더 온다.
                        val known = state.posts.mapTo(HashSet()) { post -> post.id }
                        state.copy(posts = state.posts + page.posts.filterNot { post -> post.id in known })
                    }
                }
                _uiState.update { state -> state.copy(loading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "달력 게시물 조회 실패 (mapId=$mapId)", e)
                _uiState.update { state ->
                    state.copy(loading = false, errorMessage = e.toUserMessage(POST_LOAD_FAILED_MESSAGE))
                }
            }
        }
    }

    /**
     * 이 달 게시물을 다 받았는가.
     *
     * 가장 오래된 글이 이 달 1일보다 이르면 그 뒤로는 모두 이전 달이다. 시각을 모르는 글은 건너뛰고
     * 그 앞에서 시각이 있는 가장 오래된 글로 판단한다.
     */
    private fun isCovered(month: CalendarMonth): Boolean {
        if (endReached) return true
        val oldestMillis = _uiState.value.posts.lastOrNull { post -> post.createdAtMillis != null }
            ?.createdAtMillis
            ?: return false
        return oldestMillis < month.startMillis(TimeZone.getDefault())
    }
}
