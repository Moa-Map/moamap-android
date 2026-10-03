package com.moamap.app.feature.officialmap.data.repository

import com.moamap.app.feature.officialmap.data.remote.RestroomService
import com.moamap.app.feature.officialmap.domain.model.RestroomDetail
import com.moamap.app.feature.officialmap.domain.model.RestroomMarkers
import com.moamap.app.feature.officialmap.domain.repository.RestroomRepository
import javax.inject.Inject

class RestroomRepositoryImpl @Inject constructor(
    private val service: RestroomService,
) : RestroomRepository {

    override suspend fun getRestrooms(
        south: Double,
        west: Double,
        north: Double,
        east: Double,
    ): RestroomMarkers =
        service.getRestrooms(swLat = south, swLng = west, neLat = north, neLng = east).toDomain()

    override suspend fun getRestroom(id: Long): RestroomDetail = service.getRestroom(id).toDomain()
}
