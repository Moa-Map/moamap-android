package com.moamap.app.feature.mapdetail.data.repository

import android.net.Uri
import com.moamap.app.core.common.upload.MAX_POST_PHOTO_FILE_SIZE
import com.moamap.app.core.common.upload.PhotoUploader
import com.moamap.app.core.common.upload.validateImageUpload
import com.moamap.app.feature.mapdetail.data.remote.MapPostCreateRequestDto
import com.moamap.app.feature.mapdetail.data.remote.MapPostPhotoUploadUrlRequestDto
import com.moamap.app.feature.mapdetail.data.remote.MapPostPlaceTagRequestDto
import com.moamap.app.feature.mapdetail.data.remote.MapPostService
import com.moamap.app.feature.mapdetail.domain.model.MapPostPage
import com.moamap.app.feature.mapdetail.domain.model.MapPostSort
import com.moamap.app.feature.mapdetail.domain.model.NewMapPost
import com.moamap.app.feature.mapdetail.domain.repository.MapPostRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 한 번에 받아 오는 게시물 수. 서버 기본값과 같다. */
internal const val POST_PAGE_SIZE = 20

private const val SORT_LATEST = "createdAt,desc"
private const val SORT_OLDEST = "createdAt,asc"

@Singleton
internal class MapPostRepositoryImpl @Inject constructor(
    private val mapPostService: MapPostService,
    private val uploader: PhotoUploader,
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

    /**
     * 전부 먼저 살펴본 뒤에 올린다.
     *
     * 한 장씩 살펴보고 곧바로 올리면, 셋째 사진이 형식에 걸렸을 때 앞의 두 장은 이미 올라가
     * 지울 방법이 없다. 형식·크기 검사는 발급을 요청하기 전에 모두 끝낸다.
     */
    override suspend fun uploadPhotos(mapId: Long, photos: List<Uri>): List<String> {
        if (photos.isEmpty()) return emptyList()

        val specs = photos.map { uri ->
            uploader.inspect(uri).also { photo ->
                validateImageUpload(
                    contentType = photo.contentType,
                    fileSize = photo.size,
                    maxFileSize = MAX_POST_PHOTO_FILE_SIZE,
                )
            }
        }

        return specs.map { photo ->
            val issued = mapPostService.createPhotoUploadUrl(
                mapId = mapId,
                request = MapPostPhotoUploadUrlRequestDto(
                    contentType = photo.contentType,
                    fileSize = photo.size,
                ),
            )
            require(issued.uploadUrl.isNotBlank() && issued.fileUrl.isNotBlank()) {
                "사진 업로드 주소가 비어 있습니다"
            }
            uploader.upload(uploadUrl = issued.uploadUrl, photo = photo)
            issued.fileUrl
        }
    }

    override suspend fun createPost(mapId: Long, post: NewMapPost) {
        mapPostService.createPost(
            mapId = mapId,
            request = MapPostCreateRequestDto(
                content = post.content,
                imageUrls = post.photoUrls,
                placeTags = post.placeTags.map { tag ->
                    MapPostPlaceTagRequestDto(placeId = tag.placeId, name = tag.name)
                },
            ),
        )
    }
}
