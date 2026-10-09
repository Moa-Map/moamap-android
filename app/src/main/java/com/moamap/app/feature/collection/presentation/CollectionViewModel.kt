package com.moamap.app.feature.collection.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.network.ApiException
import com.moamap.app.core.network.ConnectionException
import com.moamap.app.feature.collection.domain.model.MapType
import com.moamap.app.feature.collection.domain.model.MyMap
import com.moamap.app.feature.collection.domain.repository.MapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CollectionViewModel"
private const val LOAD_FAILED_MESSAGE = "지도 목록을 불러오지 못했어요"
private const val INVALID_CODE_MESSAGE = "코드를 다시 확인해주세요"
private const val ALREADY_JOINED_MESSAGE = "이미 참여 중인 지도예요"
private const val JOIN_FAILED_MESSAGE = "지도에 참여하지 못했어요"
private const val NETWORK_ERROR_MESSAGE = "네트워크에 연결할 수 없어요"
internal const val OWNER_CANNOT_LEAVE_MESSAGE = "방장인 지도는 나갈 수 없어요"
internal const val PERSONAL_CANNOT_LEAVE_MESSAGE = "나만의 지도는 나갈 수 없어요"
internal const val LEAVE_CHECK_FAILED_MESSAGE = "지도 정보를 불러오지 못했어요"
internal const val ORDER_SAVE_FAILED_MESSAGE = "순서를 저장하지 못했어요"

internal fun leaveFailedMessage(count: Int): String = "${count}개 지도에서 나가지 못했어요"

/** `[404] MAP_007: 유효하지 않은 초대 코드입니다.` */
private const val INVALID_INVITE_CODE = "MAP_007"

/** `[409] MAP_005: 이미 참여한 지도입니다.` */
private const val ALREADY_JOINED = "MAP_005"

/**
 * 합류 실패 안내.
 *
 * 서버 메시지를 그대로 노출하지 않고 에러 코드로 가른다. 두 코드는 실제 응답으로 확인했고,
 * 그 밖의 경우는 원인을 단정하지 않는다.
 */
private fun Throwable.toJoinMessage(): String = when {
    this is ConnectionException -> NETWORK_ERROR_MESSAGE
    this !is ApiException -> JOIN_FAILED_MESSAGE
    code == INVALID_INVITE_CODE -> INVALID_CODE_MESSAGE
    code == ALREADY_JOINED -> ALREADY_JOINED_MESSAGE
    else -> JOIN_FAILED_MESSAGE
}

private fun Char.isAsciiAlphanumeric(): Boolean =
    this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9'

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

    /** 편집을 시작하며 지도마다 나갈 수 있는지 확인하는 요청. */
    private var editJob: Job? = null

    /** 편집을 마치며 바꾼 순서를 저장하는 요청. 목록을 다시 읽기 전에 끝나길 기다린다. */
    private var orderSaveJob: Job? = null

    fun selectTab(type: MapType) {
        if (_uiState.value.selectedTab == type) return
        // 나가는 중에는 탭을 옮기지 않는다. 끝나면 떠난 탭의 목록을 다시 읽어야 한다.
        if (_uiState.value.edit?.leaving == true) return
        // 편집은 한 탭의 목록을 두고 하는 일이다. 탭을 옮기면 끝낸다.
        finishEdit()
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
        // 돌아와 목록을 다시 읽으면 확인해 둔 지도와 목록이 어긋날 수 있다. 편집은 끝낸다.
        finishEdit()
        val current = _uiState.value.selectedTab
        requestedTabs.retainAll { tab -> tab == current }
        load(current, keepCurrent = true)
    }

    fun consumeNotice() {
        _uiState.update { state -> state.copy(notice = null) }
    }

    // ---------- 편집: 순서 바꾸기·골라서 나가기 ----------

    /**
     * 편집을 시작한다.
     *
     * 내 지도 목록에는 내 역할이 없어서, 지도마다 나갈 수 있는지 따로 확인한다. 확인이 끝난
     * 지도부터 고를 수 있다. 나만의 지도는 물어볼 것도 없이 나갈 대상이 아니다.
     */
    fun startEdit() {
        val state = _uiState.value
        if (state.edit != null) return
        val maps = (state.currentMaps as? MyMapsState.Success)?.maps ?: return

        val personal = maps.filter { map -> map.personal }
            .associate { map -> map.id to LeaveEligibility.Personal }
        _uiState.update { current ->
            current.copy(edit = CollectionEditState(initialOrder = maps.map { map -> map.id }, eligibility = personal))
        }

        editJob = viewModelScope.launch {
            maps.filterNot { map -> map.personal }.forEach { map ->
                launch {
                    val eligibility = checkEligibility(map.id)
                    _uiState.update { current ->
                        val edit = current.edit ?: return@update current
                        current.copy(
                            edit = edit.copy(eligibility = edit.eligibility + (map.id to eligibility)),
                        )
                    }
                }
            }
        }
    }

    /**
     * 편집을 끝낸다. 「완료」·뒤로가기·탭 전환·화면 복귀가 모두 여기로 온다.
     *
     * 순서를 바꿨으면 이때 한 번 저장한다. 못 하면 알리고 서버 순서로 되돌린다.
     */
    fun finishEdit() {
        val edit = _uiState.value.edit ?: return
        if (edit.leaving) return
        editJob?.cancel()
        val tab = _uiState.value.selectedTab
        val moved = movedMaps(edit)
        _uiState.update { state -> state.copy(edit = null) }

        if (moved != null) {
            orderSaveJob = viewModelScope.launch {
                if (!saveOrder(tab, moved, edit.initialOrder)) load(tab, keepCurrent = true)
            }
        }
    }

    /**
     * 편집 중 [mapId] 를 [targetId] 자리로 옮긴다. 손잡이를 끌어 이웃 카드를 넘을 때마다 불린다.
     *
     * 화면 안에서만 바꾸고, 저장은 편집을 끝낼 때 한 번 한다([finishEdit]).
     * 공식지도와 커뮤니티 지도는 서버가 순서를 따로 둬서 서로의 자리로는 옮기지 않는다.
     */
    fun moveMap(mapId: Long, targetId: Long) {
        val state = _uiState.value
        if (state.edit == null || state.edit.leaving || mapId == targetId) return
        val maps = (state.currentMaps as? MyMapsState.Success)?.maps ?: return
        val from = maps.indexOfFirst { map -> map.id == mapId }
        val to = maps.indexOfFirst { map -> map.id == targetId }
        if (from < 0 || to < 0) return
        if (maps[from].official != maps[to].official) return

        val moved = maps.toMutableList().apply { add(to, removeAt(from)) }
        _uiState.update { current -> current.withState(current.selectedTab, MyMapsState.Success(moved)) }
    }

    /** 고를 수 없는 지도를 누르면 이유를 알린다. 확인 중인 지도는 아무 일도 없다. */
    fun toggleSelection(mapId: Long) {
        val edit = _uiState.value.edit ?: return
        if (edit.leaving) return

        val notice = when (edit.eligibilityOf(mapId)) {
            LeaveEligibility.Checking -> return
            LeaveEligibility.Allowed -> null
            LeaveEligibility.Owner -> OWNER_CANNOT_LEAVE_MESSAGE
            LeaveEligibility.Personal -> PERSONAL_CANNOT_LEAVE_MESSAGE
            LeaveEligibility.Unknown -> LEAVE_CHECK_FAILED_MESSAGE
        }
        if (notice != null) {
            _uiState.update { state -> state.copy(notice = notice) }
            return
        }

        val selected = if (mapId in edit.selected) edit.selected - mapId else edit.selected + mapId
        _uiState.update { state -> state.copy(edit = edit.copy(selected = selected)) }
    }

    fun openLeaveConfirm() {
        val edit = _uiState.value.edit ?: return
        if (edit.leaving || edit.selected.isEmpty()) return
        _uiState.update { state -> state.copy(edit = edit.copy(confirmVisible = true)) }
    }

    fun closeLeaveConfirm() {
        val edit = _uiState.value.edit ?: return
        _uiState.update { state -> state.copy(edit = edit.copy(confirmVisible = false)) }
    }

    /**
     * 고른 지도에서 모두 나간다.
     *
     * 한 곳씩 나간다. 일부가 실패해도 나머지는 계속하고, 끝나면 편집을 닫고 목록을 다시 읽는다.
     * 실패한 지도는 목록에 그대로 남으니 몇 개인지만 알린다.
     *
     * 순서를 바꿨으면 나가기 전에 저장한다. 나간 지도가 빠져도 남은 지도의 순서는 그대로다.
     */
    fun leaveSelected() {
        val edit = _uiState.value.edit ?: return
        if (edit.leaving || edit.selected.isEmpty()) return
        val type = _uiState.value.selectedTab
        val moved = movedMaps(edit)

        _uiState.update { state ->
            state.copy(edit = edit.copy(confirmVisible = false, leaving = true))
        }
        editJob?.cancel()
        viewModelScope.launch {
            if (moved != null) saveOrder(type, moved, edit.initialOrder)
            val failed = edit.selected.count { mapId -> !tryLeave(mapId) }
            _uiState.update { state ->
                // 순서 저장 실패 안내가 떠 있으면 나가기 실패가 없는 한 지우지 않는다.
                state.copy(edit = null, notice = if (failed > 0) leaveFailedMessage(failed) else state.notice)
            }
            load(type, keepCurrent = true)
        }
    }

    /** 편집하며 순서를 바꿨으면 지금 탭 목록, 안 바꿨으면 null. */
    private fun movedMaps(edit: CollectionEditState): List<MyMap>? =
        (_uiState.value.currentMaps as? MyMapsState.Success)?.maps
            ?.takeIf { maps -> maps.map { map -> map.id } != edit.initialOrder }

    /**
     * 바꾼 순서를 저장한다. 실패하면 알리고 false 를 돌려준다.
     *
     * 서버는 종류별로 그 종류의 내 지도를 빠짐없이 받아야 한다. 커뮤니티 탭에 함께 보이는 공식지도는
     * 따로 보내고, 순서가 그대로인 종류는 보내지 않는다.
     */
    private suspend fun saveOrder(tab: MapType, maps: List<MyMap>, initialOrder: List<Long>): Boolean = try {
        maps.groupBy { map -> if (map.official) MapType.Official else tab }.forEach { (type, group) ->
            val ids = group.map { map -> map.id }
            if (ids != initialOrder.filter { id -> id in ids }) repository.updateMyMapOrder(type, ids)
        }
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "지도 순서 저장 실패 (tab=$tab)", e)
        _uiState.update { state -> state.copy(notice = ORDER_SAVE_FAILED_MESSAGE) }
        false
    }

    private suspend fun checkEligibility(mapId: Long): LeaveEligibility = try {
        if (repository.getLeaveOutcome(mapId) == null) LeaveEligibility.Owner else LeaveEligibility.Allowed
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "나가기 가능 여부 확인 실패 (mapId=$mapId)", e)
        LeaveEligibility.Unknown
    }

    private suspend fun tryLeave(mapId: Long): Boolean = try {
        repository.leaveMap(mapId)
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "지도 나가기 실패 (mapId=$mapId)", e)
        false
    }

    // ---------- 초대 코드로 합류 ----------

    fun openJoinDialog() {
        _uiState.update { state -> state.copy(join = JoinState.Editing()) }
    }

    fun closeJoinDialog() {
        _uiState.update { state -> state.copy(join = JoinState.Hidden) }
    }

    /**
     * 코드는 영문 대문자와 숫자만 남긴다. 화면의 `#` 은 장식이라 값에 넣지 않는다.
     *
     * `Char.isLetterOrDigit()` 은 한글도 문자로 보기 때문에 쓸 수 없다. 서버가 발급하는
     * 코드는 `VH4YXZ` 같은 ASCII 영숫자다.
     */
    fun updateInviteCode(input: String) {
        val editing = _uiState.value.join as? JoinState.Editing ?: return
        if (editing.submitting) return

        val normalized = input.filter { char -> char.isAsciiAlphanumeric() }.uppercase()
        _uiState.update { state ->
            state.copy(join = editing.copy(code = normalized, errorMessage = null))
        }
    }

    /**
     * 초대 코드로 합류한다.
     *
     * 성공하면 결과가 보이는 곳으로 데려간다 - 커뮤니티 탭에서 코드를 넣었어도
     * 프라이빗 탭으로 옮기고 그 목록을 다시 읽는다.
     */
    fun join() {
        val editing = _uiState.value.join as? JoinState.Editing ?: return
        if (!editing.canSubmit) return

        // 코루틴 시작을 기다리지 않고 여기서 잠근다.
        _uiState.update { state ->
            state.copy(join = editing.copy(submitting = true, errorMessage = null))
        }

        viewModelScope.launch {
            try {
                repository.joinByInviteCode(editing.code)
                // 탭을 옮기니 편집 중이었으면 끝낸다(바꾼 순서도 이때 저장된다). [selectTab] 과 같다.
                finishEdit()
                _uiState.update { state ->
                    state.copy(join = JoinState.Hidden, selectedTab = MapType.Private)
                }
                // 합류한 지도가 보이도록 프라이빗 목록을 다시 읽는다.
                requestedTabs -= MapType.Private
                load(MapType.Private)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "초대 코드 합류 실패", e)
                _uiState.update { state ->
                    state.copy(
                        join = editing.copy(submitting = false, errorMessage = e.toJoinMessage()),
                    )
                }
            }
        }
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
            // 바꾼 순서를 저장하는 중이면 기다린다. 먼저 읽으면 저장 전 순서로 되돌아간다.
            orderSaveJob?.join()
            try {
                val maps = if (type == MapType.Community) getCommunityTabMaps() else repository.getMyMaps(type)
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

    /**
     * 커뮤니티 탭 목록. 참여한 공식지도를 맨 위에 둔다.
     *
     * 서버 「내 지도」는 종류별로만 줘서 공식지도를 따로 받아 붙인다. 공식지도를 못 받아도
     * 커뮤니티 지도는 보여준다 - 곁가지 하나 때문에 탭이 통째로 실패하면 안 된다.
     *
     * 모음 탭에서만 합친다. 장소 가져오기의 지도 고르기에 공식지도가 섞이면 서버가 등록을 거절한다.
     */
    private suspend fun getCommunityTabMaps(): List<MyMap> = coroutineScope {
        val official = async {
            try {
                repository.getMyMaps(MapType.Official)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "참여한 공식지도 조회 실패", e)
                emptyList()
            }
        }
        val community = repository.getMyMaps(MapType.Community)
        official.await() + community
    }

    /** 새로고침이 실패했는데 이미 보여줄 목록이 있으면 지우지 않는다. */
    private fun onLoadFailed(type: MapType, keepCurrent: Boolean) {
        if (keepCurrent && _uiState.value.stateOf(type) is MyMapsState.Success) return

        _uiState.update { state ->
            state.withState(type, MyMapsState.Error(LOAD_FAILED_MESSAGE))
        }
    }
}
