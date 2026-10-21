package com.moamap.app.feature.collection

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moamap.app.R
import com.moamap.app.core.common.format.formatMemberCount
import com.moamap.app.core.common.format.formatPlaceCount
import com.moamap.app.core.designsystem.component.PhotoThumbnail
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.collection.domain.model.MyMap
import com.moamap.app.feature.mapdetail.domain.model.MapRole

/** 카드 썸네일과 같은 높이를 유지해 제목/메타가 위아래로 벌어지도록 한다. */
private val CardThumbnailSize = 68.dp

/** 역할 뱃지 그림 크기. 카드 위에서는 원 지름 22 라 원본(테두리까지 23 칸)의 1.1배다. */
private val RoleBadgeSize = 25.3.dp

/** 뱃지 원이 사진 오른쪽 아래 모서리에서 오른쪽 5·아래 3 밖으로 나온다. 그림 가장자리의 테두리 몫(1.65)만큼 더 민다. */
private val RoleBadgeOffsetX = 6.65.dp
private val RoleBadgeOffsetY = 4.65.dp

@Immutable
internal data class CollectionMapUiModel(
    val id: Long,
    val title: String,
    /** 커버 이미지 주소. null 이면 [PhotoThumbnail] 이 기본 사진을 그린다. */
    val imageUrl: String? = null,
    /** 등록 장소 수. null 이면 그 자리를 그리지 않는다. */
    val placeCount: String? = null,
    val verified: Boolean = false,
    /** null 이면 인원 수를 노출하지 않는다. */
    val memberCount: String? = null,
    /** 내 역할. 방장·관리자면 사진 모서리에 뱃지를 단다. 커뮤니티 카드만 채운다. */
    val role: MapRole = MapRole.None,
)

internal fun MyMap.toCommunityUiModel(role: MapRole = MapRole.None) = CollectionMapUiModel(
    id = id,
    title = title,
    imageUrl = imageUrl,
    placeCount = formatPlaceCount(placeCount),
    verified = official,
    memberCount = formatMemberCount(memberCount),
    role = role,
)

/** 프라이빗 카드는 장소 수만 보여주는 자리다. 인원 수는 시안에 없다. */
internal fun MyMap.toPrivateUiModel() = CollectionMapUiModel(
    id = id,
    title = title,
    imageUrl = imageUrl,
    placeCount = formatPlaceCount(placeCount),
    verified = official,
)

/**
 * 모음 지도 카드.
 *
 * 장소 가져오기의 지도 선택 화면도 같은 카드를 쓰므로, 선택 표시 같은 우측 요소는
 * [trailingContent] 슬롯으로 받는다.
 *
 * @param reorderable 순서를 바꿀 수 있는 모음 목록의 카드다.
 * @param showDragHandle 왼쪽에 순서 손잡이를 둔다. 손잡이가 들면 왼쪽 여백이 12 로 준다.
 * @param dragHandleModifier 손잡이에 다는 끌기 동작.
 * @param pressFeedback 누르는 동안 회색(Gray50)으로 바꾼다. 앱은 누름 효과를 꺼 두었지만
 *  모음 편집은 시안(`1974:8284`)에 있다.
 */
@Composable
internal fun CollectionMapCard(
    map: CollectionMapUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    reorderable: Boolean = false,
    showDragHandle: Boolean = false,
    dragHandleModifier: Modifier = Modifier,
    pressFeedback: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    ShadowedSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (pressFeedback && pressed) MoaMapPrimitiveColors.Gray50 else MoaMapPrimitiveColors.White,
        border = border,
    ) {
        Row(
            modifier = Modifier
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .padding(
                    start = if (showDragHandle) 12.dp else 16.dp,
                    end = 16.dp,
                    top = if (reorderable) 16.dp else 20.dp,
                    bottom = if (reorderable) 16.dp else 20.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showDragHandle) {
                Icon(
                    painter = painterResource(R.drawable.ic_drag_handle),
                    contentDescription = null,
                    tint = MoaMapTheme.colors.textNormal,
                    modifier = dragHandleModifier.size(24.dp),
                )
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    PhotoThumbnail(imageUrl = map.imageUrl, size = CardThumbnailSize)
                    RoleBadge(
                        role = map.role,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = RoleBadgeOffsetX, y = RoleBadgeOffsetY),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(CardThumbnailSize)
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = map.title,
                            style = MoaMapTheme.typography.subtitle2,
                            color = MoaMapPrimitiveColors.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (map.verified) {
                            Icon(
                                painter = painterResource(R.drawable.ic_verify_filled),
                                contentDescription = "공식 인증",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    // 인원 수가 있는 카드와 없는 카드가 아이콘 크기·간격이 다르다.
                    // 둘 다 없으면 메타 줄을 그리지 않고 자리를 비워 둔다.
                    if (map.memberCount != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CollectionMapMeta(
                                iconRes = R.drawable.ic_person,
                                text = map.memberCount,
                                contentDescription = "참여 인원",
                                iconSize = 14.dp,
                                gap = 2.dp,
                            )
                            if (map.placeCount != null) {
                                CollectionMapMeta(
                                    iconRes = R.drawable.ic_location,
                                    text = map.placeCount,
                                    contentDescription = "등록 장소",
                                    iconSize = 14.dp,
                                    gap = 2.dp,
                                )
                            }
                        }
                    } else if (map.placeCount != null) {
                        CollectionMapMeta(
                            iconRes = R.drawable.ic_location,
                            text = map.placeCount,
                            contentDescription = "등록 장소",
                            iconSize = 12.dp,
                            gap = 4.dp,
                        )
                    }
                }

                trailingContent?.invoke()
            }
        }
    }
}

/** 방장은 파란 원에 점 셋 달린 왕관, 관리자는 노란 원에 점 없는 왕관. 그 밖의 역할은 그리지 않는다. */
@Composable
private fun RoleBadge(role: MapRole, modifier: Modifier = Modifier) {
    val (iconRes, label) = when (role) {
        MapRole.Owner -> R.drawable.ic_role_badge_owner to "방장"
        MapRole.Admin -> R.drawable.ic_role_badge_admin to "관리자"
        MapRole.Member, MapRole.None -> return
    }
    Image(
        painter = painterResource(iconRes),
        contentDescription = label,
        modifier = modifier.size(RoleBadgeSize),
    )
}

@Composable
private fun CollectionMapMeta(
    @DrawableRes iconRes: Int,
    text: String,
    contentDescription: String,
    iconSize: Dp,
    gap: Dp,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = MoaMapTheme.colors.textAssistive,
            modifier = Modifier.size(iconSize),
        )
        Text(
            text = text,
            style = MoaMapTheme.typography.caption0,
            color = MoaMapTheme.colors.textAssistive,
            maxLines = 1,
        )
    }
}
