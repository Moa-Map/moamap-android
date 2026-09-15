package com.moamap.app.feature.mapdetail.domain.repository

import com.moamap.app.feature.mapdetail.domain.model.MapPostPage
import com.moamap.app.feature.mapdetail.domain.model.MapPostSort

interface MapPostRepository {

    /** [page] 는 0 부터 센다. */
    suspend fun getPosts(mapId: Long, page: Int, sort: MapPostSort): MapPostPage
}
