package com.example.moamap.feature.collection.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moamap.feature.collection.domain.model.MapType
import com.example.moamap.feature.collection.domain.repository.MapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CollectionViewModel"
private const val LOAD_FAILED_MESSAGE = "지도 목록을 불러오지 못했어요"

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val repository: MapRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionUiState())
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    /** 탭마다 진행 중인 요청. 같은 탭을 다시 부르면 이전 요청을 버린다. */
    private val loadJobs = mutableMapOf<MapType, Job>()

    /** 한 번이라도 요청한 탭. 이미 본 탭은 다시 들어와도 그대로 보여준다. */
    private val requestedTabs = mutableSetOf<MapType>()

    fun selectTab(type: MapType) {
        if (_uiState.value.selectedTab == type) return
        _uiState.update { state -> state.copy(selectedTab = type) }

        if (type !in requestedTabs) load(type)
    }

    fun retry() = load(_uiState.value.selectedTab)

    /**
     * 화면이 보일 때 현재 탭을 읽는다. 첫 조회도 이 경로가 겸한다.
     *
     * 지도를 만들고 돌아오는 경로가 여기다. 탭 전환이 상태를 복원하므로 이 ViewModel 은
     * 살아남고, 다시 읽지 않으면 방금 만든 지도가 목록에 나타나지 않는다.
     *
     * `init` 에서 첫 조회를 하지 않는 이유는, 화면이 처음 뜰 때 이 함수도 함께 불려
     * 같은 요청이 두 번 나가기 때문이다. 안 본 탭까지 미리 받지도 않는다.
     *
     * 보고 있지 않은 탭도 낡았을 수 있다. 프라이빗 지도를 만들고 커뮤니티 탭으로 돌아오는
     * 경우가 그렇다. 그 탭까지 지금 읽지는 않고, 다음에 고를 때 새로 읽도록 표시만 지운다.
     */
    fun refresh() {
        val current = _uiState.value.selectedTab
        requestedTabs.retainAll { tab -> tab == current }
        load(current, keepCurrent = true)
    }

    /**
     * @param keepCurrent true 면 보고 있던 목록을 지우지 않는다. 새로고침 때마다 목록이
     *  사라졌다 나타나면 화면이 깜빡인다.
     */
    private fun load(type: MapType, keepCurrent: Boolean = false) {
        requestedTabs += type
        loadJobs[type]?.cancel()

        if (!keepCurrent) {
            _uiState.update { state -> state.withState(type, MyMapsState.Loading) }
        }

        loadJobs[type] = viewModelScope.launch {
            try {
                val maps = repository.getMyMaps(type)
                _uiState.update { state ->
                    state.withState(type, MyMapsState.Success(maps))
                }
            } catch (e: CancellationException) {
                // 같은 탭의 다음 요청이 이미 시작됐다. 이 요청의 결과로 상태를 건드리면 안 된다.
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "내 지도 목록 조회 실패 (type=$type)", e)
                onLoadFailed(type, keepCurrent)
            }
        }
    }

    /** 새로고침이 실패했는데 이미 보여줄 목록이 있으면 지우지 않는다. */
    private fun onLoadFailed(type: MapType, keepCurrent: Boolean) {
        if (keepCurrent && _uiState.value.stateOf(type) is MyMapsState.Success) return

        _uiState.update { state ->
            state.withState(type, MyMapsState.Error(LOAD_FAILED_MESSAGE))
        }
    }
}
