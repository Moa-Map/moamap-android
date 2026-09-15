package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.feature.mapdetail.data.remote.MapPostService
import com.moamap.app.feature.mapdetail.domain.model.MapPostPage
import com.moamap.app.feature.mapdetail.domain.model.MapPostSort
import com.moamap.app.feature.mapdetail.domain.repository.MapPostRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 한 번에 받아 오는 게시물 수. 서버 기본값과 같다. */
internal const val POST_PAGE_SIZE = 20

private const val SORT_LATEST = "createdAt,desc"
private const val SORT_OLDEST = "createdAt,asc"

@Singleton
class MapPostRepositoryImpl @Inject constructor(
    private val mapPostService: MapPostService,
) : MapPostRepository {

    override suspend fun getPosts(mapId: Long, page: Int, sort: MapPostSort): MapPostPage {
        val response = mapPostService.getPosts(
            mapId = mapId,
            page = page,
            size = POST_PAGE_SIZE,
            // 최신순도 빼지 않고 보낸다. 서버 기본값이 바뀌어도 화면의 선택과 어긋나지 않는다.
            sort = when (sort) {
                MapPostSort.Latest -> SORT_LATEST
                MapPostSort.Oldest -> SORT_OLDEST
            },
        )

        return MapPostPage(
            posts = response.content.map { dto -> dto.toMapPost() },
            // 빈 페이지도 끝으로 본다. 서버가 `last` 를 잘못 내려도 스크롤할 때마다 헛조회가 반복되지 않는다.
            isLast = response.last || response.content.isEmpty(),
        )
    }
}
