package com.example.moamap.feature.mapdetail.presentation.addplace

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.moamap.R
import com.example.moamap.core.designsystem.component.ShadowedSurface
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import com.example.moamap.feature.mapdetail.domain.model.PlaceCandidate

private val SelectedCardShape = RoundedCornerShape(16.dp)
private val InputShape = RoundedCornerShape(12.dp)
private val TagChipShape = RoundedCornerShape(1000.dp)
private val PhotoShape = RoundedCornerShape(12.dp)

/** 2단계. 고른 장소에 사진·태그·메모를 붙인다. 셋 다 선택이다. */
@Composable
internal fun PlaceFormContent(
    candidate: PlaceCandidate,
    photos: List<Uri>,
    tags: List<String>,
    tagInput: String,
    memo: String,
    canAddPhoto: Boolean,
    onAddPhotoClick: () -> Unit,
    onRemovePhoto: (Uri) -> Unit,
    onTagInputChange: (String) -> Unit,
    onTagBackspace: () -> Unit,
    onRemoveTag: (String) -> Unit,
    onMemoChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AddPlaceHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "장소등록",
            style = MoaMapTheme.typography.title3,
            color = MoaMapTheme.colors.textNormal,
        )

        SelectedPlaceCard(candidate = candidate)

        FormSection(title = "사진") {
            PhotoPicker(
                photos = photos,
                canAddPhoto = canAddPhoto,
                onAddPhotoClick = onAddPhotoClick,
                onRemovePhoto = onRemovePhoto,
            )
        }

        FormSection(title = "태그") {
            if (tags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    tags.forEach { tag ->
                        TagChip(tag = tag, onRemove = { onRemoveTag(tag) })
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            FormTextField(
                value = tagInput,
                onValueChange = onTagInputChange,
                placeholder = "태그 입력 후 스페이스 또는 엔터",
                modifier = Modifier.onPreviewKeyEvent { event ->
                    val isBackspaceDown =
                        event.type == KeyEventType.KeyDown && event.key == Key.Backspace
                    if (isBackspaceDown && tagInput.isEmpty() && tags.isNotEmpty()) {
                        onTagBackspace()
                        true
                    } else {
                        false
                    }
                },
            )
        }

        FormSection(title = "메모") {
            FormTextField(
                value = memo,
                onValueChange = onMemoChange,
                placeholder = "메모를 남겨보세요",
            )
        }
    }
}

/** 라벨 옆의 `(선택)` 은 세 항목이 모두 필수가 아니라는 표시다. */
@Composable
private fun FormSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MoaMapTheme.typography.subtitle1,
                color = MoaMapPrimitiveColors.Black,
            )
            Text(
                text = "(선택)",
                style = MoaMapTheme.typography.body2,
                color = MoaMapPrimitiveColors.Black,
            )
        }
        content()
    }
}

@Composable
private fun SelectedPlaceCard(candidate: PlaceCandidate) {
    ShadowedSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = SelectedCardShape,
        color = MoaMapPrimitiveColors.Yellow50,
        border = androidx.compose.foundation.BorderStroke(1.dp, MoaMapPrimitiveColors.Yellow500),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaceImagePlaceholder(size = 64.dp, cornerRadius = 12.dp)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = candidate.name,
                    style = MoaMapTheme.typography.subtitle2,
                    color = MoaMapTheme.colors.textNormal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (candidate.displayAddress.isNotEmpty()) {
                    Text(
                        text = candidate.displayAddress,
                        style = MoaMapTheme.typography.caption0,
                        color = MoaMapTheme.colors.textNormal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoPicker(
    photos: List<Uri>,
    canAddPhoto: Boolean,
    onAddPhotoClick: () -> Unit,
    onRemovePhoto: (Uri) -> Unit,
) {
    if (photos.isEmpty()) {
        ShadowedSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = InputShape,
            onClick = onAddPhotoClick,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    tint = MoaMapTheme.colors.textAssistive,
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    text = "사진 추가하기",
                    style = MoaMapTheme.typography.body2,
                    color = MoaMapTheme.colors.textAssistive,
                )
            }
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        photos.forEach { uri ->
            Box(modifier = Modifier.size(96.dp)) {
                AsyncImage(
                    model = uri,
                    contentDescription = "첨부한 사진",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(PhotoShape)
                        .background(MoaMapPrimitiveColors.Gray50),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp)
                        .clip(TagChipShape)
                        .background(MoaMapPrimitiveColors.TransparentBlack)
                        .clickable { onRemovePhoto(uri) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "사진 빼기",
                        tint = MoaMapPrimitiveColors.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        // 다섯 장이 차면 더 붙일 수 없다. 서버 제한이다.
        if (canAddPhoto) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(PhotoShape)
                    .background(MoaMapPrimitiveColors.White)
                    .border(1.dp, MoaMapTheme.colors.lineNormal, PhotoShape)
                    .clickable(onClick = onAddPhotoClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = "사진 추가하기",
                    tint = MoaMapTheme.colors.textAssistive,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

@Composable
private fun TagChip(tag: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .background(MoaMapPrimitiveColors.Yellow50, TagChipShape)
            .border(1.dp, MoaMapPrimitiveColors.Yellow500, TagChipShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = tag,
            style = MoaMapTheme.typography.caption0,
            color = MoaMapPrimitiveColors.Yellow900,
            maxLines = 1,
        )
        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = "$tag 태그 빼기",
            tint = MoaMapPrimitiveColors.Yellow900,
            modifier = Modifier
                .size(14.dp)
                .clickable(onClick = onRemove),
        )
    }
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    ShadowedSurface(
        modifier = modifier.fillMaxWidth(),
        shape = InputShape,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            textStyle = MoaMapTheme.typography.body2.copy(color = MoaMapTheme.colors.textNormal),
            cursorBrush = SolidColor(MoaMapPrimitiveColors.Blue500),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MoaMapTheme.typography.body2,
                        color = MoaMapTheme.colors.textAssistive,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                innerTextField()
            },
        )
    }
}

private val PreviewCandidate = PlaceCandidate(
    kakaoPlaceId = "1",
    name = "커피나무",
    address = "서울 동작구 상도동 369",
    roadAddress = "서울시 동작구 369",
    latitude = 37.4963,
    longitude = 126.9574,
    category = "음식점 > 카페",
    placeUrl = null,
)

@Preview(showBackground = true, widthDp = 393, heightDp = 800)
@Composable
private fun PlaceFormContentPreview() {
    MoaMapTheme {
        Box(modifier = Modifier.background(MoaMapTheme.colors.backgroundSecondary)) {
            PlaceFormContent(
                candidate = PreviewCandidate,
                photos = emptyList(),
                tags = listOf("성수", "카페", "데이트"),
                tagInput = "",
                memo = "",
                canAddPhoto = true,
                onAddPhotoClick = {},
                onRemovePhoto = {},
                onTagInputChange = {},
                onTagBackspace = {},
                onRemoveTag = {},
                onMemoChange = {},
            )
        }
    }
}
