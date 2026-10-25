package com.moamap.app.feature.mapdetail.presentation.members

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.core.designsystem.component.MoaMapCenteredNotice
import com.moamap.app.core.designsystem.component.MoaMapConfirmDialog
import com.moamap.app.core.designsystem.component.MoaMapErrorNotice
import com.moamap.app.core.designsystem.component.MoaMapGuideRow
import com.moamap.app.core.designsystem.component.MoaMapLoadingIndicator
import com.moamap.app.core.designsystem.component.MoaMapTitleTopBar
import com.moamap.app.core.designsystem.component.MoaMapTooltip
import com.moamap.app.core.designsystem.component.PhotoThumbnail
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import com.moamap.app.feature.mapdetail.RoleBadge
import com.moamap.app.feature.mapdetail.domain.model.MapRole
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

private val MemberCardShape = RoundedCornerShape(12.dp)
private val AvatarSize = 50.dp

/** 이름 옆 역할 뱃지. 원 지름 20(피그마)에 바깥 흰 테두리까지 그린 크기다. */
private val RoleBadgeSize = 23.dp

/** 이름과 뱃지 원 사이 4(피그마). 그림 가장자리의 흰 테두리 몫 1.5 를 뺀다. */
private val RoleBadgeGap = 2.5.dp

private val RoleGuideScrim = MoaMapPrimitiveColors.Gray500.copy(alpha = 0.7f)

/**
 * 멤버 관리. 상단바 메뉴에서 들어온다.
 *
 * 장소 관리처럼 지도 상세 위에 겹쳐 그린다. 목록과 관리자 지정이 지도 상세 백스택에 묶인
 * [MemberViewModel] 을 그대로 쓰기 때문이다.
 *
 * [roleDisplay] 와 [canGrantRole] 을 밖에서 받는다. 화면이 지도 종류를 직접 알 필요는 없고,
 * 그래야 프리뷰로 각 경우를 만들 수 있다.
 *
 * 커뮤니티 지도는 시안 `4243:29310` 대로 내 카드를 「내 역할」로 맨 위에 따로 두고, 나머지를
 * 「다른 멤버 N명」 아래에 둔다([splitMine]).
 */
@Composable
internal fun MemberScreen(
    members: List<MemberUiModel>,
    myId: Long?,
    loading: Boolean,
    errorMessage: String?,
    roleDisplay: MemberRoleDisplay,
    canGrantRole: Boolean,
    granting: Boolean,
    onGrantRoleClick: (Long) -> Unit,
    onRetryClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 역할 안내 말풍선이 이 화면을 흐려 바탕으로 깐다.
    val roleGuideHazeState = rememberHazeState()
    // 관리자로 지정하기 전에 한 번 묻는다(피그마 릴리즈 v1 「멤버관리 - info」).
    var grantTargetId by rememberSaveable { mutableStateOf<Long?>(null) }
    val grantTarget = members.firstOrNull { member -> member.id == grantTargetId }
    if (grantTarget != null) {
        MoaMapConfirmDialog(
            title = grantTarget.name,
            titleSuffix = "님을 관리자로 지정할까요?",
            message = "관리자는 장소 신청 관리하고, 모든 장소를 수정·삭제할 수 있어요",
            confirmText = "확인",
            onConfirm = {
                grantTargetId = null
                onGrantRoleClick(grantTarget.id)
            },
            onDismissRequest = { grantTargetId = null },
            dismissText = "닫기",
            dismissColor = MoaMapPrimitiveColors.Gray200,
        )
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .hazeSource(roleGuideHazeState)
            .background(MoaMapTheme.colors.backgroundSecondary)
            // 뒤에 깔린 지도로 터치가 새지 않게 빈 자리의 탭을 여기서 받는다.
            .pointerInput(Unit) { detectTapGestures() }
            .statusBarsPadding(),
    ) {
        // 시안: 상단 바 58, 그 아래 8 에서 목록이 시작한다.
        MoaMapTitleTopBar(title = "멤버 관리", onBackClick = onBackClick)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = MoaMapDimens.ScreenHorizontalPadding,
                top = 8.dp,
                end = MoaMapDimens.ScreenHorizontalPadding,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "header") {
                MemberHeader(
                    // 아직 못 읽었으면 인원수를 감춘다. 0 명은 참여자가 없다는 뜻이 되어 버린다.
                    memberCount = members.size.takeIf { !loading && errorMessage == null },
                    // 역할이 나뉘는 지도에서만 역할 안내를 띄운다.
                    showRoleGuide = roleDisplay == MemberRoleDisplay.All,
                    roleGuideHazeState = roleGuideHazeState,
                    // 목록 간격 8 과 합쳐 머리글과 목록 사이가 시안대로 20 이 된다.
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            when {
                loading -> item(key = "members-loading") { MoaMapCenteredNotice { MoaMapLoadingIndicator() } }

                errorMessage != null -> item(key = "members-error") {
                    MoaMapCenteredNotice { MoaMapErrorNotice(message = errorMessage, onRetryClick = onRetryClick) }
                }

                else -> {
                    val sections = members.splitMine(myId, roleDisplay)
                    if (sections != null) {
                        val (me, others) = sections
                        item(key = "my-title") { MemberSectionTitle("내 역할") }
                        item(key = me.id) {
                            // 시안 「본인」 카드에는 관리자 지정 버튼이 없다.
                            MemberCard(
                                member = me,
                                tag = me.tag(roleDisplay),
                                canGrant = false,
                                grantEnabled = false,
                                onGrantRoleClick = {},
                                mine = true,
                            )
                        }
                        // 시안: 묶음 사이 16. 목록 간격 8 에 8 을 더한다. 다른 멤버가 없어도 제목은 남긴다.
                        item(key = "others-title") {
                            MemberSectionTitle("다른 멤버 ${others.size}명", Modifier.padding(top = 8.dp))
                        }
                    }
                    items(sections?.second ?: members, key = { member -> member.id }) { member ->
                        MemberCard(
                            member = member,
                            tag = member.tag(roleDisplay),
                            canGrant = member.canGrantRole(canGrantRole),
                            // 오가는 중에는 다른 카드의 버튼도 잠근다. 한 번에 하나만 처리한다.
                            grantEnabled = !granting,
                            onGrantRoleClick = { grantTargetId = member.id },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberHeader(
    memberCount: Int?,
    showRoleGuide: Boolean,
    roleGuideHazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (memberCount != null) {
            Text(
                text = "${memberCount}명 참여 중",
                style = MoaMapTheme.typography.subtitle1,
                color = MoaMapTheme.colors.textNormal,
            )
        }
        if (showRoleGuide) RoleGuideRow(roleGuideHazeState)
    }
}

/**
 * 「ⓘ 어떤 역할이 있는지 궁금하신가요?」. 누르면 바로 아래에 역할별로 무엇을 할 수 있는지 말풍선이 뜬다.
 * 커뮤니티 지도에만 뜬다.
 *
 * 시안 `4243:29277`: 진회색 70% 에 뒤 흐림. 멤버 설명의 「별점·댓글」은 앱에 별점이 없어 「댓글」로 둔다.
 */
@Composable
private fun RoleGuideRow(hazeState: HazeState) {
    MoaMapGuideRow(
        text = "어떤 역할이 있는지 궁금하신가요?",
        tooltipColor = RoleGuideScrim,
        tooltipHazeState = hazeState,
    ) {
        RoleGuideTooltipContent()
    }
}

@Composable
private fun RoleGuideTooltipContent() {
    RoleGuideColumn("방장", "장소 신청 수락·거절,\n권한 위임, 강퇴")
    RoleGuideColumn("관리자", "장소 신청 수락·거절")
    RoleGuideColumn("멤버", "장소 신청,\n댓글")
}

@Composable
private fun RoleGuideColumn(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MoaMapTheme.typography.body3,
            color = MoaMapTheme.colors.textWhite,
        )
        Text(
            text = description,
            style = MoaMapTheme.typography.caption0,
            color = MoaMapTheme.colors.textWhite,
        )
    }
}

/** 「내 역할」·「다른 멤버 N명」. */
@Composable
private fun MemberSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        // 한 줄 글자도 줄 높이만큼 차지해야 시안 간격(제목 → 카드 8)이 맞는다.
        style = MoaMapTheme.typography.subtitle4.withDesignLineHeight(),
        color = MoaMapTheme.colors.textNormal,
        modifier = modifier.semantics { heading() },
    )
}

/** @param mine 내 카드. 시안 「사용자 리스트 / State=본인」 대로 노란 바탕에 노란 테두리다. */
@Composable
private fun MemberCard(
    member: MemberUiModel,
    tag: MemberRole?,
    canGrant: Boolean,
    grantEnabled: Boolean,
    onGrantRoleClick: () -> Unit,
    mine: Boolean = false,
) {
    ShadowedSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = MemberCardShape,
        color = if (mine) MoaMapPrimitiveColors.Yellow50 else MoaMapPrimitiveColors.White,
        border = if (mine) BorderStroke(1.dp, MoaMapPrimitiveColors.Yellow500) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MemberAvatar(imageUrl = member.imageUrl)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        // 뱃지가 없어도 줄 높이는 시안대로 24 로 둔다.
                        modifier = Modifier.height(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(RoleBadgeGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = member.name,
                            style = MoaMapTheme.typography.subtitle2,
                            color = MoaMapTheme.colors.textNormal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (tag != null) RoleBadge(role = tag.toMapRole(), size = RoleBadgeSize)
                    }
                    member.placeCount?.let { count ->
                        Text(
                            text = "등록한 장소 수: $count",
                            style = MoaMapTheme.typography.caption0,
                            color = MoaMapTheme.colors.textAlternative,
                        )
                    }
                }
            }

            if (canGrant) {
                GrantRoleButton(enabled = grantEnabled, onClick = onGrantRoleClick)
            }
        }
    }
}

@Composable
private fun MemberAvatar(imageUrl: String?) {
    // 프로필 사진이 없으면 시안 「이미지 플레이스홀더」 50 원형.
    PhotoThumbnail(imageUrl = imageUrl, size = AvatarSize, shape = CircleShape)
}

private fun MemberRole.toMapRole(): MapRole = when (this) {
    MemberRole.Owner -> MapRole.Owner
    MemberRole.Admin -> MapRole.Admin
    MemberRole.Member -> MapRole.Member
}

@Composable
private fun GrantRoleButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(1000.dp))
            .background(MoaMapPrimitiveColors.Blue500)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp),
    ) {
        Text(
            // 피그마는 「권한 부여」지만 무엇을 주는지 보이게 바꿨다(10-25 사용자 결정).
            text = "관리자 지정",
            style = MoaMapTheme.typography.button2,
            color = MoaMapTheme.colors.textWhite,
        )
    }
}

/**
 * 프리뷰 전용 표본. **화면에 넘기지 않는다.**
 *
 * 지어낸 이름이라 실제 지도에 띄우면 없는 사람이 참여한 것처럼 보인다. 별도 파일로 두지 않는
 * 이유도 그것이다 - 여기 private 으로 묶어 두면 프리뷰 밖으로 샐 길이 없다.
 */
private val PreviewMembers = listOf(
    MemberUiModel(1L, "김도현", null, MemberRole.Owner, placeCount = 12),
    MemberUiModel(2L, "이서연", null, MemberRole.Admin, placeCount = 5),
    MemberUiModel(3L, "박지훈", null, MemberRole.Member, placeCount = 0),
)

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MemberScreenCommunityPreview() {
    MoaMapTheme {
        MemberScreen(
            members = PreviewMembers,
            // 방장인 내가 「내 역할」로 맨 위에.
            myId = 1L,
            loading = false,
            errorMessage = null,
            roleDisplay = MemberRoleDisplay.All,
            canGrantRole = true,
            granting = false,
            onGrantRoleClick = {},
            onRetryClick = {},
            onBackClick = {},
        )
    }
}

/** 프라이빗 지도: 역할 안내·뱃지·관리자 지정 없이 한 줄 목록. 내 카드도 따로 두지 않는다. */
@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MemberScreenPrivatePreview() {
    MoaMapTheme {
        MemberScreen(
            members = PreviewMembers,
            myId = 1L,
            loading = false,
            errorMessage = null,
            roleDisplay = MemberRoleDisplay.None,
            canGrantRole = false,
            granting = false,
            onGrantRoleClick = {},
            onRetryClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RoleGuideTooltipPreview() {
    MoaMapTheme {
        MoaMapTooltip(tailAlignment = Alignment.Start, tailInset = 14.dp, color = RoleGuideScrim) {
            RoleGuideTooltipContent()
        }
    }
}
