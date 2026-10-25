package com.moamap.app.feature.explore.domain.model

import androidx.compose.runtime.Immutable

/** 홈 맨 위 배너 한 장. 그림 한 장이 배너 하나다. */
@Immutable
data class Banner(
    val id: Long,
    val imageUrl: String,
)
