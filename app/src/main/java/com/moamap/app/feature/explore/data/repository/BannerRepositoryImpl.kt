package com.moamap.app.feature.explore.data.repository

import com.moamap.app.feature.explore.data.remote.BannerService
import com.moamap.app.feature.explore.domain.model.Banner
import com.moamap.app.feature.explore.domain.repository.BannerRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class BannerRepositoryImpl @Inject constructor(
    private val bannerService: BannerService,
) : BannerRepository {

    /** 그림 주소가 빈 배너는 뺀다. 그릴 것이 없다. */
    override suspend fun getBanners(): List<Banner> = bannerService.getBanners().mapNotNull { dto ->
        dto.imageUrl?.takeIf { url -> url.isNotBlank() }?.let { url -> Banner(id = dto.bannerId, imageUrl = url) }
    }
}
