package com.example.moamap.feature.mapdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.moamap.R
import com.example.moamap.core.designsystem.component.ShadowedSurface
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme

private val BottomSheetGrabberShape = RoundedCornerShape(100.dp)
private val SearchControlShape = RoundedCornerShape(44.dp)
private val CategoryChipShape = RoundedCornerShape(20.dp)

@Composable
internal fun MapDetailBottomSheet(
    places: List<PlaceUiModel>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onPlaceClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 698.dp)
            .background(MoaMapTheme.colors.backgroundSecondary),
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
            text = "장소 32곳",
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
                Text(
                    text = "장소,지도를 검색해보세요",
                    style = MoaMapTheme.typography.body2,
                    color = MoaMapTheme.colors.textAssistive,
                    maxLines = 1,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MapDetailCategories.forEach { category ->
                val selected = category == selectedCategory
                Surface(
                    modifier = Modifier.semantics { this.selected = selected },
                    shape = CategoryChipShape,
                    color = if (selected) {
                        MoaMapPrimitiveColors.Black
                    } else {
                        MoaMapPrimitiveColors.White
                    },
                    onClick = { onCategorySelected(category) },
                ) {
                    Text(
                        text = category,
                        style = MoaMapTheme.typography.button3,
                        color = if (selected) {
                            MoaMapPrimitiveColors.White
                        } else {
                            MoaMapPrimitiveColors.Black
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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

@Preview(showBackground = true, widthDp = 393, heightDp = 698)
@Composable
private fun MapDetailBottomSheetPreview() {
    MoaMapTheme {
        MapDetailBottomSheet(
            places = SamplePlaces,
            selectedCategory = "전체",
            onCategorySelected = {},
            onPlaceClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
