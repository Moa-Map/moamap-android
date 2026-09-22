package com.moamap.app.feature.mapdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moamap.app.core.designsystem.modifier.dismissKeyboardOnBackgroundTap
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.ListCardShadowBlurRadius
import com.moamap.app.core.designsystem.component.ListCardShadowColor
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme

private val BottomSheetGrabberShape = RoundedCornerShape(100.dp)
private val SearchControlShape = RoundedCornerShape(44.dp)
private val CategoryChipShape = RoundedCornerShape(1000.dp)

/** 칩 줄 아래로 조금 남기는 여백. 접었을 때 목록 첫 줄이 살짝 비쳐 더 있다는 걸 알린다. */
private val SheetHeaderBottomGap = 12.dp

/** 시트를 끝까지 올렸을 때의 높이. 상단 탭이 보이는 자리까지만 올라온다. */
private val SheetExpandedHeight = 698.dp

/**
 * 키보드가 올라와 있는 동안의 높이.
 *
 * 상단 탭까지 덮어 [SheetExpandedHeight] 보다 높다. 키보드가 화면 아래 절반을 먹는
 * 동안 목록이 몇 줄이라도 더 보이게 하려는 자리다.
 */
private val SheetSearchingHeight = 768.dp

@Composable
internal fun MapDetailBottomSheet(
    places: List<PlaceUiModel>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchFocused: () -> Unit,
    onPlaceClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    /** 서버가 세어 준 등록 장소 수. 응답이 오기 전에는 null 이라 개수를 감춘다. */
    placeCount: Int? = null,
    categoryFilters: List<PlaceCategoryFilter> = listOf(PlaceCategoryFilter.All),
    selectedCategory: PlaceCategoryFilter = PlaceCategoryFilter.All,
    onCategorySelect: (PlaceCategoryFilter) -> Unit = {},
    /**
     * 제목부터 칩 줄까지의 높이. 접힌 시트가 딱 그만큼만 올라오도록 화면에 알려 준다.
     *
     * 상수로 두면 글자 크기를 키운 기기에서 칩이 잘린다.
     */
    onHeaderHeightChange: (Dp) -> Unit = {},
) {
    val density = LocalDensity.current
    val currentOnHeaderHeightChange by rememberUpdatedState(onHeaderHeightChange)
    /**
     * 키보드가 떠 있는지.
     *
     * 포커스로 재지 않는다. 키보드를 내려도 입력창 포커스는 그대로 남아, 포커스를 보면
     * 키보드가 사라진 뒤에도 시트가 [SheetSearchingHeight] 에 묶인다.
     */
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    // 키보드가 내려갔으면 입력창도 놓아준다. 안 놓으면 커서가 혼자 깜빡인다.
    val focusManager = LocalFocusManager.current
    LaunchedEffect(imeVisible) {
        if (!imeVisible) focusManager.clearFocus()
    }

    Column(
        modifier = modifier
            .dismissKeyboardOnBackgroundTap()
            .fillMaxWidth()
            .heightIn(max = if (imeVisible) SheetSearchingHeight else SheetExpandedHeight)
            .background(MoaMapTheme.colors.backgroundSecondary)
            // 키보드가 올라오면 목록 아래쪽이 그만큼 밀려 마지막 카드까지 닿는다.
            .imePadding(),
    ) {
        // 접었을 때 여기까지 보인다. 높이를 재서 화면에 알려 준다.
        Column(
            modifier = Modifier.onSizeChanged { size ->
                currentOnHeaderHeightChange(with(density) { size.height.toDp() } + SheetHeaderBottomGap)
            },
        ) {
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
                            shape = BottomSheetGrabberShape,
                        ),
                )
            }

            Text(
                text = placeCount?.let { count -> "장소 ${count}곳" } ?: "장소",
                style = MoaMapTheme.typography.title3,
                color = MoaMapTheme.colors.textNormal,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            ShadowedSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = 20.dp),
                shape = SearchControlShape,
                color = MoaMapPrimitiveColors.White,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        tint = MoaMapTheme.colors.textAssistive,
                        modifier = Modifier.size(16.dp),
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "장소를 검색해보세요",
                                style = MoaMapTheme.typography.body2,
                                color = MoaMapTheme.colors.textAssistive,
                                maxLines = 1,
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) onSearchFocused()
                                },
                            textStyle = MoaMapTheme.typography.body2.copy(
                                color = MoaMapTheme.colors.textNormal,
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(MoaMapTheme.colors.textNormal),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            CategoryChipRow(
                filters = categoryFilters,
                selected = selectedCategory,
                onSelect = onCategorySelect,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (places.isEmpty()) {
            // weight 를 줘야 목록이 있을 때와 시트 높이가 같다. 없으면 검색 결과가 빌
            // 때마다 시트가 글자 높이로 쪼그라들었다 펴진다.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                Text(
                    text = when {
                        searchQuery.isNotBlank() -> "검색 결과가 없어요"
                        selectedCategory != PlaceCategoryFilter.All -> "이 카테고리의 장소가 없어요"
                        else -> "아직 등록된 장소가 없어요"
                    },
                    style = MoaMapTheme.typography.body2,
                    color = MoaMapTheme.colors.textAssistive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = places,
                    key = PlaceUiModel::id,
                ) { place ->
                    PlaceListItem(
                        place = place,
                        onClick = { onPlaceClick(place.id) },
                    )
                }
            }
        }
    }
}

/**
 * 카테고리 칩 줄. 탐색 화면의 칩과 같은 모양이다.
 *
 * 장소가 없거나 한 종류뿐이어도 「전체」 칩만 두고 줄은 남긴다. 줄이 사라지면 접힌 시트
 * 높이가 지도마다 달라진다.
 */
@Composable
private fun CategoryChipRow(
    filters: List<PlaceCategoryFilter>,
    selected: PlaceCategoryFilter,
    onSelect: (PlaceCategoryFilter) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(items = filters, key = { filter -> filter.saveKey() }) { filter ->
            CategoryChip(
                label = filter.label,
                selected = filter == selected,
                onClick = { onSelect(filter) },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ShadowedSurface(
        shape = CategoryChipShape,
        color = if (selected) MoaMapPrimitiveColors.Gray800 else MoaMapPrimitiveColors.White,
        shadowBlurRadius = ListCardShadowBlurRadius,
        shadowColor = ListCardShadowColor,
        onClick = onClick,
    ) {
        Text(
            text = label,
            style = MoaMapTheme.typography.button3,
            color = if (selected) MoaMapTheme.colors.textWhite else MoaMapTheme.colors.textNormal,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 698)
@Composable
private fun MapDetailBottomSheetPreview() {
    MoaMapTheme {
        MapDetailBottomSheet(
            places = SamplePlaces,
            searchQuery = "",
            onSearchQueryChange = {},
            onSearchFocused = {},
            onPlaceClick = {},
            modifier = Modifier.fillMaxSize(),
            placeCount = 32,
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 698)
@Composable
private fun MapDetailBottomSheetEmptySearchPreview() {
    MoaMapTheme {
        MapDetailBottomSheet(
            places = emptyList(),
            searchQuery = "없는장소",
            onSearchQueryChange = {},
            onSearchFocused = {},
            onPlaceClick = {},
            modifier = Modifier.fillMaxSize(),
            placeCount = 32,
        )
    }
}
