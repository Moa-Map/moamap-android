package com.example.moamap.feature.mapdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.moamap.R
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme

private val PlaceDetailSheetShape = RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)
private val PlaceImageShape = RoundedCornerShape(16.dp)
private val PlaceCategoryShape = RoundedCornerShape(100.dp)
private val ReviewComposerShape = RoundedCornerShape(20.dp)
private val ReviewInputShape = RoundedCornerShape(100.dp)
private val PlaceDetailGrabberShape = RoundedCornerShape(100.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaceDetailSheet(
    place: PlaceUiModel,
    reviews: List<PlaceReviewUiModel>,
    onDismiss: () -> Unit,
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
                    .height(25.dp),
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
        )
    }
}

@Composable
private fun PlaceDetailSheetContent(
    place: PlaceUiModel,
    reviews: List<PlaceReviewUiModel>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 852.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            PlaceDetailControls(onDismiss = onDismiss)
            PlaceHeader(place = place)
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = MoaMapTheme.colors.lineNormal,
            )
            ReviewComposer(placeId = place.id)
        }

        items(
            items = reviews,
            key = PlaceReviewUiModel::id,
        ) { review ->
            ReviewRow(review = review)
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
                .background(
                    color = MoaMapPrimitiveColors.Yellow50,
                    shape = PlaceImageShape,
                ),
        )

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
                    painter = painterResource(R.drawable.ic_favorite_filled),
                    contentDescription = "즐겨찾기",
                    tint = MoaMapTheme.colors.statusAlert,
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlaceDetailMetric(
                    iconRes = R.drawable.ic_star_filled,
                    value = place.rating.toString(),
                    contentDescription = "평점",
                    iconTint = MoaMapPrimitiveColors.Yellow500,
                )
                PlaceDetailMetric(
                    iconRes = R.drawable.ic_comment,
                    value = place.reviewCount.toString(),
                    contentDescription = "후기 수",
                    iconTint = MoaMapPrimitiveColors.Blue500,
                )
            }
        }
    }
}

@Composable
private fun PlaceDetailMetric(
    iconRes: Int,
    value: String,
    contentDescription: String,
    iconTint: androidx.compose.ui.graphics.Color,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = value,
            style = MoaMapTheme.typography.body3,
            color = MoaMapTheme.colors.textAlternative,
        )
    }
}

@Composable
private fun ReviewComposer(placeId: Long) {
    var rating by rememberSaveable(placeId) { mutableStateOf(0) }
    var reviewText by rememberSaveable(placeId) { mutableStateOf("") }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        shape = ReviewComposerShape,
        color = MoaMapPrimitiveColors.Yellow50,
        shadowElevation = 5.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "별점과 함께 후기를 입력해주세요.",
                style = MoaMapTheme.typography.body1,
                color = MoaMapTheme.colors.textNormal,
            )

            Row(
                modifier = Modifier.selectableGroup(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                (1..5).forEach { value ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .selectable(
                                selected = rating == value,
                                onClick = { rating = value },
                                role = Role.RadioButton,
                            )
                            .semantics {
                                contentDescription = "${value}점"
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(
                                if (value <= rating) {
                                    R.drawable.ic_star_filled
                                } else {
                                    R.drawable.ic_star_outline
                                },
                            ),
                            contentDescription = null,
                            tint = if (value <= rating) {
                                MoaMapPrimitiveColors.Yellow500
                            } else {
                                MoaMapPrimitiveColors.Gray100
                            },
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = ReviewInputShape,
                    color = MoaMapPrimitiveColors.White,
                ) {
                    BasicTextField(
                        value = reviewText,
                        onValueChange = { reviewText = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        singleLine = true,
                        textStyle = MoaMapTheme.typography.body2.copy(
                            color = MoaMapTheme.colors.textNormal,
                        ),
                        cursorBrush = SolidColor(MoaMapPrimitiveColors.Blue500),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                if (reviewText.isEmpty()) {
                                    Text(
                                        text = "이 장소에 대한 경험을 공유해주세요",
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

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(
                            role = Role.Button,
                            onClick = { reviewText = "" },
                        )
                        .semantics {
                            contentDescription = "후기 보내기"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MoaMapPrimitiveColors.Blue500,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.ic_send),
                                contentDescription = null,
                                tint = MoaMapPrimitiveColors.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
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
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                Text(
                    text = review.userName,
                    style = MoaMapTheme.typography.body2,
                    color = MoaMapTheme.colors.textNormal,
                )
                Row(
                    modifier = Modifier.semantics {
                        contentDescription = "평점 ${review.rating}점"
                    },
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    (1..5).forEach { value ->
                        Icon(
                            painter = painterResource(
                                if (value <= review.rating) {
                                    R.drawable.ic_star_filled
                                } else {
                                    R.drawable.ic_star_outline
                                },
                            ),
                            contentDescription = null,
                            tint = if (value <= review.rating) {
                                MoaMapPrimitiveColors.Yellow500
                            } else {
                                MoaMapPrimitiveColors.Gray100
                            },
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Text(
                    text = review.message,
                    style = MoaMapTheme.typography.body1,
                    color = MoaMapTheme.colors.textNormal,
                )
                Text(
                    text = review.relativeTime,
                    style = MoaMapTheme.typography.caption0,
                    color = MoaMapTheme.colors.textAlternative,
                )
            }

            Text(
                text = "⋮",
                style = MoaMapTheme.typography.subtitle1,
                color = MoaMapTheme.colors.textAlternative,
            )
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = MoaMapTheme.colors.lineNormal,
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
                reviews = SamplePlaceReviews,
                onDismiss = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
