package com.moamap.app.feature.mypage.data.repository

import com.moamap.app.feature.mapdetail.data.repository.parseServerDateTime
import com.moamap.app.feature.mypage.data.remote.NoticeDto
import com.moamap.app.feature.mypage.data.remote.NoticeService
import com.moamap.app.feature.mypage.domain.model.Notice
import com.moamap.app.feature.mypage.domain.model.NoticePage
import com.moamap.app.feature.mypage.domain.repository.NoticeRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class NoticeRepositoryImpl @Inject constructor(
    private val noticeService: NoticeService,
) : NoticeRepository {

    override suspend fun getNotices(page: Int, size: Int): NoticePage {
        val response = noticeService.getNotices(page = page, size = size)
        return NoticePage(notices = response.content.map { dto -> dto.toNotice() }, isLast = response.last)
    }

    override suspend fun getNotice(id: Long): Notice = noticeService.getNotice(id).toNotice()
}

internal fun NoticeDto.toNotice(): Notice = Notice(
    id = noticeId,
    title = title.orEmpty(),
    content = content.orEmpty(),
    createdAtMillis = parseServerDateTime(createdAt),
)
