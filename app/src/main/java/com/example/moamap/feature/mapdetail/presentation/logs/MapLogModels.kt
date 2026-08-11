package com.example.moamap.feature.mapdetail.presentation.logs

import androidx.compose.runtime.Immutable
import com.example.moamap.feature.mapdetail.domain.model.MapActivity
import com.example.moamap.feature.mapdetail.domain.model.MapActivityType
import com.example.moamap.feature.mapdetail.relativeTimeLabel

/** 타임라인 점 색으로 구분되는 로그 종류. */
internal enum class MapLogType {
    PlaceAdded,
    PlaceRemoved,

    /** 후기 작성. 서버가 프라이빗 지도에만 내려준다. */
    ReviewCreated,
}

/**
 * 활동 내역 한 건.
 *
 * 화면에 그릴 형태 그대로 담는다. 시각도 이미 "2시간 전" 이다 - 여기서 시각을 들고 있으면
 * 포맷 코드가 화면으로 들어온다.
 */
@Immutable
internal data class MapLogUiModel(
    /**
     * 목록 키.
     *
     * 서버 응답에 로그 식별자가 없어 만들어 쓴다. 같은 사람이 같은 초에 같은 장소로 두 건을
     * 남기면 나머지 값이 전부 겹치므로 순번을 섞는다 - 키가 겹치면 `LazyColumn` 이 터진다.
     */
    val id: String,
    val type: MapLogType,
    val userName: String,
    val userImageUrl: String?,
    val message: String,
    val timeAgo: String,
)

/**
 * 일반 참여자가 올린 장소 등록 요청.
 *
 * 공개 지도의 방장·관리자에게만 보인다.
 */
@Immutable
internal data class PendingRequestUiModel(
    val id: Long,
    val userName: String,
    val userImageUrl: String?,
    val message: String,
    val timeAgo: String,
)

/** 이름을 못 얻은 사용자. 이름 자리가 빈 줄로 보이지 않게 채운다. */
private const val UNKNOWN_ACTOR = "알 수 없는 사용자"

internal fun List<MapActivity>.toMapLogUiModels(nowMillis: Long): List<MapLogUiModel> =
    mapIndexed { index, activity ->
        MapLogUiModel(
            id = "$index-${activity.type.name}-${activity.placeId}-${activity.occurredAtMillis}",
            type = activity.type.toLogType(),
            userName = activity.actorName ?: UNKNOWN_ACTOR,
            userImageUrl = activity.actorImageUrl,
            message = activity.toMessage(),
            timeAgo = relativeTimeLabel(activity.occurredAtMillis, nowMillis),
        )
    }

private fun MapActivityType.toLogType(): MapLogType = when (this) {
    MapActivityType.PlaceAdded -> MapLogType.PlaceAdded
    MapActivityType.PlaceRemoved -> MapLogType.PlaceRemoved
    MapActivityType.ReviewCreated -> MapLogType.ReviewCreated
}

/**
 * 카드에 들어갈 문장.
 *
 * 장소명이 없으면 이름을 뺀 문장으로 바꾼다. 빈 따옴표(`‘’`)가 남으면 지워진 장소처럼 보인다.
 */
private fun MapActivity.toMessage(): String = when (type) {
    MapActivityType.PlaceAdded -> placeName
        ?.let { name -> "‘$name’ ${name.objectParticle()} 추가했어요" }
        ?: "장소를 추가했어요"

    MapActivityType.PlaceRemoved -> placeName
        ?.let { name -> "‘$name’ ${name.objectParticle()} 지도에서 삭제했어요" }
        ?: "장소를 지도에서 삭제했어요"

    MapActivityType.ReviewCreated -> {
        val stars = rating?.let { score -> "별점 ${score}점 " }.orEmpty()
        placeName
            ?.let { name -> "‘$name’ 에 ${stars}후기를 남겼어요" }
            ?: "${stars}후기를 남겼어요"
    }
}

/** 한글 음절 영역. 이 밖의 글자는 받침을 따질 수 없다. */
private val HANGUL_SYLLABLES = '가'..'힣'

/** 한 음절을 이루는 종성의 가짓수. 받침 없음까지 세어 28 이다. */
private const val JONGSUNG_COUNT = 28

/**
 * 목적격 조사. 받침이 있으면 "을", 없으면 "를".
 *
 * 한글로 끝나지 않는 이름은 "를" 로 둔다. 영문·숫자는 읽는 소리를 봐야 정할 수 있는데,
 * 그러자고 발음 사전을 들일 만한 자리가 아니다.
 */
private fun String.objectParticle(): String {
    val last = lastOrNull() ?: return "를"
    if (last !in HANGUL_SYLLABLES) return "를"

    return if ((last - '가') % JONGSUNG_COUNT == 0) "를" else "을"
}
