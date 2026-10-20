package com.moamap.app.feature.mypage.domain.repository

import com.moamap.app.feature.mypage.domain.model.Notice
import com.moamap.app.feature.mypage.domain.model.NoticePage

interface NoticeRepository {

    /** 최신 등록순 [page] 번째 쪽(0부터). */
    suspend fun getNotices(page: Int, size: Int): NoticePage

    suspend fun getNotice(id: Long): Notice
}
