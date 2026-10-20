package com.moamap.app.feature.mypage.domain.model

data class Notice(
    val id: Long,
    val title: String,
    val content: String,
    /** 등록 시각. 서버 값을 읽지 못했으면 null 이고 화면은 날짜를 빼고 그린다. */
    val createdAtMillis: Long?,
)

data class NoticePage(
    val notices: List<Notice>,
    val isLast: Boolean,
)
