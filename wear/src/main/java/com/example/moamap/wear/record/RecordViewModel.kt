package com.example.moamap.wear.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moamap.core.walksession.WalkSessionPayload
import com.example.moamap.wear.health.ExerciseRecorder
import com.example.moamap.wear.transfer.PendingSessionStore
import com.example.moamap.wear.transfer.SessionSender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val recorder: ExerciseRecorder,
    private val sender: SessionSender,
    private val pendingStore: PendingSessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecordUiState>(RecordUiState.Idle)
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    private var startedAtEpochMillis: Long = 0
    private var clientSessionId: String = ""

    // 진행 중인 전송을 최대 1개로 유지한다. 이미 도는 전송이 있으면 새로 launch 하지 않고
    // 무시한다 - 취소하지 않는다, 이미 시작된 전송은 끝까지 간다.
    private var transferJob: Job? = null

    // start()도 동일한 정책의 single-flight 가드가 필요하다. load()가 suspend가 된 뒤로는
    // 빠른 연속 탭이 두 개의 동시 코루틴을 만들 수 있어, 이미 도는 시작 요청이 있으면
    // 새로 launch 하지 않고 무시한다 - 취소하지 않는다.
    private var startJob: Job? = null

    // 센서 배치가 배터리 절약을 위해 늦게 도착해도 화면 타이머는 벽시계 기준으로 계속 움직인다.
    private var tickerJob: Job? = null

    init {
        // 샘플·등록오류 구독은 ViewModel 생애주기 동안 딱 한 번만 연다. start()가 호출될 때마다
        // 새로 구독하면 이전 구독이 살아있는 채로 남아 샘플이 중복 집계된다. onSampleObserved는
        // Recording이 아닌 상태를 무시하고, registrationError는 기록 중이 아닐 때 null을
        // 유지하므로 idle 상태에서 계속 돌아도 안전하다.
        viewModelScope.launch {
            recorder.samples.collect { samples ->
                val latest = samples.lastOrNull() ?: return@collect
                _uiState.update { state ->
                    state.onSampleObserved(
                        elapsedMillis = System.currentTimeMillis() - startedAtEpochMillis,
                        heartRate = latest.hr,
                        sampleCount = samples.size,
                    )
                }
            }
        }

        // 센서 등록은 start() 반환 후에 비동기로 실패할 수 있다. 이 신호가 없으면
        // "기록 중" 화면이 도는데 샘플은 0개인 상태를 사용자가 알 수 없다.
        // 단, 등록 오류는 Recording 상태에서만 화면을 바꿔야 한다 - 이미 Finished나 Idle로
        // 넘어간 뒤에 늦게 도착한 오류가 그 화면을 덮어써서는 안 된다.
        viewModelScope.launch {
            recorder.registrationError.collect { throwable ->
                if (throwable == null) return@collect
                _uiState.update { state ->
                    if (state is RecordUiState.Recording) {
                        RecordUiState.PermissionDenied(
                            throwable.message ?: "센서를 시작하지 못했어요"
                        )
                    } else {
                        state
                    }
                }
            }
        }

        // 앱이 죽은 뒤 다시 켜졌을 때 못 보낸 세션이 있으면 종료 화면에서 재전송할 수 있게 한다.
        viewModelScope.launch {
            pendingStore.load()?.let { pending ->
                _uiState.value = RecordUiState.Finished(
                    sampleCount = pending.samples.size,
                    transferState = TransferState.FAILED,
                )
                startedAtEpochMillis = pending.startedAtEpochMillis
                clientSessionId = pending.clientSessionId
            }
        }
    }

    fun onPermissionDenied(message: String) {
        _uiState.value = RecordUiState.PermissionDenied(message)
    }

    /**
     * 전송을 마친 화면과 권한 거부 화면에서 대기 상태로 돌아온다.
     * 이 경로가 없으면 두 화면이 막다른 길이 되어 앱을 강제 종료해야만 새 기록을 시작할 수 있다.
     */
    fun reset() {
        val previous = _uiState.value
        if (_uiState.updateAndGet { it.onResetRequested() } !is RecordUiState.Idle) return

        // 기록 중 등록 오류로 넘어온 경우에는 화면만 Recording 을 벗어났을 뿐,
        // Health Services 쪽 세션은 열린 채로 남아 다음 start() 를 실패시킬 수 있다.
        // 티커도 그 경로에서는 취소된 적이 없으므로 여기서 함께 정리한다.
        if (previous is RecordUiState.PermissionDenied) {
            tickerJob?.cancel()
            viewModelScope.launch { recorder.stop() }
        }
    }

    /** 이미 진행 중인 시작 요청이 있으면 무시한다 - 취소하지 않고 그대로 끝까지 진행한다. */
    fun start() {
        if (startJob?.isActive == true) return
        startJob = viewModelScope.launch {
            // 아직 보내지 못한 세션이 있으면 새 기록을 시작하지 않는다. onStartRequested()는
            // Finished(FAILED)를 포함해 어떤 상태에서도 새 Recording으로 넘어가므로, 여기서 막지
            // 않으면 사용자가 재전송을 시도하기 전에 미전송 세션이 조용히 덮어써진다.
            if (pendingStore.load() != null) return@launch

            startedAtEpochMillis = System.currentTimeMillis()
            clientSessionId = UUID.randomUUID().toString()
            _uiState.update { it.onStartRequested() }
            startTicker()

            runCatching { recorder.start() }
                .onFailure { throwable ->
                    // 이 시작 시도가 만든 Recording 상태만 덮어쓴다. 그 사이 사용자가 이미
                    // 종료했거나(Finished) 다른 상태로 넘어갔다면 그 화면을 건드리지 않는다.
                    _uiState.update { state ->
                        if (state is RecordUiState.Recording) {
                            RecordUiState.PermissionDenied(
                                throwable.message ?: "세션을 시작하지 못했어요"
                            )
                        } else {
                            state
                        }
                    }
                }
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                _uiState.update { state ->
                    if (state is RecordUiState.Recording) {
                        state.copy(elapsedMillis = System.currentTimeMillis() - startedAtEpochMillis)
                    } else {
                        state
                    }
                }
            }
        }
    }

    fun stop() {
        tickerJob?.cancel()
        viewModelScope.launch {
            val samples = recorder.stop()
            val payload = WalkSessionPayload(
                clientSessionId = clientSessionId,
                startedAtEpochMillis = startedAtEpochMillis,
                endedAtEpochMillis = System.currentTimeMillis(),
                samples = samples,
            )
            // 보내기 전에 먼저 저장한다. 전송이 실패해도 기록은 남는다.
            pendingStore.save(payload)
            _uiState.update { it.onStopRequested() }
            launchTransfer(payload)
        }
    }

    fun retry() {
        viewModelScope.launch {
            val payload = pendingStore.load() ?: return@launch
            _uiState.update { it.onRetryRequested() }
            launchTransfer(payload)
        }
    }

    /** 이미 진행 중인 전송이 있으면 무시한다 - 취소하지 않고 그대로 끝까지 보낸다. */
    private fun launchTransfer(payload: WalkSessionPayload) {
        if (transferJob?.isActive == true) return
        transferJob = viewModelScope.launch { transfer(payload) }
    }

    private suspend fun transfer(payload: WalkSessionPayload) {
        val result = sender.send(payload)
        // 실제로 SUCCESS로 전이된 경우에만 pendingStore를 지운다. 상태 갱신이 종단 상태라 무시된
        // 경우(예: 이미 SUCCESS/FAILED로 결착된 뒤 늦게 도착한 결과)에는 저장소를 건드리지 않는다.
        val newState = _uiState.updateAndGet { it.onTransferResult(success = result.isSuccess) }
        if (newState is RecordUiState.Finished && newState.transferState == TransferState.SUCCESS) {
            pendingStore.clear()
        }
    }
}
