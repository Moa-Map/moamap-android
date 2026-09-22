package com.moamap.app.feature.mapdetail.presentation.posts

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.core.designsystem.modifier.dismissKeyboardOnBackgroundTap
import com.moamap.app.R
import androidx.activity.compose.BackHandler
import com.moamap.app.core.common.gallery.GalleryPickerScreen
import com.moamap.app.core.common.imagepicker.rememberImagePickerController
import com.moamap.app.core.common.imagepicker.rememberImagePickerState
import com.moamap.app.core.common.upload.ALLOWED_IMAGE_CONTENT_TYPES
import com.moamap.app.core.designsystem.component.ErrorSnackbar
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.mapdetail.MapDetailTopBarHeight
import com.moamap.app.feature.mapdetail.domain.model.MapPlace
import com.moamap.app.feature.mapdetail.presentation.addplace.PhotoPicker

/** 촬영본을 두는 캐시 폴더. `res/xml/profile_image_paths.xml` 에 같은 이름이 있어야 카메라가 뜬다. */
private const val POST_PHOTO_CACHE_DIRECTORY = "post_photos"

private val InputShape = RoundedCornerShape(12.dp)
private val SubmitShape = RoundedCornerShape(8.dp)

/** 본문 입력줄의 최소 높이. 여백·글자 수와 합쳐 시안의 입력칸(101dp)과 맞춘다. 글이 길어지면 늘어난다. */
private val ContentTextMinHeight = 56.dp

/**
 * 새 게시물. 로그 탭의 `+` 버튼에서 들어온다.
 *
 * 지도 상세 위에 겹쳐 그린다. 태그할 장소는 지도 상세가 이미 불러온 목록([places])에서 고르고,
 * 올리고 나면 뒤의 로그 탭이 목록을 다시 읽어야 해서 같은 화면 안에 있는 편이 간단하다.
 *
 * 사진은 시안대로 앱 안 갤러리([GalleryPickerScreen])에서 고른다. 이 화면 위에 한 장 더 덮고,
 * 고른 사진을 받아 돌아온다. 촬영은 갤러리 첫 칸에서 이어 간다.
 */
@Composable
internal fun MapPostCreateScreen(
    state: MapPostCreateUiState,
    places: List<MapPlace>,
    onContentChange: (String) -> Unit,
    onAddPhoto: (Uri) -> Unit,
    onRemovePhoto: (Uri) -> Unit,
    onAddPlace: (MapPlace) -> Unit,
    onRemovePlace: (Long) -> Unit,
    onSubmitClick: () -> Unit,
    onErrorShown: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pickerState = rememberImagePickerState()
    val pickerController = rememberImagePickerController(
        state = pickerState,
        cacheDirectoryName = POST_PHOTO_CACHE_DIRECTORY,
        fileNamePrefix = "post",
        // 서버가 받지 않는 형식은 갤러리에서부터 보이지 않게 한다.
        mimeTypes = ALLOWED_IMAGE_CONTENT_TYPES.toTypedArray(),
        onImageSelected = onAddPhoto,
    )
    var placeSheetVisible by rememberSaveable { mutableStateOf(false) }
    var galleryVisible by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary)
            // 뒤에 깔린 지도로 터치가 새지 않게 빈 자리의 탭을 여기서 받는다.
            .dismissKeyboardOnBackgroundTap()
            .statusBarsPadding()
            .imePadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PostCreateTopBar(onBackClick = onBackClick)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding)
                    .padding(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PhotoPicker(
                    photos = state.photos,
                    // 올리는 중에는 목록을 바꾸지 못하게 한다. 올린 주소와 목록이 어긋난다.
                    canAddPhoto = state.canAddPhoto && !state.submitting,
                    onAddPhotoClick = { if (!state.submitting) galleryVisible = true },
                    onRemovePhoto = { uri -> if (!state.submitting) onRemovePhoto(uri) },
                )

                ContentField(
                    value = state.content,
                    enabled = !state.submitting,
                    onValueChange = onContentChange,
                )

                state.places.forEach { place ->
                    SelectedPlaceRow(
                        place = place,
                        onRemoveClick = { if (!state.submitting) onRemovePlace(place.placeId) },
                    )
                }

                if (state.canAddPlace) {
                    AddPlaceRow(onClick = { if (!state.submitting) placeSheetVisible = true })
                }
            }

            Column(modifier = Modifier.navigationBarsPadding()) {
                ErrorSnackbar(message = state.errorMessage, onShown = onErrorShown)
                SubmitButton(
                    enabled = state.canSubmit,
                    submitting = state.submitting,
                    onClick = onSubmitClick,
                    modifier = Modifier.padding(
                        start = MoaMapDimens.ScreenHorizontalPadding,
                        end = MoaMapDimens.ScreenHorizontalPadding,
                        bottom = 16.dp,
                    ),
                )
            }
        }

        if (galleryVisible) {
            GalleryPickerScreen(
                // 남은 자리만큼만 고르게 한다. 다섯 장을 채우면 더 고를 수 없다.
                maxSelectable = MAX_POST_PHOTOS - state.photos.size,
                onCameraClick = {
                    galleryVisible = false
                    pickerController.requestCamera()
                },
                onConfirm = { photos ->
                    galleryVisible = false
                    photos.forEach(onAddPhoto)
                },
                onBackClick = { galleryVisible = false },
            )
        }
    }

    BackHandler(enabled = galleryVisible) { galleryVisible = false }

    if (placeSheetVisible) {
        PlacePickerSheet(
            // 이미 고른 장소는 목록에서 뺀다. 같은 장소를 두 번 태그하면 서버가 거절한다.
            places = places.filterNot { place -> state.places.any { it.placeId == place.id } },
            onPlaceClick = { place ->
                placeSheetVisible = false
                onAddPlace(place)
            },
            onDismiss = { placeSheetVisible = false },
        )
    }
}

@Composable
private fun PostCreateTopBar(onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MapDetailTopBarHeight),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .size(48.dp)
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = "뒤로가기",
                tint = MoaMapTheme.colors.textNormal,
                modifier = Modifier.size(24.dp),
            )
        }

        Text(
            text = "새 게시물",
            style = MoaMapTheme.typography.title3,
            color = MoaMapTheme.colors.textNormal,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun ContentField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    ShadowedSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = InputShape,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                textStyle = MoaMapTheme.typography.body2.copy(color = MoaMapTheme.colors.textNormal),
                cursorBrush = SolidColor(MoaMapTheme.colors.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ContentTextMinHeight),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = "텍스트를 입력해주세요.",
                                style = MoaMapTheme.typography.body2,
                                color = MoaMapTheme.colors.textAssistive,
                            )
                        }
                        inner()
                    }
                },
            )
            // 한도에 닿으면 더 써지지 않는다. 왜 안 써지는지 보이게 남은 양을 적는다.
            Text(
                text = "${value.length}/$MAX_POST_CONTENT_LENGTH",
                style = MoaMapTheme.typography.caption0,
                color = MoaMapTheme.colors.textAssistive,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

@Composable
private fun AddPlaceRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_location),
            contentDescription = null,
            tint = MoaMapTheme.colors.textNormal,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = "장소 추가",
            style = MoaMapTheme.typography.subtitle2,
            color = MoaMapTheme.colors.textNormal,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        )
        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = MoaMapTheme.colors.textNormal,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun SelectedPlaceRow(place: SelectedPostPlace, onRemoveClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_location),
            contentDescription = null,
            tint = MoaMapTheme.colors.textNormal,
            modifier = Modifier.size(24.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = place.name,
                style = MoaMapTheme.typography.subtitle4,
                color = MoaMapTheme.colors.textNormal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (place.address.isNotEmpty()) {
                Text(
                    text = place.address,
                    style = MoaMapTheme.typography.caption0,
                    color = MoaMapTheme.colors.textAssistive,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable(onClick = onRemoveClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "장소 빼기",
                tint = MoaMapTheme.colors.textNormal,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun SubmitButton(
    enabled: Boolean,
    submitting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(SubmitShape)
            .background(if (enabled || submitting) MoaMapTheme.colors.primary else MoaMapPrimitiveColors.Gray200)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (submitting) {
            CircularProgressIndicator(
                color = MoaMapTheme.colors.textWhite,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp),
            )
        } else {
            Text(
                text = "공유하기",
                style = MoaMapTheme.typography.button0,
                color = MoaMapTheme.colors.textWhite,
            )
        }
    }
}

/** 이 지도에 등록된 장소에서 고른다. 게시물은 지도 안의 장소만 태그할 수 있다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlacePickerSheet(
    places: List<MapPlace>,
    onPlaceClick: (MapPlace) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MoaMapTheme.colors.backgroundSecondary,
        tonalElevation = 0.dp,
    ) {
        Text(
            text = "장소 추가",
            style = MoaMapTheme.typography.title3,
            color = MoaMapTheme.colors.textNormal,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 12.dp),
        )

        if (places.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 60.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "태그할 수 있는 장소가 없어요",
                    style = MoaMapTheme.typography.body2,
                    color = MoaMapTheme.colors.textAssistive,
                )
            }
            return@ModalBottomSheet
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(places, key = { place -> place.id }) { place ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlaceClick(place) }
                        .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = place.name,
                        style = MoaMapTheme.typography.subtitle4,
                        color = MoaMapTheme.colors.textNormal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (place.address.isNotEmpty()) {
                        Text(
                            text = place.address,
                            style = MoaMapTheme.typography.caption0,
                            color = MoaMapTheme.colors.textAssistive,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MapPostCreateScreenPreview() {
    MoaMapTheme {
        MapPostCreateScreen(
            state = MapPostCreateUiState(
                content = "성수 카페 다녀왔어요",
                places = listOf(
                    SelectedPostPlace(placeId = 1, name = "블루보틀 성수점", address = "서울 성동구 아차산로 7"),
                ),
            ),
            places = emptyList(),
            onContentChange = {},
            onAddPhoto = {},
            onRemovePhoto = {},
            onAddPlace = {},
            onRemovePlace = {},
            onSubmitClick = {},
            onErrorShown = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MapPostCreateScreenEmptyPreview() {
    MoaMapTheme {
        MapPostCreateScreen(
            state = MapPostCreateUiState(),
            places = emptyList(),
            onContentChange = {},
            onAddPhoto = {},
            onRemovePhoto = {},
            onAddPlace = {},
            onRemovePlace = {},
            onSubmitClick = {},
            onErrorShown = {},
            onBackClick = {},
        )
    }
}
