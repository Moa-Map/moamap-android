package com.moamap.app.feature.mapdetail.domain.repository

import android.net.Uri
import com.moamap.app.feature.mapdetail.domain.model.MapPostPage
import com.moamap.app.feature.mapdetail.domain.model.MapPostSort
import com.moamap.app.feature.mapdetail.domain.model.NewMapPost

interface MapPostRepository {

    /** [page] 는 0 부터 센다. */
    suspend fun getPosts(mapId: Long, page: Int, sort: MapPostSort): MapPostPage

    /**
     * 사진을 올리고 게시물에 담을 주소를 고른 순서대로 돌려준다.
     *
     * 하나라도 형식·크기에 걸리면 아무것도 올리지 않고
     * [com.moamap.app.core.common.upload.ImageUploadException] 을 던진다.
     */
    suspend fun uploadPhotos(mapId: Long, photos: List<Uri>): List<String>

    suspend fun createPost(mapId: Long, post: NewMapPost)
}
