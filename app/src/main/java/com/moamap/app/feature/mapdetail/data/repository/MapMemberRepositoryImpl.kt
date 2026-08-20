package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.feature.collection.data.remote.MapMemberRoleUpdateRequestDto
import com.moamap.app.feature.collection.data.remote.MapService
import com.moamap.app.feature.mapdetail.domain.model.MapMember
import com.moamap.app.feature.mapdetail.domain.repository.MapMemberRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val ROLE_ADMIN = "ADMIN"

@Singleton
class MapMemberRepositoryImpl @Inject constructor(
    private val mapService: MapService,
) : MapMemberRepository {

    override suspend fun getMembers(mapId: Long): List<MapMember> =
        mapService.getMembers(mapId).members.map { dto -> dto.toMapMember() }

    override suspend fun grantAdmin(mapId: Long, userId: Long) {
        mapService.updateMemberRole(
            mapId = mapId,
            userId = userId,
            request = MapMemberRoleUpdateRequestDto(role = ROLE_ADMIN),
        )
    }
}
