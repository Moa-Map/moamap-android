package com.moamap.app.feature.mapdetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.ButtonShadowBlurRadius
import com.moamap.app.core.designsystem.component.ButtonShadowColor
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight

private val PlacePhotoHeight = 175.dp
private val SheetPhotoHeight = 167.dp
private val PlacePhotoShape = RoundedCornerShape(4.dp)
private val PlaceTagShape = RoundedCornerShape(100.dp)
private val PlaceActionShape = RoundedCornerShape(8.dp)
private val PlaceActionHeight = 44.dp

/** 이름 줄 오른쪽 끝 인스타그램 버튼. 버튼이 있으면 이름·태그는 이 폭에 간격 8 을 더한 만큼 비켜 선다. */
private val InstagramButtonSize = 36.dp
private val InstagramButtonClearance = InstagramButtonSize + 8.dp

/**
 * 「나만의 지도에 추가」 버튼 상태.
 *
 * 결과 안내를 버튼 아래에 띄운다. 시트 위로는 화면의 스낵바가 보이지 않는다.
 */
@Immutable
internal data class PersonalMapActionUiModel(
    val adding: Boolean = false,
    val message: String? = null,
    val failed: Boolean = false,
)

internal fun likeIconRes(liked: Boolean): Int = if (liked) {
    R.drawable.ic_favorite_filled
} else {
    R.drawable.ic_favorite_outline
}

/**
 * 장소 정보. 시안 「장소 상세 설명」(10/3 장소 상세 페이지·사진 없는 시트).
 *
 * 이름·태그·위치 → 사진(있을 때만) → 설명·하트·신고하기 순서다. 사진이 없는 장소는 사진 칸만
 * 빠지고 나머지 배치는 같다. 인스타그램에서 가져온 장소는 이름 줄 오른쪽 끝에 인스타그램 버튼이
 * 겹쳐 선다([onInstagramClick] 가 null 이면 없다).
 */
@Composable
internal fun PlaceHeader(
    place: PlaceUiModel,
    onLikeClick: () -> Unit,
    onInstagramClick: (() -> Unit)?,
    showsReactions: Boolean,
) {
    val clearance = if (onInstagramClick != null) InstagramButtonClearance else 0.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PlaceName(name = place.name, endClearance = clearance)
                if (place.tags.isNotEmpty()) {
                    PlaceTagRow(
                        tags = place.tags,
                        modifier = Modifier.padding(end = clearance),
                    )
                }
                if (place.address.isNotBlank()) PlaceLocation(address = place.address)
            }

            onInstagramClick?.let { onClick ->
                PlaceInstagramButton(onClick = onClick, modifier = Modifier.align(Alignment.TopEnd))
            }
        }

        place.photoUrl?.let { url -> PlacePhoto(url = url, height = PlacePhotoHeight) }

        val showsDescription = place.description.isNotBlank()
        if (showsDescription || showsReactions) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showsDescription) {
                    Text(
                        text = place.description,
                        style = MoaMapTheme.typography.body2.withDesignLineHeight(),
                        color = MoaMapTheme.colors.textAlternative,
                    )
                }
                if (showsReactions) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlaceLikeButton(place = place, onClick = onLikeClick)
                        // 신고는 아직 기능이 없어 누르지 않는다(사용자 결정) - 시안의 밑줄 글자만 둔다.
                        Text(
                            text = "신고하기",
                            style = MoaMapTheme.typography.body2.copy(textDecoration = TextDecoration.Underline),
                            color = MoaMapTheme.colors.textAlternative,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 사진 있는 장소의 시트 배치. 시안 「10/3」 시트(사진 있음·사진 있음 + 인스타 버튼).
 *
 * 이름 → 태그 · 주소 한 줄 → 하트 → 사진(167) → 버튼 줄(간격 4). 설명·신고하기·위치 아이콘은 없다.
 * 거의 끝까지 끌어 올려 페이지가 되면 [PlaceHeader] 배치로 겹쳐 바뀐다.
 */
@Composable
internal fun PlaceCompactHeader(
    place: PlaceUiModel,
    onLikeClick: () -> Unit,
    onInstagramClick: (() -> Unit)?,
    showsReactions: Boolean,
    actions: @Composable () -> Unit,
) {
    val clearance = if (onInstagramClick != null) InstagramButtonClearance else 0.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PlaceName(name = place.name, endClearance = clearance)
                PlaceTagAddressLine(
                    tags = place.tags,
                    address = place.address,
                    modifier = Modifier.padding(end = clearance),
                )
                if (showsReactions) PlaceLikeButton(place = place, onClick = onLikeClick)
            }

            onInstagramClick?.let { onClick ->
                PlaceInstagramButton(onClick = onClick, modifier = Modifier.align(Alignment.TopEnd))
            }
        }

        place.photoUrl?.let { url -> PlacePhoto(url = url, height = SheetPhotoHeight) }
        actions()
    }
}

@Composable
private fun PlaceName(name: String, endClearance: Dp) {
    Text(
        text = name,
        style = MoaMapTheme.typography.subtitle1.withDesignLineHeight(),
        color = MoaMapTheme.colors.textNormal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(end = endClearance),
    )
}

/** 등록할 때 단 태그. 이 줄이 넘치면 다음 줄로 내린다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaceTagRow(tags: List<String>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tags.forEach { tag -> PlaceTagChip(tag = tag) }
    }
}

/**
 * 시트의 「태그 · 주소」 한 줄. 넘치면 주소부터 말줄임(사용자 결정), 태그만으로 넘치면 넘친 칩은
 * 잘린다. 태그가 없으면 「·」 없이 주소만 둔다.
 */
@Composable
private fun PlaceTagAddressLine(tags: List<String>, address: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tags.forEach { tag -> PlaceTagChip(tag = tag) }
        if (address.isNotBlank()) {
            Text(
                text = if (tags.isEmpty()) address else "· $address",
                style = MoaMapTheme.typography.body2.withDesignLineHeight(),
                color = MoaMapTheme.colors.textAlternative,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // 칩을 먼저 놓고 남은 폭만 쓴다.
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

@Composable
private fun PlaceTagChip(tag: String) {
    Text(
        text = tag,
        style = MoaMapTheme.typography.caption0,
        color = MoaMapPrimitiveColors.Yellow900,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .background(color = MoaMapPrimitiveColors.Yellow50, shape = PlaceTagShape)
            .border(width = 1.dp, color = MoaMapPrimitiveColors.Yellow500, shape = PlaceTagShape)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
private fun PlaceLocation(address: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_location),
            contentDescription = null,
            tint = MoaMapTheme.colors.textAlternative,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = address,
            style = MoaMapTheme.typography.body2.withDesignLineHeight(),
            color = MoaMapTheme.colors.textAlternative,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 장소 사진 첫 장. 폭 가득, 페이지 175·시트 167. */
@Composable
private fun PlacePhoto(url: String, height: Dp) {
    val placeholder = painterResource(R.drawable.img_photo_placeholder)
    AsyncImage(
        model = url,
        contentDescription = "장소 사진",
        placeholder = placeholder,
        error = placeholder,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(PlacePhotoShape),
    )
}

/** 하트 28 + 수. 누르면 바로 바뀌고 서버로 확정한다. */
@Composable
private fun PlaceLikeButton(place: PlaceUiModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = if (place.liked) "하트 취소하기" else "하트 누르기"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(likeIconRes(place.liked)),
            contentDescription = null,
            tint = if (place.liked) MoaMapTheme.colors.statusAlert else MoaMapPrimitiveColors.Gray100,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = place.likeCount.toString(),
            style = MoaMapTheme.typography.caption0,
            color = MoaMapTheme.colors.textAlternative,
        )
    }
}

/**
 * 인스타그램 원본 게시물로 가는 원 버튼(36). 인스타그램 링크로 가져온 장소에만 둔다(사용자 결정).
 * 누르는 칸은 48 이고 그림은 그 가운데라, 칸을 바깥으로 6 씩 내밀어 그림이 시안 자리(이름 줄 오른쪽
 * 끝)에 선다.
 */
@Composable
private fun PlaceInstagramButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .offset(x = 6.dp, y = (-6).dp)
            .size(48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "인스타그램 게시물 보기" },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.img_place_instagram_link),
            contentDescription = null,
            modifier = Modifier.size(InstagramButtonSize),
        )
    }
}

@Composable
internal fun PlaceActions(
    personalMapAction: PersonalMapActionUiModel?,
    onAddToPersonalMapClick: () -> Unit,
    onShowOnMapClick: () -> Unit,
    /** 두 버튼 사이. 시안은 페이지 8, 사진 있는 시트 4. */
    buttonGap: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(buttonGap),
        ) {
            if (personalMapAction != null) {
                PlaceActionButton(
                    label = "나만의 지도에 추가",
                    iconRes = R.drawable.ic_add,
                    containerColor = MoaMapPrimitiveColors.Blue500,
                    contentColor = MoaMapPrimitiveColors.White,
                    loading = personalMapAction.adding,
                    onClick = onAddToPersonalMapClick,
                    modifier = Modifier.weight(1f),
                )
            }
            PlaceActionButton(
                label = "지도 보기",
                iconRes = R.drawable.ic_map,
                containerColor = MoaMapPrimitiveColors.Gray200,
                contentColor = MoaMapPrimitiveColors.White,
                loading = false,
                onClick = onShowOnMapClick,
                modifier = Modifier.weight(1f),
            )
        }

        personalMapAction?.message?.let { message ->
            Text(
                text = message,
                style = MoaMapTheme.typography.caption0,
                // 성공은 다른 안내 문구와 같은 회색, 실패만 빨강으로 눈에 띄게 한다.
                color = if (personalMapAction.failed) {
                    MoaMapTheme.colors.statusAlert
                } else {
                    MoaMapTheme.colors.textAssistive
                },
            )
        }
    }
}

@Composable
private fun PlaceActionButton(
    label: String,
    iconRes: Int,
    containerColor: Color,
    contentColor: Color,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShadowedSurface(
        modifier = modifier.height(PlaceActionHeight),
        shape = PlaceActionShape,
        color = containerColor,
        shadowBlurRadius = ButtonShadowBlurRadius,
        shadowColor = ButtonShadowColor,
        onClick = if (loading) null else onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    color = contentColor,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = label,
                style = MoaMapTheme.typography.button2,
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}
