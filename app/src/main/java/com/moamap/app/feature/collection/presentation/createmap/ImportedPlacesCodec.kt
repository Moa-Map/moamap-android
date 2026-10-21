package com.moamap.app.feature.collection.presentation.createmap

import android.net.Uri
import com.moamap.app.feature.collection.domain.model.EditedPlace
import com.moamap.app.feature.collection.domain.model.ImportedPlace
import com.moamap.app.feature.collection.domain.model.PlaceEdit
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * 새 지도에 함께 넣을 장소를 문자열 하나로 바꾼다.
 *
 * 가져오기 흐름이 만들기 화면에 결과를 넘길 때와, 만들기 화면이 [androidx.lifecycle.SavedStateHandle]
 * 에 남길 때 함께 쓴다. 둘 다 Bundle 에 담겨야 하는데, 그러자고 도메인 모델에 Parcelable 을
 * 붙일 이유는 없다 - [CreateMapUiState] 를 항목별로 쪼개 두는 것과 같은 이유다.
 */
internal fun encodeImportedPlaces(places: List<EditedPlace>): String =
    ImportedPlacesJson.encodeToString(
        ListSerializer(SavedImportedPlace.serializer()),
        places.map(EditedPlace::toSaved),
    )

/** 형식이 맞지 않으면 빈 목록으로 본다. 저장한 뒤 앱이 바뀌었을 수 있다. */
internal fun decodeImportedPlaces(encoded: String): List<EditedPlace> =
    runCatching {
        ImportedPlacesJson.decodeFromString(
            ListSerializer(SavedImportedPlace.serializer()),
            encoded,
        )
    }.getOrDefault(emptyList()).map(SavedImportedPlace::toEditedPlace)

private val ImportedPlacesJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class SavedImportedPlace(
    val id: String,
    val name: String,
    val address: String? = null,
    val roadAddress: String? = null,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val category: String? = null,
    val description: String? = null,
    val kakaoPlaceId: String? = null,
    val sourceType: String = "",
    val sourceUrl: String? = null,
    val tags: List<String> = emptyList(),
    val memo: String = "",
    /** 아직 올리지 않은 사진의 `content://` URI. */
    val photos: List<String> = emptyList(),
)

private fun EditedPlace.toSaved() = SavedImportedPlace(
    id = place.id,
    name = place.name,
    address = place.address,
    roadAddress = place.roadAddress,
    lat = place.lat,
    lng = place.lng,
    category = place.category,
    description = place.description,
    kakaoPlaceId = place.kakaoPlaceId,
    sourceType = place.sourceType,
    sourceUrl = place.sourceUrl,
    tags = edit.tags,
    memo = edit.memo,
    photos = edit.photos.map(Uri::toString),
)

private fun SavedImportedPlace.toEditedPlace() = EditedPlace(
    place = ImportedPlace(
        id = id,
        name = name,
        address = address,
        roadAddress = roadAddress,
        lat = lat,
        lng = lng,
        category = category,
        description = description,
        kakaoPlaceId = kakaoPlaceId,
        sourceType = sourceType,
        sourceUrl = sourceUrl,
    ),
    edit = PlaceEdit(tags = tags, memo = memo, photos = photos.map(Uri::parse)),
)
