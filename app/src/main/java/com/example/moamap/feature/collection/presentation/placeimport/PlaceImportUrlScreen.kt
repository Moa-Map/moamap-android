package com.example.moamap.feature.collection.presentation.placeimport

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.moamap.core.designsystem.component.ShadowedSurface
import com.example.moamap.core.designsystem.theme.MoaMapDimens
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme

@Composable
internal fun PlaceImportUrlScreen(
    url: String,
    canSearch: Boolean,
    onUrlChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlaceImportTopBar(onBackClick = onBackClick)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding),
            ) {
                Spacer(Modifier.height(PlaceImportContentTopSpacing))

                PlaceImportHeader(
                    title = "가져올 장소 url을 입력해주세요",
                    description = "링크 속 장소를 자동으로 인식해서 추가해드려요",
                )

                Spacer(Modifier.height(PlaceImportSectionSpacing))

                UrlInputField(
                    url = url,
                    canSearch = canSearch,
                    onUrlChange = onUrlChange,
                    onSearch = onSearchClick,
                )
            }
        }

        PlaceImportBottomBar(modifier = Modifier.align(Alignment.BottomCenter)) {
            PlaceImportPrimaryButton(
                text = "검색하기",
                enabled = canSearch,
                onClick = onSearchClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Material `TextField` 는 자체 패딩과 인디케이터가 있어 디자인의
 * `padding 12/16 + 모서리 12 + 그림자` 를 맞추기 어려워 [BasicTextField] 위에 placeholder 를 겹친다.
 */
@Composable
private fun UrlInputField(
    url: String,
    canSearch: Boolean,
    onUrlChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    ShadowedSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = PlaceImportCardShape,
        color = MoaMapPrimitiveColors.White,
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (url.isEmpty()) {
                Text(
                    text = "url을 입력해주세요",
                    style = MoaMapTheme.typography.body2,
                    color = MoaMapTheme.colors.textAssistive,
                )
            }
            BasicTextField(
                value = url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MoaMapTheme.typography.body2.copy(
                    color = MoaMapTheme.colors.textNormal,
                ),
                singleLine = true,
                cursorBrush = SolidColor(MoaMapTheme.colors.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Search,
                ),
                // 검색 버튼과 달리 IME 검색키는 비활성화할 수 없다. 빈 URL 로 로딩 화면에
                // 들어가면 추출이 시작되지 않아 끝나지 않는 로딩에 갇힌다.
                keyboardActions = KeyboardActions(onSearch = { if (canSearch) onSearch() }),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PlaceImportUrlScreenEmptyPreview() {
    MoaMapTheme {
        PlaceImportUrlScreen(
            url = "",
            canSearch = false,
            onUrlChange = {},
            onBackClick = {},
            onSearchClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PlaceImportUrlScreenFilledPreview() {
    MoaMapTheme {
        PlaceImportUrlScreen(
            url = "https://www.instagram.com/reel/ABC123/",
            canSearch = true,
            onUrlChange = {},
            onBackClick = {},
            onSearchClick = {},
        )
    }
}
