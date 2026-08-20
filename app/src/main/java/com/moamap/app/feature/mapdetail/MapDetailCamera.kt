package com.moamap.app.feature.mapdetail

import android.util.Log
import com.moamap.app.feature.mapdetail.domain.model.MapPlace
import com.mapbox.common.location.DeviceLocationProvider
import com.mapbox.common.location.Location
import com.mapbox.common.location.LocationObserver
import com.mapbox.common.location.LocationServiceFactory
import com.mapbox.geojson.Point
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

private const val TAG = "MapDetailCamera"

/**
 * 현재 위치를 실측으로 기다리는 한도.
 *
 * 실내나 약신호에서는 첫 좌표가 영영 오지 않을 수도 있다. 그때 버튼이 계속 잠겨 있으면
 * 화면이 멈춘 것처럼 보인다. 끊고 안내 문구로 넘어간다.
 */
private const val LocationTimeoutMillis = 5_000L

/** 처음 지도를 열 때 카메라를 어디에 둘지. */
internal sealed interface InitialCamera {
    /** 여러 장소가 한 화면에 담기게 맞춘다. */
    data class Fit(val points: List<Point>) : InitialCamera

    /** 한 지점을 [MapDetailDefaultZoom] 으로 본다. */
    data class Center(val point: Point) : InitialCamera
}

/**
 * 초기 카메라를 정한다.
 *
 * 장소를 현재 위치보다 앞에 둔다. 위치를 앞에 두면 서울 지도를 부산에서 열었을 때
 * 마커가 한 개도 없는 빈 화면이 뜬다. 이 화면은 그 지도의 장소를 보러 들어온 자리다.
 *
 * 좌표가 (0, 0) 인 장소는 세지 않는다. `PlaceDto.lat/lng` 의 기본값이라 좌표가 비어 온
 * 장소가 그 값을 달고 온다. 그대로 담으면 기니만 앞바다까지 화면에 넣으려 든다.
 */
internal fun initialCamera(
    places: List<MapPlace>,
    deviceLocation: Point?,
): InitialCamera {
    val points = places
        .filter { place -> place.latitude != 0.0 || place.longitude != 0.0 }
        .map { place -> Point.fromLngLat(place.longitude, place.latitude) }

    return when {
        points.size >= 2 -> InitialCamera.Fit(points)
        points.size == 1 -> InitialCamera.Center(points.first())
        deviceLocation != null -> InitialCamera.Center(deviceLocation)
        else -> InitialCamera.Center(MapDetailCenter)
    }
}

/**
 * "내 위치로 가기" 로 옮겨 갈 때의 줌.
 *
 * 이미 기본 줌보다 확대해 본 사람의 줌은 뺏지 않는다. 반대로 전국이 보이도록 축소해 둔
 * 상태에서는 끌어당긴다 - 그렇지 않으면 카메라만 움직이고 내 위치로 왔다는 느낌이 나지 않는다.
 *
 * 카메라가 아직 붙지 않았으면 [currentZoom] 이 null 이다. 그때는 기본 줌으로 간다.
 */
internal fun myLocationZoom(currentZoom: Double?): Double =
    maxOf(currentZoom ?: MapDetailDefaultZoom, MapDetailDefaultZoom)

/**
 * 마지막으로 알려진 기기 위치. 못 얻으면 null 이다.
 *
 * 실시간 추적을 걸지 않는다. 초기 카메라를 한 번 정하는 게 전부라 구독을 열 이유가 없다.
 *
 * 권한이 없거나 위치 서비스가 꺼져 있으면 그냥 null 이 온다. 호출부는 그때 고정 좌표로
 * 넘어가면 된다.
 */
internal suspend fun lastKnownLocation(): Point? =
    deviceLocationProvider()?.cachedLocation()

/**
 * 지금 위치. 못 얻으면 null 이다.
 *
 * 캐시를 먼저 본다. 대부분은 값이 들어 있어 버튼을 누른 즉시 카메라가 움직인다. 비어 있는
 * 기기(부팅 직후 등)에서만 1회성 구독을 열어 첫 좌표를 기다린다 - 그 비용을 매번 치를 이유가 없다.
 *
 * 캐시 읽기까지 포함해 [LocationTimeoutMillis] 안에 끝낸다.
 */
internal suspend fun currentLocation(): Point? {
    val provider = deviceLocationProvider() ?: return null

    return withTimeoutOrNull(LocationTimeoutMillis) {
        provider.cachedLocation() ?: provider.firstFreshLocation()
    }
}

private fun deviceLocationProvider(): DeviceLocationProvider? = try {
    LocationServiceFactory.getOrCreate()
        .getDeviceLocationProvider(null)
        .takeIf { expected -> expected.isValue }
        ?.value
} catch (e: Exception) {
    Log.w(TAG, "위치 제공자를 얻지 못했다", e)
    null
}

/** 하드웨어를 깨우지 않고 캐시된 좌표만 읽는다. 비어 있으면 null. */
private suspend fun DeviceLocationProvider.cachedLocation(): Point? =
    suspendCancellableCoroutine { continuation ->
        val cancelable = try {
            getLastLocation { location -> continuation.resume(location?.toPoint()) }
        } catch (e: SecurityException) {
            // 권한 없이 부르면 여기로 온다. 예외로 화면을 깨지 않고 폴백으로 보낸다.
            Log.w(TAG, "위치 권한이 없어 마지막 위치를 읽지 못했다", e)
            continuation.resume(null)
            null
        }

        continuation.invokeOnCancellation { cancelable?.cancel() }
    }

/**
 * 구독을 열어 첫 좌표 하나만 받고 바로 닫는다.
 *
 * 옵저버를 떼는 자리가 셋이다 - 좌표를 받았을 때, 등록이 실패했을 때, 그리고 바깥에서
 * 취소됐을 때(타임아웃 포함). 하나라도 빠지면 화면을 떠난 뒤에도 구독이 살아 새는 자리가 된다.
 */
private suspend fun DeviceLocationProvider.firstFreshLocation(): Point? =
    suspendCancellableCoroutine { continuation ->
        val done = AtomicBoolean(false)

        val observer = object : LocationObserver {
            override fun onLocationUpdateReceived(locations: List<Location>) {
                // 좌표 없는 묶음이 올 수 있다. 그걸 답으로 삼으면 기다릴 이유가 없어진다.
                // 묶음은 오래된 것부터라 마지막이 가장 새 좌표다.
                val point = locations.lastOrNull()?.toPoint() ?: return
                if (!done.compareAndSet(false, true)) return

                // 먼저 답하고 뗀다. 콜백 안에서 자기를 떼는 게 구현에 따라 던질 수 있는데,
                // 그때 답이 아직 안 나가 있으면 타임아웃까지 통째로 매달린다.
                continuation.resume(point)
                runCatching { removeLocationObserver(this) }
            }
        }

        try {
            addLocationObserver(observer)
        } catch (e: SecurityException) {
            Log.w(TAG, "위치 권한이 없어 위치를 구독하지 못했다", e)
            if (done.compareAndSet(false, true)) continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        // 타임아웃이든 화면 이탈이든, 끊길 때는 무조건 뗀다. 등록돼 있지 않으면 no-op 이다.
        continuation.invokeOnCancellation { runCatching { removeLocationObserver(observer) } }
    }

private fun Location.toPoint(): Point = Point.fromLngLat(longitude, latitude)
