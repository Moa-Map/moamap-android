package com.example.moamap.feature.mapdetail

import android.util.Log
import com.example.moamap.feature.mapdetail.domain.model.MapPlace
import com.mapbox.common.location.LocationServiceFactory
import com.mapbox.geojson.Point
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val TAG = "MapDetailCamera"

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
 * 마지막으로 알려진 기기 위치. 못 얻으면 null 이다.
 *
 * 실시간 추적을 걸지 않는다. 초기 카메라를 한 번 정하는 게 전부라 구독을 열 이유가 없다.
 *
 * 권한이 없거나 위치 서비스가 꺼져 있으면 그냥 null 이 온다. 호출부는 그때 고정 좌표로
 * 넘어가면 된다.
 */
internal suspend fun lastKnownLocation(): Point? {
    val provider = try {
        LocationServiceFactory.getOrCreate()
            .getDeviceLocationProvider(null)
            .takeIf { expected -> expected.isValue }
            ?.value
    } catch (e: Exception) {
        Log.w(TAG, "위치 제공자를 얻지 못했다", e)
        null
    } ?: return null

    return suspendCancellableCoroutine { continuation ->
        val cancelable = try {
            provider.getLastLocation { location ->
                continuation.resume(
                    location?.let { found -> Point.fromLngLat(found.longitude, found.latitude) },
                )
            }
        } catch (e: SecurityException) {
            // 권한 없이 부르면 여기로 온다. 예외로 화면을 깨지 않고 폴백으로 보낸다.
            Log.w(TAG, "위치 권한이 없어 마지막 위치를 읽지 못했다", e)
            continuation.resume(null)
            null
        }

        continuation.invokeOnCancellation { cancelable?.cancel() }
    }
}
