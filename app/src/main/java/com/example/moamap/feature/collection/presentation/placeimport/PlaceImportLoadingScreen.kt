package com.example.moamap.feature.collection.presentation.placeimport

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.moamap.core.designsystem.theme.MoaMapDimens
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme

private val ProgressIndicatorSize = 80.dp

@Composable
internal fun PlaceImportLoadingScreen(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 뒤로가기는 진행 중이던 추출을 취소하고 URL 입력으로 되돌린다.
    BackHandler(onBack = onCancel)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary)
            .statusBarsPadding(),
    ) {
        PlaceImportTopBar(onBackClick = onCancel)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(ProgressIndicatorSize),
                    color = MoaMapTheme.colors.primary,
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "장소 불러오는 중..",
                        style = MoaMapTheme.typography.title1,
                        color = MoaMapPrimitiveColors.Black,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "AI가 영상을 분석하고 있어요.\n최대 30초 정도 걸려요.",
                        style = MoaMapTheme.typography.subtitle4,
                        color = MoaMapPrimitiveColors.Black,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PlaceImportLoadingScreenPreview() {
    MoaMapTheme {
        PlaceImportLoadingScreen(onCancel = {})
    }
}
