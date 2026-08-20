package com.moamap.app.feature.collection.presentation.createmap

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.ButtonShadowBlurRadius
import com.moamap.app.core.designsystem.component.ButtonShadowColor
import com.moamap.app.core.designsystem.component.CardShadowBlurRadius
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight

private val FieldShape = RoundedCornerShape(12.dp)
private val VisibilityCardShape = RoundedCornerShape(16.dp)
private val SubmitButtonShape = RoundedCornerShape(8.dp)
private val ChipShape = RoundedCornerShape(100.dp)

/**
 * 사진 카드의 가로:세로. 피그마가 353x235.33 으로 그렸는데 정확히 3:2 다.
 *
 * 높이를 dp 로 못 박으면 폭이 좁은 기기에서 카드만 세로로 길어 보인다. 커버 사진을
 * 미리보기로 채우는 자리라 비율을 지키는 쪽이 맞다.
 */
private const val PhotoCardAspectRatio = 3f / 2f

private val VisibilityCardHeight = 113.dp
private val SubmitButtonHeight = 54.dp

private val PhotoCardShadowColor = MoaMapPrimitiveColors.Black.copy(alpha = 0.1f)

/** 제목과 그 아래 내용 사이 간격. 피그마의 섹션 공통값이다. */
private val SectionTitleGap = 6.dp

@Composable
internal fun CreateMapSectionTitle(text: String) {
    Text(
        text = text,
        style = MoaMapTheme.typography.subtitle1,
        color = MoaMapTheme.colors.textNormal,
    )
}

/**
 * 지도 사진 선택 카드.
 *
 * 고른 사진이 있으면 카드를 가득 채워 보여주고, 없으면 추가 안내를 보여준다.
 */
@Composable
internal fun MapPhotoField(
    imageUri: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SectionTitleGap),
    ) {
        CreateMapSectionTitle("지도 사진")

        ShadowedSurface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PhotoCardAspectRatio),
            shape = FieldShape,
            shadowBlurRadius = CardShadowBlurRadius,
            shadowColor = PhotoCardShadowColor,
            onClick = onClick,
        ) {
            if (imageUri == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
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
            } else {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "선택한 지도 사진",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(FieldShape),
                )
            }
        }
    }
}

/**
 * 라벨 + 입력창 한 벌.
 *
 * 태그처럼 라벨과 입력창 사이에 무언가 끼는 경우가 있어 [betweenLabelAndInput] 슬롯을 둔다.
 */
@Composable
internal fun CreateMapInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    betweenLabelAndInput: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MoaMapTheme.typography.subtitle2,
            color = MoaMapTheme.colors.textNormal,
            modifier = Modifier.padding(start = 2.dp),
        )

        betweenLabelAndInput?.invoke()

        ShadowedSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = FieldShape,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MoaMapTheme.typography.body2.copy(
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
                                style = MoaMapTheme.typography.body2,
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
                style = if (selected) {
                    MoaMapTheme.typography.body3
                } else {
                    MoaMapTheme.typography.body2
                },
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

/** 담은 태그 목록. 한 줄을 넘기면 다음 줄로 흘린다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TagChipRow(
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
            TagChip(tag = tag, onRemove = { onRemoveTag(tag) })
        }
    }
}

@Composable
private fun TagChip(
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
            style = MoaMapTheme.typography.caption0,
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

/** 진행 표시 크기. 버튼 높이 안에 들어가면서 글자와 비슷한 무게로 보이는 값. */
private val SubmitProgressSize = 20.dp

@Composable
internal fun CreateMapSubmitButton(
    enabled: Boolean,
    submitting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShadowedSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(SubmitButtonHeight),
        shape = SubmitButtonShape,
        color = if (enabled) MoaMapTheme.colors.primary else MoaMapPrimitiveColors.Gray200,
        shadowBlurRadius = ButtonShadowBlurRadius,
        shadowColor = ButtonShadowColor,
        onClick = if (enabled) onClick else null,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (submitting) {
                CircularProgressIndicator(
                    color = MoaMapTheme.colors.textWhite,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(SubmitProgressSize),
                )
            } else {
                Text(
                    text = "지도 만들기",
                    style = MoaMapTheme.typography.button0,
                    color = MoaMapTheme.colors.textWhite,
                )
            }
        }
    }
}

