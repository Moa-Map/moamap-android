package com.moamap.app.feature.collection

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.MoaMapErrorNotice
import com.moamap.app.core.designsystem.component.MoaMapListPlaceholder
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import com.moamap.app.feature.collection.domain.model.MyMap
import com.moamap.app.feature.collection.presentation.CollectionEditState
import com.moamap.app.feature.collection.presentation.LeaveEligibility
import com.moamap.app.feature.collection.presentation.MyMapsState
import com.moamap.app.feature.collection.presentation.placeimport.PlaceImportCheckBox
import com.moamap.app.feature.collection.presentation.placeimport.selectedCardBorder
import com.moamap.app.feature.collection.presentation.splitPersonal

/** 모음 카드끼리의 간격. 순서를 바꿀 때 이웃을 넘었는지 재는 데도 쓴다. */
private val CardSpacing = 8.dp

/** 인스타그램 브랜드 색. 디자인 시스템 팔레트가 아니라서 토큰으로 승격하지 않는다. */
private val InstagramCardBackground = Color(0xFFFFF5FB)
private val InstagramCardTitle = Color(0xFFAF0069)

/** 프라이빗 탭 상단 액션 카드 높이. */
private val ActionCardHeight = 72.dp

@Composable
internal fun CommunityTabContent(
    state: MyMapsState,
    edit: CollectionEditState?,
    onRetryClick: () -> Unit,
    onMapClick: (MyMap) -> Unit,
    onEditClick: () -> Unit,
    onMapMove: (mapId: Long, targetId: Long) -> Unit,
) {
    MapsStateContent(
        state = state,
        emptyMessage = "아직 참여한 지도가 없어요",
        onRetryClick = onRetryClick,
    ) { maps ->
        // 공식지도와 커뮤니티 지도는 서버가 한 줄 순서로 둔다. 서로의 자리로도 끌어 옮긴다.
        val reorder = rememberMapReorderState(
            ids = maps.map { map -> map.id },
            spacing = CardSpacing,
            onMove = onMapMove,
        )
        // 시안 `1974:7318`: 목록 오른쪽 위에 「편집」, 목록과 12. 폭을 채워야 「편집」이 오른쪽 끝에 붙는다.
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EditToggle(edit = edit, onClick = onEditClick, modifier = Modifier.align(Alignment.End))
            Column(verticalArrangement = Arrangement.spacedBy(CardSpacing)) {
                // 순서를 바꾸면 카드가 자리째 옮겨가야 끌던 손잡이의 동작이 끊기지 않는다.
                maps.forEach { map ->
                    key(map.id) {
                        MyMapCard(
                            map = map,
                            uiModel = map.toCommunityUiModel(),
                            edit = edit,
                            reorder = reorder,
                            onClick = { onMapClick(map) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun PrivateTabContent(
    state: MyMapsState,
    edit: CollectionEditState?,
    onRetryClick: () -> Unit,
    onInstagramImportClick: () -> Unit,
    onMapShareImportClick: () -> Unit,
    onMapClick: (MyMap) -> Unit,
    onEditClick: () -> Unit,
    onMapMove: (mapId: Long, targetId: Long) -> Unit,
) {
    // 액션 카드는 목록 상태와 무관하게 늘 보인다. 목록이 비었을 때야말로 만들 진입점이 필요하다.
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PrivateActionCards(
            onInstagramImportClick = onInstagramImportClick,
            onMapShareImportClick = onMapShareImportClick,
        )

        // 목록이 비어도 「프라이빗 지도」 제목과 「편집」은 보이고, 그 아래에서 비었다고 알린다.
        MapsStateContent(
            state = state,
            emptyMessage = null,
            onRetryClick = onRetryClick,
        ) { maps ->
            val sections = maps.splitPersonal()
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                // 나만의 지도는 나갈 대상이 아니라 편집해도 체크박스를 달지 않는다.
                PrivateMapSection("나만의 지도", sections.personal, edit = null, onMapClick = onMapClick)
                // 시안 `1976:8630`: 「편집」은 이 섹션 제목 아래, 목록 바로 위에 있다.
                PrivateMapSection(
                    title = "프라이빗 지도",
                    maps = sections.others,
                    edit = edit,
                    onMapClick = onMapClick,
                    onMapMove = onMapMove,
                    emptyMessage = "참여하고 있는 프라이빗 지도가 없습니다",
                    editToggle = {
                        EditToggle(edit = edit, onClick = onEditClick, modifier = Modifier.align(Alignment.End))
                    },
                )
            }
        }
    }
}

/**
 * 목록 자리의 로딩·오류·빈 상태를 한곳에서 그린다.
 *
 * 세 상태 모두 같은 높이를 차지해, 상태가 바뀔 때 화면이 튀지 않는다.
 */
@Composable
internal fun MapsStateContent(
    state: MyMapsState,
    /** null 이면 목록이 비어도 [content] 를 그린다. 빈 상태를 화면이 직접 그릴 때다. */
    emptyMessage: String?,
    onRetryClick: () -> Unit,
    content: @Composable (List<MyMap>) -> Unit,
) {
    when (state) {
        MyMapsState.Loading -> MoaMapListPlaceholder { CircularProgressIndicator() }

        is MyMapsState.Error -> MoaMapListPlaceholder {
            MoaMapErrorNotice(message = state.message, onRetryClick = onRetryClick)
        }

        is MyMapsState.Success -> {
            if (state.maps.isEmpty() && emptyMessage != null) {
                MoaMapListPlaceholder {
                    Text(
                        text = emptyMessage,
                        style = MoaMapTheme.typography.body2,
                        color = MoaMapTheme.colors.textAssistive,
                    )
                }
            } else {
                content(state.maps)
            }
        }
    }
}

@Composable
private fun PrivateActionCards(
    onInstagramImportClick: () -> Unit,
    onMapShareImportClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ImportActionCard(
            iconRes = R.drawable.ic_instagram_logo,
            title = "인스타그램",
            subtitle = "장소 찾기",
            backgroundColor = InstagramCardBackground,
            titleColor = InstagramCardTitle,
            // 브랜드 로고라 원본 색을 그대로 쓴다.
            iconTint = Color.Unspecified,
            onClick = onInstagramImportClick,
            modifier = Modifier.weight(1f),
        )
        ImportActionCard(
            iconRes = R.drawable.ic_map,
            title = "외부 지도",
            subtitle = "불러오기",
            backgroundColor = MoaMapPrimitiveColors.Yellow50,
            titleColor = MoaMapPrimitiveColors.Yellow800,
            iconTint = MoaMapPrimitiveColors.Yellow500,
            onClick = onMapShareImportClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun ImportActionCard(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    backgroundColor: Color,
    titleColor: Color,
    iconTint: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    ShadowedSurface(
        modifier = modifier.height(ActionCardHeight),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                // 두 줄 사이 간격은 피그마에도 gap 이 없고 line height 로만 벌어진다.
                // 기본 Trim 을 끄지 않으면 디자인보다 줄이 붙는다.
                Text(
                    text = title,
                    style = MoaMapTheme.typography.subtitle2.withDesignLineHeight(),
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MoaMapTheme.typography.body1.withDesignLineHeight(),
                    color = MoaMapTheme.colors.textNormal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_arrow_outward),
                contentDescription = null,
                tint = MoaMapPrimitiveColors.Black,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** 목록이 비어도 제목은 남긴다. 자리가 사라졌다 나타나면 화면 구성이 바뀌어 보인다. */
@Composable
private fun PrivateMapSection(
    title: String,
    maps: List<MyMap>,
    edit: CollectionEditState?,
    onMapClick: (MyMap) -> Unit,
    onMapMove: (mapId: Long, targetId: Long) -> Unit = { _, _ -> },
    /** 있으면 지도가 없어도 섹션을 그리고 이 문구로 비었다고 알린다. 없으면 섹션째 숨긴다. */
    emptyMessage: String? = null,
    editToggle: (@Composable ColumnScope.() -> Unit)? = null,
) {
    // 제목만 떠 있고 아래가 비어 있으면 못 불러온 것처럼 보인다.
    if (maps.isEmpty() && emptyMessage == null) return

    val reorder = rememberMapReorderState(ids = maps.map { map -> map.id }, spacing = CardSpacing, onMove = onMapMove)
    // 시안: 제목과 목록 사이 12, 「편집」과 카드·카드끼리는 8.
    // 폭을 채워야 「편집」이 오른쪽 끝에 붙는다. 카드가 없으면 안내 문구 폭으로 줄어든다.
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MoaMapTheme.typography.title2,
            color = MoaMapTheme.colors.textNormal,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CardSpacing),
        ) {
            editToggle?.invoke(this)
            if (maps.isEmpty() && emptyMessage != null) {
                Text(
                    text = emptyMessage,
                    style = MoaMapTheme.typography.body2,
                    color = MoaMapTheme.colors.textAssistive,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            // 순서를 바꾸면 카드가 자리째 옮겨가야 끌던 손잡이의 동작이 끊기지 않는다.
            maps.forEach { map ->
                key(map.id) {
                    MyMapCard(
                        map = map,
                        uiModel = map.toPrivateUiModel(),
                        edit = edit,
                        reorder = reorder,
                        onClick = { onMapClick(map) },
                    )
                }
            }
        }
    }
}

/** 「편집」·「완료」. 시안에서 목록 오른쪽 위에 글자만 있다. */
@Composable
private fun EditToggle(edit: CollectionEditState?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = if (edit != null) "완료" else "편집",
        style = MoaMapTheme.typography.button1,
        color = MoaMapTheme.colors.textNormal,
        modifier = modifier.clickable(enabled = edit?.leaving != true, onClick = onClick),
    )
}

/**
 * 모음 목록의 카드. 시안 `1974:7318`·`1974:7468`.
 *
 * 편집 중에만 순서 손잡이가 보이고, 손잡이를 끌어 순서를 바꾼다. 바꾼 순서는 편집을 마칠 때 저장한다.
 * 편집 중에는 체크박스가 붙고 누르는 동안 회색이 된다. 고를 수 없는 지도는 체크박스가 회색이다.
 */
@Composable
private fun MyMapCard(
    map: MyMap,
    uiModel: CollectionMapUiModel,
    edit: CollectionEditState?,
    reorder: MapReorderState,
    onClick: () -> Unit,
) {
    val selected = edit != null && map.id in edit.selected
    CollectionMapCard(
        map = uiModel,
        onClick = onClick,
        modifier = Modifier.reorderableItem(reorder, map.id),
        border = selectedCardBorder(selected),
        reorderable = true,
        showDragHandle = edit != null,
        dragHandleModifier = Modifier.reorderHandle(reorder, map.id),
        pressFeedback = edit != null,
        trailingContent = edit?.let { current ->
            {
                // 확인 중인 지도는 고를 수 있는 모양으로 둔다. 곧 대부분 고를 수 있게 된다.
                val eligibility = current.eligibilityOf(map.id)
                PlaceImportCheckBox(
                    checked = selected,
                    enabled = eligibility == LeaveEligibility.Allowed ||
                        eligibility == LeaveEligibility.Checking,
                )
            }
        },
    )
}
