package com.moamap.app.feature.mapdetail.presentation.members

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImage
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.ListCardShadowColor
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme

private val SheetShape = RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)
private val MemberCardShape = RoundedCornerShape(12.dp)
private val AvatarSize = 50.dp

/**
 * 멤버 관리 바텀시트.
 *
 * [showRoles] 와 [canGrantRole] 를 밖에서 받는다. 프라이빗 지도는 역할 개념이 없어 둘 다
 * false 다 - 시트가 지도 타입을 직접 알 필요는 없고, 그래야 프리뷰로 두 경우를 만들 수 있다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MemberSheet(
    members: List<MemberUiModel>,
    loading: Boolean,
    errorMessage: String?,
    showRoles: Boolean,
    canGrantRole: Boolean,
    granting: Boolean,
    onGrantRoleClick: (Long) -> Unit,
    onRetryClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = SheetShape,
        containerColor = MoaMapTheme.colors.backgroundSecondary,
    ) {
        MemberSheetContent(
            members = members,
            loading = loading,
            errorMessage = errorMessage,
            showRoles = showRoles,
            canGrantRole = canGrantRole,
            granting = granting,
            onGrantRoleClick = onGrantRoleClick,
            onRetryClick = onRetryClick,
        )
    }
}

/** 시트 껍데기와 분리해 프리뷰에서 그대로 그릴 수 있게 한다. */
@Composable
private fun MemberSheetContent(
    members: List<MemberUiModel>,
    loading: Boolean,
    errorMessage: String?,
    showRoles: Boolean,
    canGrantRole: Boolean,
    granting: Boolean,
    onGrantRoleClick: (Long) -> Unit,
    onRetryClick: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = MoaMapDimens.ScreenHorizontalPadding,
            end = MoaMapDimens.ScreenHorizontalPadding,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") {
            MemberSheetHeader(
                // 아직 못 읽었으면 인원수를 감춘다. 0 명은 참여자가 없다는 뜻이 되어 버린다.
                memberCount = members.size.takeIf { !loading && errorMessage == null },
                // 역할이 없는 지도에서는 역할 안내를 띄울 이유가 없다.
                showRoleGuide = showRoles,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        when {
            loading -> item(key = "members-loading") { LoadingMembers() }

            errorMessage != null -> item(key = "members-error") {
                MembersError(message = errorMessage, onRetryClick = onRetryClick)
            }

            else -> items(members, key = { member -> member.id }) { member ->
                MemberCard(
                    member = member,
                    showRole = showRoles,
                    canGrant = member.canGrantRole(canGrantRole),
                    // 오가는 중에는 다른 카드의 버튼도 잠근다. 한 번에 하나만 처리한다.
                    grantEnabled = !granting,
                    onGrantRoleClick = { onGrantRoleClick(member.id) },
                )
            }
        }
    }
}

@Composable
private fun LoadingMembers() {
    CenteredMemberNotice {
        CircularProgressIndicator(
            color = MoaMapTheme.colors.textAssistive,
            strokeWidth = 2.dp,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun MembersError(message: String, onRetryClick: () -> Unit) {
    CenteredMemberNotice {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = message,
                style = MoaMapTheme.typography.body2,
                color = MoaMapTheme.colors.textAssistive,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onRetryClick) {
                Text(
                    text = "다시 시도",
                    style = MoaMapTheme.typography.body2,
                    color = MoaMapTheme.colors.textNormal,
                )
            }
        }
    }
}

/** 목록 자리를 대신 채우는 안내. 두 상태가 같은 높이를 써야 시트가 덜컹이지 않는다. */
@Composable
private fun CenteredMemberNotice(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun MemberSheetHeader(
    memberCount: Int?,
    showRoleGuide: Boolean,
    modifier: Modifier = Modifier,
) {
    var guideVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "멤버 관리",
                style = MoaMapTheme.typography.title3,
                color = MoaMapTheme.colors.textNormal,
            )
            if (showRoleGuide) {
                Box {
                    Icon(
                        painter = painterResource(R.drawable.ic_info),
                        contentDescription = "역할 안내",
                        tint = MoaMapTheme.colors.textAssistive,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { guideVisible = !guideVisible },
                    )
                    if (guideVisible) {
                        Popup(
                            alignment = Alignment.TopStart,
                            onDismissRequest = { guideVisible = false },
                            properties = PopupProperties(focusable = true),
                        ) {
                            RoleGuideTooltip()
                        }
                    }
                }
            }
        }
        if (memberCount != null) {
            Text(
                text = "${memberCount}명 참여 중",
                style = MoaMapTheme.typography.body2,
                color = MoaMapTheme.colors.textAlternative,
            )
        }
    }
}

/** 역할별로 무엇을 할 수 있는지. 공개 지도에만 뜬다. */
@Composable
private fun RoleGuideTooltip() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MoaMapPrimitiveColors.Blue800)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RoleGuideColumn("방장", "장소 신청 수락·거절,\n권한 위임, 강퇴")
        RoleGuideColumn("관리자", "장소 신청 수락·거절")
        RoleGuideColumn("멤버", "장소 신청,\n별점·댓글")
    }
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

@Composable
private fun MemberCard(
    member: MemberUiModel,
    showRole: Boolean,
    canGrant: Boolean,
    grantEnabled: Boolean,
    onGrantRoleClick: () -> Unit,
) {
    ShadowedSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = MemberCardShape,
        color = MoaMapPrimitiveColors.White,
        shadowColor = ListCardShadowColor,
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                    // 프라이빗 지도에는 역할이 없다.
                    if (showRole) MemberRoleTag(member.role)
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
    Box(
        modifier = Modifier
            .size(AvatarSize)
            .clip(CircleShape)
            .background(MoaMapPrimitiveColors.Blue50),
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(AvatarSize),
            )
        }
    }
}

@Composable
private fun MemberRoleTag(role: MemberRole) {
    Text(
        text = role.label,
        style = MoaMapTheme.typography.caption0,
        color = MoaMapPrimitiveColors.Blue900,
        modifier = Modifier
            .clip(RoundedCornerShape(1000.dp))
            .background(MoaMapPrimitiveColors.Blue50)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
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
            text = "권한 부여",
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
    MemberUiModel(1L, "김도현", null, MemberRole.Owner),
    MemberUiModel(2L, "이서연", null, MemberRole.Admin),
    MemberUiModel(3L, "박지훈", null, MemberRole.Member),
)

@Preview(showBackground = true, widthDp = 393)
@Composable
private fun MemberSheetPublicPreview() {
    MoaMapTheme {
        Box(modifier = Modifier.background(MoaMapTheme.colors.backgroundSecondary)) {
            MemberSheetContent(
                members = PreviewMembers,
                loading = false,
                errorMessage = null,
                showRoles = true,
                canGrantRole = true,
                granting = false,
                onGrantRoleClick = {},
                onRetryClick = {},
            )
        }
    }
}

/** 프라이빗 지도: 역할 표시도 권한 부여도 없다. */
@Preview(showBackground = true, widthDp = 393)
@Composable
private fun MemberSheetPrivatePreview() {
    MoaMapTheme {
        Box(modifier = Modifier.background(MoaMapTheme.colors.backgroundSecondary)) {
            MemberSheetContent(
                members = PreviewMembers,
                loading = false,
                errorMessage = null,
                showRoles = false,
                canGrantRole = false,
                granting = false,
                onGrantRoleClick = {},
                onRetryClick = {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393)
@Composable
private fun MemberSheetErrorPreview() {
    MoaMapTheme {
        Box(modifier = Modifier.background(MoaMapTheme.colors.backgroundSecondary)) {
            MemberSheetContent(
                members = emptyList(),
                loading = false,
                errorMessage = MEMBER_LOAD_FAILED_MESSAGE,
                showRoles = true,
                canGrantRole = true,
                granting = false,
                onGrantRoleClick = {},
                onRetryClick = {},
            )
        }
    }
}
