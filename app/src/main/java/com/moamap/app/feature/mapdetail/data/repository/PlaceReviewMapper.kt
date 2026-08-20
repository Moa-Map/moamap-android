package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.feature.explore.data.remote.PlaceReviewDto
import com.moamap.app.feature.mapdetail.domain.model.PlaceReview

fun PlaceReviewDto.toPlaceReview(authorName: String?): PlaceReview = PlaceReview(
    id = id,
    authorId = userId,
    authorName = authorName?.takeIf { name -> name.isNotBlank() },
    // 별점은 서버가 1~5 로 검증하지만, 옛 데이터가 벗어나도 별 다섯 칸 밖으로 나가지 않게 한다.
    rating = rating.coerceIn(0, 5),
    content = content?.trim().orEmpty(),
    createdAtMillis = parseServerDateTime(createdAt),
)
