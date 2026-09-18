package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.feature.explore.data.remote.PlaceReviewDto
import com.moamap.app.feature.mapdetail.domain.model.PlaceReview

fun PlaceReviewDto.toPlaceReview(authorName: String?): PlaceReview = PlaceReview(
    id = id,
    authorId = userId,
    authorName = authorName?.takeIf { name -> name.isNotBlank() },
    content = content?.trim().orEmpty(),
    imageUrls = imageUrls.filter { url -> url.isNotBlank() },
    createdAtMillis = parseServerDateTime(createdAt),
)
