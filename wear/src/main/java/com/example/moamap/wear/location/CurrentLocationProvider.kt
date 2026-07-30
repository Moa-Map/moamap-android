package com.example.moamap.wear.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import com.example.moamap.core.walksession.WalkSample
import com.example.moamap.wear.health.ExerciseRecorder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** 이 시간 안에 들어온 기록 중 좌표는 "지금 위치"로 인정한다. */
private const val FRESH_SAMPLE_MAX_AGE_MILLIS = 30_000L

/** 측위를 기다리는 한도. 넘기면 포기하고 사용자에게 알린다. */
private const val FIX_TIMEOUT_MILLIS = 15_000L

/**
 * 기록 중이면 이미 들어온 좌표를 쓰고, 아니면 그 자리에서 한 번 측위한다.
 *
 * 기록 중에는 Health Services 가 이미 GPS 를 돌리고 있다. 거기서 나온 좌표를 두고
 * 따로 측위하면 사용자를 몇 초 더 기다리게 만들 뿐이고 배터리도 두 배로 쓴다.
 */
@Singleton
class CurrentLocationProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val recorder: ExerciseRecorder,
) {

    // getCurrentLocation 의 콜백을 받을 곳. 메인 스레드를 붙잡지 않는다.
    private val executor = Executors.newSingleThreadExecutor()

    /** 좌표를 얻지 못하면 null. 부르는 쪽이 전송을 포기한다. */
    suspend fun current(): WalkSample? {
        latestFreshLocation(
            samples = recorder.samples.value,
            nowEpochMillis = System.currentTimeMillis(),
            maxAgeMillis = FRESH_SAMPLE_MAX_AGE_MILLIS,
        )?.let { return it }

        if (!hasPermission()) return null

        val location = withTimeoutOrNull(FIX_TIMEOUT_MILLIS) { requestSingleFix() } ?: return null

        return WalkSample(
            tsEpochMillis = System.currentTimeMillis(),
            lat = location.latitude,
            lng = location.longitude,
            accuracyMeters = location.accuracy.toDouble(),
        )
    }

    /**
     * 부르는 쪽이 안내 문구를 고르는 데 쓴다.
     *
     * 권한이 없는 것과 측위에 실패한 것은 사용자가 할 수 있는 조치가 다르다. 둘 다
     * "위치를 못 찾았어요" 로 뭉뚱그리면 권한을 켜면 되는 사람이 계속 다시 누르게 된다.
     */
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private suspend fun requestSingleFix(): Location? = suspendCancellableCoroutine { continuation ->
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val provider = manager?.let { pickProvider(it) }
        if (manager == null || provider == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val signal = CancellationSignal()
        continuation.invokeOnCancellation { signal.cancel() }

        try {
            manager.getCurrentLocation(provider, signal, executor) { location ->
                if (continuation.isActive) continuation.resume(location)
            }
        } catch (e: SecurityException) {
            // 권한이 도중에 회수된 경우. 좌표가 없다는 사실만 알리면 된다.
            if (continuation.isActive) continuation.resume(null)
        }
    }

    /**
     * FUSED 가 있으면 그쪽이 실내에서도 잘 잡는다. 없는 기기에서는 GPS 로 내려간다.
     * (`LocationManager.FUSED_PROVIDER` 는 API 31 부터라 문자열 상수로 조회한다.)
     */
    private fun pickProvider(manager: LocationManager): String? {
        val candidates = listOf(
            "fused",
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
        )
        return candidates.firstOrNull { it in manager.allProviders }
    }
}

/**
 * 기록 중 들어온 샘플 중 "지금" 이라 부를 만한 마지막 좌표.
 *
 * 위치와 심박은 별개의 행으로 오므로 마지막 샘플이 아니라 마지막 **위치** 샘플을 찾는다.
 * 미래 타임스탬프는 버린다 - 부팅 시각 보정이 어긋나면 만들어질 수 있고, 그런 값을
 * 현재 위치라고 보내면 안 된다.
 */
internal fun latestFreshLocation(
    samples: List<WalkSample>,
    nowEpochMillis: Long,
    maxAgeMillis: Long,
): WalkSample? = samples
    .lastOrNull { sample -> sample.lat != null && sample.lng != null }
    ?.takeIf { sample -> nowEpochMillis - sample.tsEpochMillis in 0..maxAgeMillis }
