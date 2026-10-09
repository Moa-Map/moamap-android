package com.moamap.app.feature.collection.presentation.createmap

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.moamap.app.core.common.imagepicker.rememberImagePickerController
import com.moamap.app.core.common.imagepicker.rememberImagePickerState
import com.moamap.app.core.common.upload.ALLOWED_IMAGE_CONTENT_TYPES
import com.moamap.app.core.designsystem.component.ImageSourceMenu
import com.moamap.app.core.designsystem.component.MapFormSectionTitle
import com.moamap.app.core.designsystem.component.MapPhotoField
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import dev.chrisbanes.haze.HazeState

private val VisibilityCardShape = RoundedCornerShape(16.dp)

private val VisibilityCardHeight = 113.dp

/** 촬영본이 쌓이는 캐시 위치. `res/xml/profile_image_paths.xml` 의 `cache-path` 와 맞춰야 한다. */
private const val MapImageCacheDirectory = "map_images"
private const val MapImageFilePrefix = "map"

/**
 * 갤러리에 보일 형식.
 *
 * 발급 전에 거르는 형식과 같은 값을 써야 한다. 고르고 나서 거절당하지 않도록 선택기에서
 * 미리 좁히는 것뿐이라, 목록을 따로 두면 서버 계약이 바뀔 때 조용히 어긋난다.
 */
private val CoverImageMimeTypes = ALLOWED_IMAGE_CONTENT_TYPES.toTypedArray()

/**
 * 지도 사진 칸 + 누르면 뜨는 카메라·갤러리 메뉴. 새 지도 만들기와 지도 정보 수정이 함께 쓴다.
 *
 * @param enabled false 면 눌러도 메뉴를 띄우지 않는다. 올리는 중에 고른 값을 버리면 왜 안
 * 바뀌는지 알 수 없어서 아예 잠근다.
 * @param hazeState 메뉴가 바탕으로 흐릴 화면.
 */
@Composable
internal fun MapCoverPickerField(
    photo: String?,
    onPhotoSelected: (String) -> Unit,
    enabled: Boolean,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    showsEditOverlay: Boolean = false,
) {
    val pickerState = rememberImagePickerState()
    val pickerController = rememberImagePickerController(
        state = pickerState,
        cacheDirectoryName = MapImageCacheDirectory,
        fileNamePrefix = MapImageFilePrefix,
        mimeTypes = CoverImageMimeTypes,
        onImageSelected = { uri -> onPhotoSelected(uri.toString()) },
    )

    Box(modifier = modifier) {
        MapPhotoField(
            photo = photo,
            onClick = { if (enabled) pickerState.showSourceMenu() },
            showsEditOverlay = showsEditOverlay,
        )

        if (pickerState.isSourceMenuVisible) {
            Popup(
                alignment = Alignment.BottomCenter,
                onDismissRequest = pickerState::dismissSourceMenu,
                properties = PopupProperties(focusable = true),
            ) {
                ImageSourceMenu(
                    onCameraClick = pickerController::requestCamera,
                    onGalleryClick = pickerController::requestGallery,
                    hazeState = hazeState,
                )
            }
        }
    }
}

/**
 * 공개 범위 카드 한 장.
 *
 * 고르면 파란 배경·테두리로 바뀌고 글자도 굵어진다.
 */
@Composable
internal fun VisibilityCard(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) {
        MoaMapPrimitiveColors.Blue700
    } else {
        MoaMapTheme.colors.textAssistive
    }

    ShadowedSurface(
        modifier = modifier.height(VisibilityCardHeight),
        shape = VisibilityCardShape,
        color = if (selected) MoaMapPrimitiveColors.Blue50 else MoaMapPrimitiveColors.White,
        border = if (selected) {
            BorderStroke(1.dp, MoaMapPrimitiveColors.Blue500)
        } else {
            null
        },
        onClick = onClick,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(11.dp, Alignment.CenterVertically),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                // 고르지 않은 카드의 아이콘은 글자보다 진하다.
                tint = if (selected) {
                    MoaMapPrimitiveColors.Blue700
                } else {
                    MoaMapTheme.colors.textAlternative
                },
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = title,
                // 시안처럼 줄 높이(21)를 다 써야 아이콘·부제와의 간격 11 이 시안과 같아진다.
                style = if (selected) {
                    MoaMapTheme.typography.body3
                } else {
                    MoaMapTheme.typography.body2
                }.withDesignLineHeight(),
                color = contentColor,
            )
            Text(
                text = subtitle,
                style = MoaMapTheme.typography.caption0.withDesignLineHeight(),
                color = contentColor,
            )
        }
    }
}

/** URL 가져오기 설정의 UI. 전체 행을 눌러도 스위치를 전환할 수 있다. */
@Composable
internal fun UrlImportPermissionSection(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MapFormSectionTitle("URL로 장소 추가 허용하기")
            Text(
                text = "URL을 붙여넣어 장소를 추가할 수 있어요",
                style = MoaMapTheme.typography.body2.withDesignLineHeight(),
                color = MoaMapTheme.colors.textAssistive,
            )
        }
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 24.dp)
                .clip(CircleShape)
                .background(if (checked) MoaMapTheme.colors.primary else Color(0xFFB1B3B4))
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(MoaMapPrimitiveColors.White, CircleShape),
            )
        }
    }
}
