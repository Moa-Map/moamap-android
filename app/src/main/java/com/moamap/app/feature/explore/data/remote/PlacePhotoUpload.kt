package com.moamap.app.feature.explore.data.remote

import android.net.Uri
import com.moamap.app.core.common.upload.MAX_PLACE_PHOTO_FILE_SIZE
import com.moamap.app.core.common.upload.PhotoUploader
import com.moamap.app.core.common.upload.validateImageUpload

/**
 * 장소 사진을 올리고 등록 요청에 실을 접근 주소를 돌려준다.
 *
 * 살펴보기 → 일괄 발급 → 장별 업로드 순으로 간다. 발급 요청이 각 파일의 크기와 형식을 한 번에
 * 요구해서 먼저 다 살펴봐야 한다. 다만 내용은 그때 읽지 않는다 - 수 MB 짜리 다섯 장을 동시에
 * 들고 있으면 터진다. 업로드는 한 장씩, 그 순간에 흘려보낸다.
 *
 * 한 장이라도 실패하면 예외를 던진다. 사진이 빠진 채로 장소가 등록되면 사용자가 알아챌 방법이 없다.
 *
 * 살펴본 뒤 곧바로 검증한다. 발급을 받고 나서 걸러도 늦다 - 한 장이라도 올린 뒤에 막히면 지울
 * 방법이 없어 스토리지에 고아 파일이 남는다.
 *
 * 지도 상세의 장소 추가와 링크로 가져온 장소의 일괄 등록이 함께 쓴다.
 *
 * @param mapId 발급 권한이 장소 등록 권한과 같아 서버가 요구한다.
 */
internal suspend fun PlaceService.uploadPlacePhotos(
    uploader: PhotoUploader,
    mapId: Long,
    photos: List<Uri>,
): List<String> {
    if (photos.isEmpty()) return emptyList()

    val specs = photos.map { uri ->
        uploader.inspect(uri).also { photo ->
            validateImageUpload(
                contentType = photo.contentType,
                fileSize = photo.size,
                maxFileSize = MAX_PLACE_PHOTO_FILE_SIZE,
            )
        }
    }

    val issued = createPhotoUploadUrls(
        PhotoUploadUrlRequestDto(
            mapId = mapId,
            files = specs.map { photo ->
                PhotoFileSpecDto(contentType = photo.contentType, fileSize = photo.size)
            },
        ),
    )

    // 발급 수가 요청 수와 다르면 어떤 사진이 빠졌는지 알 수 없다. 조용히 덜 올리지 않는다.
    check(issued.size == specs.size) {
        "사진 업로드 주소를 ${specs.size}개 요청했는데 ${issued.size}개 받았습니다"
    }

    issued.forEachIndexed { index, url ->
        require(url.uploadUrl.isNotBlank() && url.fileUrl.isNotBlank()) {
            "사진 업로드 주소가 비어 있습니다"
        }
        uploader.upload(uploadUrl = url.uploadUrl, photo = specs[index])
    }

    return issued.map { url -> url.fileUrl }
}
