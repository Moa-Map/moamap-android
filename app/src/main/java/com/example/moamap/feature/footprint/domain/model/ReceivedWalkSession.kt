package com.example.moamap.feature.footprint.domain.model

import com.example.moamap.core.walksession.WalkSessionPayload
import com.example.moamap.core.walksession.WalkSessionStats

/** 워치에서 받아 폰에 저장된 세션 하나. */
data class ReceivedWalkSession(
    val payload: WalkSessionPayload,
    val stats: WalkSessionStats,
    val receivedAtEpochMillis: Long,
    val fileName: String,
)
