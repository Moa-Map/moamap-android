package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.core.network.kakao.KakaoLocalService
import com.moamap.app.feature.mapdetail.domain.model.PlaceCandidate
import com.moamap.app.feature.mapdetail.domain.repository.PlaceSearchRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 카카오 로컬 API 를 직접 부르는 구현. 서버 프록시가 생기면 이 클래스만 갈아끼운다. */
@Singleton
class KakaoPlaceSearchRepository @Inject constructor(
    private val service: KakaoLocalService,
) : PlaceSearchRepository {

    override suspend fun search(query: String): List<PlaceCandidate> {
        val trimmed = query.trim()
        // 빈 검색어를 보내면 카카오가 400 을 낸다. 부를 이유도 없다.
        if (trimmed.isEmpty()) return emptyList()

        return service.searchKeyword(query = trimmed, size = PAGE_SIZE)
            .documents
            .mapNotNull { dto -> dto.toPlaceCandidate() }
    }

    private companion object {
        /** 카카오 기본값과 같다. 무한 스크롤을 붙이기 전까지는 첫 페이지만 쓴다. */
        const val PAGE_SIZE = 15
    }
}
