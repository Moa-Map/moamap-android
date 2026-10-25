package com.moamap.app.feature.explore.domain.repository

import com.moamap.app.feature.explore.domain.model.Banner

interface BannerRepository {
    /** 노출 순서대로의 배너. 실패 시 예외를 던진다. */
    suspend fun getBanners(): List<Banner>
}
