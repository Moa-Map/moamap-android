package com.example.moamap.wear.health

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.concurrent.futures.await
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import com.example.moamap.core.walksession.WalkSample
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Health Services 로 걷기 세션을 열고 GPS·심박을 모은다.
 *
 * 이 클래스는 프레임워크 경계다 — 변환 규칙은 [toWalkSamples] 에 있고 여기서는 배선만 한다.
 * 세션은 화면이 꺼져도 유지되며, 배터리를 위해 배치로 전달된다.
 */
@Singleton
class ExerciseRecorder @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val exerciseClient = HealthServices.getClient(context).exerciseClient

    private val _samples = MutableStateFlow<List<WalkSample>>(emptyList())
    val samples: StateFlow<List<WalkSample>> = _samples.asStateFlow()

    private val _latestHeartRate = MutableStateFlow<Double?>(null)
    val latestHeartRate: StateFlow<Double?> = _latestHeartRate.asStateFlow()

    private val _registrationError = MutableStateFlow<Throwable?>(null)
    val registrationError: StateFlow<Throwable?> = _registrationError.asStateFlow()

    /**
     * 세션마다 새로 만드는 종료 신호. Health Services 는 종료 시 마지막 배치를 실은
     * ENDED 업데이트를 콜백으로 보내는데, [stop] 이 그걸 기다리려면 붙잡을 것이 필요하다.
     * 세션 밖에서는 null 이다.
     */
    @Volatile
    private var endedSignal: CompletableDeferred<Unit>? = null

    private val callback = object : ExerciseUpdateCallback {

        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            val bootEpochMillis = System.currentTimeMillis() - SystemClock.elapsedRealtime()

            val locations = update.latestMetrics
                .getData(DataType.LOCATION)
                .map { point ->
                    val value = point.value
                    RawLocationReading(
                        elapsedFromBootMillis = point.timeDurationFromBoot.toMillis(),
                        lat = value.latitude,
                        lng = value.longitude,
                        // 정확도는 이번 범위에서 쓰지 않는다. 백엔드 체류 감지에 필요해지면
                        // point.accuracy 를 LocationAccuracy 로 캐스팅해 채운다.
                        accuracyMeters = null,
                    )
                }

            val heartRates = update.latestMetrics
                .getData(DataType.HEART_RATE_BPM)
                .map { point ->
                    RawHeartRateReading(
                        elapsedFromBootMillis = point.timeDurationFromBoot.toMillis(),
                        bpm = point.value,
                    )
                }

            if (locations.isNotEmpty() || heartRates.isNotEmpty()) {
                val newSamples = toWalkSamples(bootEpochMillis, locations, heartRates)
                _samples.update { existing -> existing + newSamples }
                heartRates.lastOrNull()?.let { _latestHeartRate.value = it.bpm }
            }

            // 종료 신호는 샘플이 하나도 없는 업데이트로도 오므로 위 분기 밖에서 확인한다.
            if (update.exerciseStateInfo.state.isEnded) {
                endedSignal?.complete(Unit)
            }
        }

        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) = Unit

        /**
         * GPS 가 측위 중인지(ACQUIRING) 아예 불가인지(NO_GNSS/UNAVAILABLE) 구분할 유일한 신호다.
         * 지금은 로그로만 남긴다 — 화면 노출은 좌표 수집 개선에서 다룬다.
         */
        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {
            Log.i(TAG, "가용성 변경: ${dataType.name} -> $availability")
        }

        override fun onRegistered() = Unit

        override fun onRegistrationFailed(throwable: Throwable) {
            Log.e(TAG, "센서 등록 실패", throwable)
            _registrationError.value = throwable
        }
    }

    /** 새 세션을 연다. 이전 세션의 샘플은 버린다. */
    suspend fun start() {
        _samples.value = emptyList()
        _latestHeartRate.value = null
        _registrationError.value = null
        endedSignal = CompletableDeferred()

        exerciseClient.setUpdateCallback(callback)

        val config = ExerciseConfig(
            exerciseType = ExerciseType.WALKING,
            dataTypes = setOf(DataType.HEART_RATE_BPM, DataType.LOCATION),
            isAutoPauseAndResumeEnabled = false,
            isGpsEnabled = true,
        )

        // 센서를 열기 전에 프로세스를 포그라운드로 승격한다. 화면이 꺼진 뒤 얼어붙으면
        // 콜백 배달이 멈춰 세션이 초반 몇십 초짜리로 잘린다.
        ExerciseService.start(context)
        runCatching { exerciseClient.startExerciseAsync(config).await() }
            .onFailure { throwable ->
                // 세션이 열리지 않았는데 "기록 중" 알림만 남아 있으면 안 된다.
                ExerciseService.stop(context)
                endedSignal = null
                throw throwable
            }
    }

    /** 세션을 끝내고 모인 샘플을 시간순으로 돌려준다. */
    suspend fun stop(): List<WalkSample> {
        // 배터리를 아끼려 배치로 쌓아둔 것을 먼저 밀어낸다. 이걸 건너뛰면 마지막 배치가 통째로 사라진다.
        runCatching { exerciseClient.flushAsync().await() }
            .onFailure { Log.w(TAG, "샘플 플러시 실패 - 세션 끝부분이 빠질 수 있음", it) }

        runCatching { exerciseClient.endExerciseAsync().await() }
            .onFailure { Log.w(TAG, "운동 세션 종료 실패", it) }

        // endExerciseAsync 의 완료는 "종료 요청이 접수됐다"는 뜻이지 마지막 배치가 콜백에
        // 도착했다는 뜻이 아니다. ENDED 업데이트를 받기 전에 콜백을 해제하면 그 배치를 버리게 된다.
        val signal = endedSignal
        val confirmedEnded = signal != null &&
            withTimeoutOrNull(ENDED_TIMEOUT_MILLIS) { signal.await() } != null
        endedSignal = null

        if (!confirmedEnded) {
            Log.w(TAG, "종료 업데이트를 확인하지 못함 - 종료를 다시 요청한다")
            ensureExerciseTornDown()
        }

        runCatching { exerciseClient.clearUpdateCallbackAsync(callback).await() }
            .onFailure { Log.w(TAG, "업데이트 콜백 해제 실패 - 다음 세션에 영향을 줄 수 있음", it) }

        ExerciseService.stop(context)

        // 정리에 실패했더라도 사용자가 걸으며 모은 샘플은 그대로 돌려준다.
        // 세션 정리는 우리 사정이고, 그것 때문에 사용자가 걸은 기록을 잃게 만들지 않는다.
        return _samples.value.sortedBy { it.tsEpochMillis }
    }

    /**
     * ENDED 를 확인하지 못했을 때 종료를 한 번 더 요청한다.
     *
     * 종료가 접수되지 않은 채로 두면 세션이 Health Services 쪽에 살아남아 센서가 계속 돌고,
     * 다음 [start] 의 startExerciseAsync 가 "이미 진행 중"으로 막혀 사용자가 새 기록을
     * 영영 시작하지 못하는 상태가 된다. 재요청 한 번이면 일시적 IPC 실패는 대부분 걷힌다.
     *
     * 확인까지 기다리지는 않는다 — 종료를 누른 사용자를 몇 초 더 붙잡아 둘 만한 이득이 없다.
     * getCurrentExerciseInfoAsync 로 실제 상태를 묻는 방법도 있지만, 판정에 필요한
     * ExerciseTrackedStatus 상수가 라이브러리 내부 전용(@RestrictTo)이라 쓸 수 없다.
     */
    private suspend fun ensureExerciseTornDown() {
        runCatching { exerciseClient.endExerciseAsync().await() }
            .onFailure { Log.w(TAG, "종료 재요청 실패 - 세션이 남아 다음 기록이 막힐 수 있음", it) }
    }

    private companion object {
        private const val TAG = "ExerciseRecorder"

        /** 종료 업데이트를 기다리는 한도. 넘기면 지금까지 모인 것만 들고 나간다. */
        private const val ENDED_TIMEOUT_MILLIS = 5_000L
    }
}
