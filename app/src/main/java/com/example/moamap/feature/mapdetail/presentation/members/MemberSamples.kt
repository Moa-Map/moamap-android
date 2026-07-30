package com.example.moamap.feature.mapdetail.presentation.members

/**
 * 멤버 관리 목데이터.
 *
 * 서버에 멤버 목록 API 가 아직 없어 화면만 먼저 만든다. **연동할 때 이 파일을 지운다.**
 */
internal val SampleMembers = listOf(
    MemberUiModel(1L, "김도현", null, MemberRole.Owner, "24곳"),
    MemberUiModel(2L, "이서연", null, MemberRole.Admin, "18곳"),
    MemberUiModel(3L, "박지훈", null, MemberRole.Member, "9곳"),
    MemberUiModel(4L, "최유진", null, MemberRole.Member, "5곳"),
    MemberUiModel(5L, "정민수", null, MemberRole.Member, "0곳"),
)
