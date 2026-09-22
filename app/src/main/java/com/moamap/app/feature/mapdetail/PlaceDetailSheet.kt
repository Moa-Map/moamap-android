package com.moamap.app.feature.mapdetail

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.moamap.app.core.designsystem.modifier.dismissKeyboardOnBackgroundTap
import com.moamap.app.R
import com.moamap.app.core.common.imagepicker.rememberImagePickerController
import com.moamap.app.core.common.imagepicker.rememberImagePickerState
import com.moamap.app.core.common.upload.ALLOWED_IMAGE_CONTENT_TYPES
import com.moamap.app.core.designsystem.component.ButtonShadowBlurRadius
import com.moamap.app.core.designsystem.component.ButtonShadowColor
import com.moamap.app.core.designsystem.component.CardShadowBlurRadius
import com.moamap.app.core.designsystem.component.CardShadowColor
import com.moamap.app.core.designsystem.component.ImageSourceMenu
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.mapdetail.presentation.addplace.PLACE_PHOTO_CACHE_DIRECTORY

private val PlaceDetailSheetShape = RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)
private val PlaceImageShape = RoundedCornerShape(16.dp)
private val PlaceCategoryShape = RoundedCornerShape(100.dp)
private val PlaceActionShape = RoundedCornerShape(8.dp)
private val ReviewInputShape = RoundedCornerShape(100.dp)
private val ReviewPhotoShape = RoundedCornerShape(8.dp)
private val PlaceDetailGrabberShape = RoundedCornerShape(100.dp)

/** 후기 자리의 로딩·오류·빈 상태가 함께 쓰는 높이. 상태가 바뀌어도 시트가 튀지 않는다. */
private val ReviewPlaceholderHeight = 140.dp

private val PlaceActionHeight = 44.dp
private val ReviewPhotoSize = 96.dp
private val ReviewDraftPhotoSize = 64.dp

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaceDetailSheet(
    place: PlaceUiModel,
    reviews: PlaceReviewsUiModel,
    onDismiss: () -> Unit,
    onExternalLinkClick: () -> Unit,
    onRetryReviews: () -> Unit = {},
    /** null 이면 「나만의 지도에 추가」를 띄우지 않는다. 나만의 지도를 보고 있을 때다. */
    personalMapAction: PersonalMapActionUiModel? = null,
    onAddToPersonalMapClick: () -> Unit = {},
    onSubmitReview: ((reviewText: String, photo: Uri?) -> Boolean)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = PlaceDetailSheetShape,
        containerColor = MoaMapTheme.colors.backgroundSecondary,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(25.dp)
                    .dismissKeyboardOnBackgroundTap(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 35.dp, height = 5.dp)
                        .background(
                            color = MoaMapPrimitiveColors.Gray100,
                            shape = PlaceDetailGrabberShape,
                        ),
                )
            }
        },
    ) {
        PlaceDetailSheetContent(
            place = place,
            reviews = reviews,
            onDismiss = onDismiss,
            onExternalLinkClick = onExternalLinkClick,
            onRetryReviews = onRetryReviews,
            personalMapAction = personalMapAction,
            onAddToPersonalMapClick = onAddToPersonalMapClick,
            onSubmitReview = onSubmitReview,
        )
    }
}

internal fun favoriteIconRes(favorite: Boolean): Int = if (favorite) {
    R.drawable.ic_favorite_filled
} else {
    R.drawable.ic_favorite_outline
}

internal fun trySubmitReview(
    reviewText: String,
    photo: Uri?,
    onSubmitReview: ((reviewText: String, photo: Uri?) -> Boolean)?,
): Boolean = onSubmitReview?.invoke(reviewText, photo) == true

/**
 * 시트 내용. 입력창은 목록과 함께 스크롤되지 않고 아래에 붙는다(시안).
 *
 * 카메라·갤러리 고르기는 시트 안에 겹쳐 띄운다. Popup 으로 띄우면 시트 밖에 그려져, 바깥을
 * 눌렀을 때 시트까지 함께 닫힌다.
 */
@Composable
private fun PlaceDetailSheetContent(
    place: PlaceUiModel,
    reviews: PlaceReviewsUiModel,
    onDismiss: () -> Unit,
    onExternalLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRetryReviews: () -> Unit = {},
    personalMapAction: PersonalMapActionUiModel? = null,
    onAddToPersonalMapClick: () -> Unit = {},
    onSubmitReview: ((reviewText: String, photo: Uri?) -> Boolean)? = null,
) {
    var reviewPhoto by rememberSaveable(place.id) { mutableStateOf<Uri?>(null) }
    val pickerState = rememberImagePickerState()
    val pickerController = rememberImagePickerController(
        state = pickerState,
        cacheDirectoryName = PLACE_PHOTO_CACHE_DIRECTORY,
        fileNamePrefix = "review",
        // 서버가 받지 않는 형식은 갤러리에서부터 보이지 않게 한다.
        mimeTypes = ALLOWED_IMAGE_CONTENT_TYPES.toTypedArray(),
        onImageSelected = { uri -> reviewPhoto = uri },
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 852.dp)
            .dismissKeyboardOnBackgroundTap(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                item {
                    PlaceDetailControls(onDismiss = onDismiss)
                    PlaceHeader(place = place)
                    PlaceActions(
                        personalMapAction = personalMapAction,
                        onAddToPersonalMapClick = onAddToPersonalMapClick,
                        onExternalLinkClick = onExternalLinkClick,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MoaMapTheme.colors.lineNormal,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }

                reviewItems(reviews = reviews, onRetryReviews = onRetryReviews)
            }

            ReviewComposer(
                placeId = place.id,
                reviews = reviews,
                photo = reviewPhoto,
                onAddPhotoClick = pickerState::showSourceMenu,
                onRemovePhotoClick = { reviewPhoto = null },
                onPhotoSubmitted = { reviewPhoto = null },
                onSubmitReview = onSubmitReview,
            )
        }

        if (pickerState.isSourceMenuVisible) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        detectTapGestures { pickerState.dismissSourceMenu() }
                    },
                contentAlignment = Alignment.Center,
            ) {
                ImageSourceMenu(
                    onCameraClick = pickerController::requestCamera,
                    onGalleryClick = pickerController::requestGallery,
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.reviewItems(
    reviews: PlaceReviewsUiModel,
    onRetryReviews: () -> Unit,
) {
    val loadErrorMessage = reviews.loadErrorMessage
    when {
        reviews.loading -> item { ReviewPlaceholder { CircularProgressIndicator() } }

        loadErrorMessage != null -> item {
            ReviewPlaceholder {
                ReviewLoadError(message = loadErrorMessage, onRetryClick = onRetryReviews)
            }
        }

        reviews.items.isEmpty() -> item {
            ReviewPlaceholder {
                Text(
                    text = "아직 후기가 없어요",
                    style = MoaMapTheme.typography.body2,
                    color = MoaMapTheme.colors.textAssistive,
                )
            }
        }

        else -> items(
            items = reviews.items,
            key = PlaceReviewUiModel::id,
        ) { review ->
            ReviewRow(review = review)
        }
    }
}

@Composable
private fun ReviewPlaceholder(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ReviewPlaceholderHeight),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun ReviewLoadError(message: String, onRetryClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            style = MoaMapTheme.typography.body2,
            color = MoaMapTheme.colors.textAssistive,
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MoaMapPrimitiveColors.Blue500,
            onClick = onRetryClick,
        ) {
            Text(
                text = "다시 시도",
                style = MoaMapTheme.typography.button2,
                color = MoaMapTheme.colors.textWhite,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun PlaceDetailControls(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = "뒤로가기",
                tint = MoaMapTheme.colors.textNormal,
                modifier = Modifier.size(24.dp),
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "닫기",
                tint = MoaMapTheme.colors.textNormal,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

/** 장소 정보. 별점·후기 수는 시안에서 빠졌다. */
@Composable
private fun PlaceHeader(place: PlaceUiModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(PlaceImageShape)
                .background(
                    color = MoaMapPrimitiveColors.Yellow50,
                    shape = PlaceImageShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_photo_camera),
                contentDescription = null,
                tint = MoaMapPrimitiveColors.Gray500,
                modifier = Modifier.size(24.dp),
            )
            AsyncImage(
                model = place.photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = place.name,
                    style = MoaMapTheme.typography.subtitle1,
                    color = MoaMapTheme.colors.textNormal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(favoriteIconRes(place.favorite)),
                    contentDescription = if (place.favorite) "즐겨찾기됨" else "즐겨찾기 안 됨",
                    tint = if (place.favorite) {
                        MoaMapTheme.colors.statusAlert
                    } else {
                        MoaMapPrimitiveColors.Gray100
                    },
                    modifier = Modifier.size(24.dp),
                )
                Icon(
                    painter = painterResource(R.drawable.ic_flag_filled),
                    contentDescription = "플래그",
                    tint = MoaMapTheme.colors.textNormal,
                    modifier = Modifier.size(24.dp),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = place.category,
                    style = MoaMapTheme.typography.caption0,
                    color = MoaMapPrimitiveColors.Yellow900,
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = MoaMapPrimitiveColors.Yellow500,
                            shape = PlaceCategoryShape,
                        )
                        .background(
                            color = MoaMapPrimitiveColors.Yellow50,
                            shape = PlaceCategoryShape,
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
                Text(
                    text = place.area,
                    style = MoaMapTheme.typography.body2,
                    color = MoaMapTheme.colors.textAlternative,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_location),
                    contentDescription = null,
                    tint = MoaMapTheme.colors.textAlternative,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = place.address,
                    style = MoaMapTheme.typography.body2,
                    color = MoaMapTheme.colors.textAlternative,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * 「나만의 지도에 추가」·「외부 링크로 가기」.
 *
 * 나만의 지도를 보고 있으면 [personalMapAction] 이 null 이라 외부 링크만 남아 한 줄을 다 쓴다.
 */
@Composable
private fun PlaceActions(
    personalMapAction: PersonalMapActionUiModel?,
    onAddToPersonalMapClick: () -> Unit,
    onExternalLinkClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                label = "외부 링크로 가기",
                iconRes = R.drawable.ic_arrow_outward,
                containerColor = MoaMapPrimitiveColors.Yellow100,
                contentColor = MoaMapPrimitiveColors.Yellow800,
                loading = false,
                onClick = onExternalLinkClick,
                modifier = Modifier.weight(1f),
            )
        }

        personalMapAction?.message?.let { message ->
            Text(
                text = message,
                style = MoaMapTheme.typography.caption0,
                color = if (personalMapAction.failed) {
                    MoaMapTheme.colors.statusAlert
                } else {
                    MoaMapPrimitiveColors.Blue600
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

/**
 * 후기 입력. 별점 없이 글과 사진 한 장을 남긴다.
 *
 * 사진은 시트가 들고 있다가([photo]) 서버가 받아들인 뒤에 비운다. 보내자마자 지우면 실패했을 때
 * 고른 사진이 날아간다.
 */
@Composable
private fun ReviewComposer(
    placeId: Long,
    reviews: PlaceReviewsUiModel,
    photo: Uri?,
    onAddPhotoClick: () -> Unit,
    onRemovePhotoClick: () -> Unit,
    onPhotoSubmitted: () -> Unit,
    onSubmitReview: ((reviewText: String, photo: Uri?) -> Boolean)?,
) {
    var reviewText by rememberSaveable(placeId) { mutableStateOf("") }
    val inputEnabled = onSubmitReview != null && !reviews.submitting
    val canSend = inputEnabled && (reviewText.isNotBlank() || photo != null)

    LaunchedEffect(reviews.submittedCount) {
        if (reviews.submittedCount > 0) {
            reviewText = ""
            onPhotoSubmitted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (photo != null) {
            ReviewDraftPhoto(
                photo = photo,
                removable = inputEnabled,
                onRemoveClick = onRemovePhotoClick,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 그림자는 내용 뒤에 따로 깔아야 한다. blur 를 입력창 자체에 걸면 안의 글자까지 흐려진다.
            ShadowedSurface(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = ReviewInputShape,
                color = MoaMapPrimitiveColors.White,
                shadowBlurRadius = CardShadowBlurRadius,
                shadowColor = CardShadowColor,
            ) {
                Row(
                    modifier = Modifier.padding(start = 8.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            // 사진은 한 장만 받는다. 이미 골랐으면 지우고 다시 고른다.
                            .clickable(
                                enabled = inputEnabled && photo == null,
                                role = Role.Button,
                                onClick = onAddPhotoClick,
                            )
                            .semantics { contentDescription = "사진 첨부" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = null,
                            tint = if (inputEnabled && photo == null) {
                                MoaMapTheme.colors.textNormal
                            } else {
                                MoaMapTheme.colors.textDisable
                            },
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    BasicTextField(
                        value = reviewText,
                        onValueChange = { reviewText = it },
                        modifier = Modifier.weight(1f),
                        enabled = inputEnabled,
                        singleLine = true,
                        textStyle = MoaMapTheme.typography.body2.copy(
                            color = MoaMapTheme.colors.textNormal,
                        ),
                        cursorBrush = SolidColor(MoaMapPrimitiveColors.Blue500),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (reviewText.isEmpty()) {
                                    Text(
                                        text = if (onSubmitReview == null) {
                                            "지도에 참여하면 후기를 남길 수 있어요"
                                        } else {
                                            "이 장소에 대한 경험을 공유해주세요"
                                        },
                                        style = MoaMapTheme.typography.body2,
                                        color = MoaMapTheme.colors.textAssistive,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) MoaMapPrimitiveColors.Blue500 else MoaMapPrimitiveColors.Gray200,
                    )
                    .clickable(
                        enabled = canSend,
                        role = Role.Button,
                        // 입력은 여기서 비우지 않는다. 서버가 받아들였는지는 아직 모른다.
                        onClick = { trySubmitReview(reviewText, photo, onSubmitReview) },
                    )
                    .semantics { contentDescription = "후기 보내기" },
                contentAlignment = Alignment.Center,
            ) {
                if (reviews.submitting) {
                    CircularProgressIndicator(
                        color = MoaMapPrimitiveColors.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_send),
                        contentDescription = null,
                        tint = MoaMapPrimitiveColors.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        reviews.submitErrorMessage?.let { message ->
            Text(
                text = message,
                style = MoaMapTheme.typography.caption0,
                color = MoaMapTheme.colors.statusAlert,
            )
        }
    }
}

/** 보내기 전의 첨부 사진. 오른쪽 위를 눌러 뺀다. */
@Composable
private fun ReviewDraftPhoto(
    photo: Uri,
    removable: Boolean,
    onRemoveClick: () -> Unit,
) {
    Box(modifier = Modifier.size(ReviewDraftPhotoSize)) {
        AsyncImage(
            model = photo,
            contentDescription = "첨부한 사진",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(ReviewPhotoShape)
                .background(MoaMapPrimitiveColors.Gray50),
        )
        if (removable) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MoaMapPrimitiveColors.Black.copy(alpha = 0.6f))
                    .clickable(role = Role.Button, onClick = onRemoveClick)
                    .semantics { contentDescription = "첨부한 사진 빼기" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = null,
                    tint = MoaMapPrimitiveColors.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun ReviewRow(review: PlaceReviewUiModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = MoaMapPrimitiveColors.Black,
                        shape = CircleShape,
                    ),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = review.userName,
                        style = MoaMapTheme.typography.body2,
                        color = MoaMapTheme.colors.textNormal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        text = review.relativeTime,
                        style = MoaMapTheme.typography.caption0,
                        color = MoaMapTheme.colors.textAlternative,
                    )
                }
                // 사진만 남기고 글은 비워 둘 수 있다. 그때 빈 줄이 끼지 않게 통째로 뺀다.
                if (review.message.isNotBlank()) {
                    Text(
                        text = review.message,
                        style = MoaMapTheme.typography.body1,
                        color = MoaMapTheme.colors.textNormal,
                    )
                }
                review.photoUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "후기 사진",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(ReviewPhotoSize)
                            .clip(ReviewPhotoShape)
                            .background(MoaMapPrimitiveColors.Gray50),
                    )
                }
            }
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = MoaMapTheme.colors.lineAlternative,
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PlaceDetailSheetPreview() {
    MoaMapTheme {
        Surface(color = MoaMapTheme.colors.backgroundSecondary) {
            PlaceDetailSheetContent(
                place = SamplePlaces.first(),
                reviews = PlaceReviewsUiModel(items = SamplePlaceReviews),
                onDismiss = {},
                onExternalLinkClick = {},
                personalMapAction = PersonalMapActionUiModel(message = "나만의 지도에 추가했어요"),
                onSubmitReview = { _, _ -> true },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** 나만의 지도를 보고 있을 때: 외부 링크만 남는다. 참여 전이라 입력도 막힌다. */
@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PlaceDetailSheetPersonalMapPreview() {
    MoaMapTheme {
        Surface(color = MoaMapTheme.colors.backgroundSecondary) {
            PlaceDetailSheetContent(
                place = SamplePlaces.first(),
                reviews = PlaceReviewsUiModel(),
                onDismiss = {},
                onExternalLinkClick = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
