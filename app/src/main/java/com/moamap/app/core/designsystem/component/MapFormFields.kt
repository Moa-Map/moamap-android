package com.moamap.app.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.moamap.app.R
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight

// 새 지도 만들기와 지도 정보 수정이 함께 쓰는 입력 부품. 아래 고정 버튼은 MoaMapLargeButton.

private val FieldShape = RoundedCornerShape(12.dp)
private val ChipShape = RoundedCornerShape(100.dp)

private const val PhotoCardAspectRatio = 3f / 2f

/** 제목과 그 아래 내용 사이 간격. 피그마의 섹션 공통값이다. */
private val SectionTitleGap = 6.dp

/** 사진 위에 깔아 「사진 수정하기」가 읽히게 하는 막. 시안 검정 60%. */
private val PhotoEditScrim = Color.Black.copy(alpha = 0.6f)

@Composable
internal fun MapFormSectionTitle(text: String, required: Boolean = false) {
    MapFormLabel(
        text = text,
        style = MoaMapTheme.typography.subtitle1.withDesignLineHeight(),
        required = required,
    )
}

/**
 * 칸 제목. 필수 칸이면 뒤에 빨간 별표를 붙인다.
 *
 * 화면 읽기가 별표를 "별표"로 읽지 않게 제목 줄을 「지도 이름, 필수」 한 덩어리로 바꿔 둔다.
 */
@Composable
private fun MapFormLabel(
    text: String,
    style: TextStyle,
    required: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = if (required) {
            modifier.clearAndSetSemantics { contentDescription = "$text, 필수" }
        } else {
            modifier
        },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = text, style = style, color = MoaMapTheme.colors.textNormal)
        if (required) {
            Text(text = "*", style = style, color = MoaMapTheme.colors.statusAlert)
        }
    }
}

/**
 * 지도 사진 칸.
 *
 * 사진이 있으면 카드를 가득 채워 보여주고, 없으면 추가 안내를 보여준다.
 *
 * @param photo 고른 사진의 `content://` 또는 이미 올라가 있는 사진 주소.
 * @param showsEditOverlay 사진 위에 어두운 막과 「사진 수정하기」를 띄울지. 지도 정보 수정 시안이다.
 */
@Composable
internal fun MapPhotoField(
    photo: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showsEditOverlay: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SectionTitleGap),
    ) {
        MapFormSectionTitle("지도 사진")

        ShadowedSurface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PhotoCardAspectRatio),
            shape = FieldShape,
            shadowBlurRadius = CardShadowBlurRadius,
            onClick = onClick,
        ) {
            if (photo == null) {
                PhotoFieldGuide(
                    iconRes = R.drawable.ic_add,
                    text = "사진 추가하기",
                    color = MoaMapTheme.colors.textAssistive,
                )
            } else {
                AsyncImage(
                    model = photo,
                    contentDescription = "지도 사진",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(FieldShape),
                )
                if (showsEditOverlay) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PhotoEditScrim),
                        contentAlignment = Alignment.Center,
                    ) {
                        PhotoFieldGuide(
                            iconRes = R.drawable.ic_edit,
                            text = "사진 수정하기",
                            color = MoaMapTheme.colors.textWhite,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoFieldGuide(@DrawableRes iconRes: Int, text: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = text,
            style = MoaMapTheme.typography.body2.withDesignLineHeight(),
            color = color,
        )
    }
}

/**
 * 라벨 + 입력창 한 벌.
 *
 * 태그처럼 라벨과 입력창 사이에 무언가 끼는 경우가 있어 [betweenLabelAndInput] 슬롯을 둔다.
 */
@Composable
internal fun MapFormInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    required: Boolean = false,
    betweenLabelAndInput: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MapFormLabel(
            text = label,
            style = MoaMapTheme.typography.subtitle2.withDesignLineHeight(),
            required = required,
            modifier = Modifier.padding(start = 2.dp),
        )

        betweenLabelAndInput?.invoke()

        MoaMapInputSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = FieldShape,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MoaMapTheme.typography.body2.withDesignLineHeight().copy(
                    color = MoaMapTheme.colors.textNormal,
                ),
                cursorBrush = SolidColor(MoaMapTheme.colors.primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = imeAction),
                keyboardActions = keyboardActions,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MoaMapTheme.typography.body2.withDesignLineHeight(),
                                color = MoaMapTheme.colors.textAssistive,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
    }
}

/** 담은 태그 목록. 한 줄을 넘기면 다음 줄로 흘린다. 칩을 누르면 그 태그를 뺀다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MapTagChipRow(
    tags: List<String>,
    onRemoveTag: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tags.forEach { tag ->
            MapTagChip(tag = tag, onRemove = { onRemoveTag(tag) })
        }
    }
}

@Composable
private fun MapTagChip(
    tag: String,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(ChipShape)
            .background(MoaMapPrimitiveColors.Yellow50, ChipShape)
            .border(BorderStroke(1.dp, MoaMapPrimitiveColors.Yellow500), ChipShape)
            .clickable(onClick = onRemove)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = tag,
            style = MoaMapTheme.typography.caption0.withDesignLineHeight(),
            color = MoaMapPrimitiveColors.Yellow900,
        )
        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = "$tag 태그 삭제",
            tint = MoaMapPrimitiveColors.Yellow900,
            modifier = Modifier.size(14.dp),
        )
    }
}

