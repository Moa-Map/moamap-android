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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

            if (locations.isEmpty() && heartRates.isEmpty()) return

            val newSamples = toWalkSamples(bootEpochMillis, locations, heartRates)
            _samples.update { existing -> existing + newSamples }
            heartRates.lastOrNull()?.let { _latestHeartRate.value = it.bpm }
        }

        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) = Unit

        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) = Unit

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

        exerciseClient.setUpdateCallback(callback)

        val config = ExerciseConfig(
            exerciseType = ExerciseType.WALKING,
            dataTypes = setOf(DataType.HEART_RATE_BPM, DataType.LOCATION),
            isAutoPauseAndResumeEnabled = false,
            isGpsEnabled = true,
        )
        exerciseClient.startExerciseAsync(config).await()
    }

    /** 세션을 끝내고 모인 샘플을 시간순으로 돌려준다. */
    suspend fun stop(): List<WalkSample> {
        runCatching { exerciseClient.endExerciseAsync().await() }
            .onFailure { Log.w(TAG, "운동 세션 종료 실패", it) }
        runCatching { exerciseClient.clearUpdateCallbackAsync(callback).await() }
            .onFailure { Log.w(TAG, "업데이트 콜백 해제 실패 - 다음 세션에 영향을 줄 수 있음", it) }
        return _samples.value.sortedBy { it.tsEpochMillis }
    }

    private companion object {
        private const val TAG = "ExerciseRecorder"
    }
}
