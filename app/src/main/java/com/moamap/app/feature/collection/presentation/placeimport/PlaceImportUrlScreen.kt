package com.moamap.app.feature.collection.presentation.placeimport

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.ErrorSnackbar
import com.moamap.app.core.designsystem.component.MoaMapSearchBar
import com.moamap.app.core.designsystem.component.MoaMapSymbolNotice
import com.moamap.app.core.designsystem.modifier.dismissKeyboardOnBackgroundTap
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.collection.domain.model.PlaceImportSource
import com.moamap.app.feature.mapdetail.presentation.addplace.NoticeRetryButton
import com.moamap.app.feature.mapdetail.presentation.addplace.SearchStatusTopGap

/**
 * 링크 입력. 시안 릴리즈 v1 「인스타그램 장소 찾기」·「장소 가져오기」(외부 지도)·「양식 오류」.
 *
 * 버튼 없이 장소 검색처럼 키보드 엔터로 시작한다([onSubmit]). 링크가 잘못되면 입력칸이 빨개지고 아래에
 * 「링크를 다시 확인해주세요」가 뜬다. 입력칸 아래 같은 자리에 흐린 로고 안내가 있다.
 */
@Composable
internal fun PlaceImportUrlScreen(
    source: PlaceImportSource,
    url: String,
    linkInvalid: Boolean,
    errorMessage: String?,
    onUrlChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onSubmit: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val texts = linkStepTexts(source)
    // 다시 입력하거나 다시 시도할 때 ViewModel 이 지운다 - 한 번 보이고 사라지는 안내가 아니다.
    val inlineError = errorMessage.takeIf { source == PlaceImportSource.Instagram }

    Box(
        modifier = modifier
            .dismissKeyboardOnBackgroundTap()
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlaceImportTopBar(onBackClick = onBackClick, title = linkStepTitle(source))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding),
            ) {
                Spacer(Modifier.height(PlaceImportContentTopSpacing))

                PlaceImportHeader(title = texts.title, description = texts.description)

                Spacer(Modifier.height(PlaceImportSectionSpacing))

                UrlInputField(url = url, invalid = linkInvalid, onUrlChange = onUrlChange, onSubmit = onSubmit)

                // 오류 글자는 안내 위 빈 자리에 겹쳐 둔다. 시안은 오류가 떠도 안내 자리가 그대로다.
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (linkInvalid) {
                        Text(
                            text = "링크를 다시 확인해주세요",
                            style = MoaMapTheme.typography.body2,
                            color = MoaMapTheme.colors.statusAlert,
                            modifier = Modifier.padding(start = 2.dp, top = 4.dp),
                        )
                    }
                    if (inlineError != null) {
                        // 시안 「상세지도/장소추가 - 에러」: 안내 자리에 오류와 [다시 시도].
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            MoaMapSymbolNotice(
                                title = inlineError,
                                modifier = Modifier.padding(top = SearchStatusTopGap),
                            ) {
                                Text(
                                    text = "잠시 후 다시 시도해주세요",
                                    style = MoaMapTheme.typography.body2,
                                    color = MoaMapTheme.colors.textAlternative,
                                    textAlign = TextAlign.Center,
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            NoticeRetryButton(onClick = onSubmit)
                        }
                    } else {
                        MoaMapSymbolNotice(
                            title = texts.noticeTitle,
                            modifier = Modifier.padding(top = SearchStatusTopGap),
                        ) {
                            Text(
                                text = texts.noticeDescription,
                                style = MoaMapTheme.typography.body2,
                                color = MoaMapTheme.colors.textAlternative,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        // 인스타그램 불러오기 오류는 위 안내 자리에 남긴다. 외부 지도는 아직 아래 안내 한 줄이다.
        if (inlineError == null) {
            ErrorSnackbar(
                message = errorMessage,
                onShown = onErrorShown,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/** 링크 입력 화면의 문구. 인스타그램과 외부 지도가 다르다(시안). */
private data class LinkStepTexts(
    val title: String,
    val description: String,
    val noticeTitle: String,
    val noticeDescription: String,
)

private fun linkStepTexts(source: PlaceImportSource): LinkStepTexts = when (source) {
    PlaceImportSource.Instagram -> LinkStepTexts(
        title = "가져올 장소의 링크를 붙여넣어 주세요",
        description = "링크 속 장소를 찾아 지도에 추가해드려요",
        noticeTitle = "발견한 장소를 지도에 쏙!",
        noticeDescription = "릴스에서 발견한 장소를\n지도에 모아보세요",
    )
    PlaceImportSource.MapShare -> LinkStepTexts(
        title = "가져올 지도 링크를 입력해주세요",
        description = "링크 속 지도를 자동으로 추가해드려요",
        noticeTitle = "저장해둔 장소를 모아맵으로 쏙!",
        noticeDescription = "외부 지도에 모아둔 장소들을\n모아맵으로 가져와 보세요.",
    )
}

/**
 * 링크 아이콘 검색창 한 줄. 글자가 있으면 오른쪽에 지우기(X)를 둔다. 키보드 엔터가 [onSubmit] 이다.
 *
 * 들어오면 바로 입력할 수 있게 포커스를 준다 - 버튼이 없어 공유로 링크가 채워져 들어와도 키보드 엔터가
 * 있어야 시작할 수 있다.
 */
@Composable
private fun UrlInputField(
    url: String,
    invalid: Boolean,
    onUrlChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    MoaMapSearchBar(
        iconRes = R.drawable.ic_link,
        border = if (invalid) BorderStroke(1.dp, MoaMapTheme.colors.statusAlert) else null,
    ) {
        BasicTextField(
            value = url,
            onValueChange = onUrlChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            singleLine = true,
            textStyle = MoaMapTheme.typography.body2.copy(color = MoaMapTheme.colors.textNormal),
            cursorBrush = SolidColor(MoaMapTheme.colors.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            decorationBox = { innerTextField ->
                if (url.isEmpty()) {
                    Text(
                        text = "링크를 붙여넣어 주세요",
                        style = MoaMapTheme.typography.body2,
                        color = MoaMapTheme.colors.textAssistive,
                        maxLines = 1,
                    )
                }
                innerTextField()
            },
        )
        if (url.isNotEmpty()) {
            Icon(
                painter = painterResource(R.drawable.ic_cancel),
                contentDescription = "링크 지우기",
                tint = MoaMapTheme.colors.textNormal,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onUrlChange("") },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PlaceImportUrlScreenEmptyPreview() {
    MoaMapTheme {
        PlaceImportUrlScreen(
            source = PlaceImportSource.Instagram,
            url = "",
            linkInvalid = false,
            errorMessage = null,
            onUrlChange = {},
            onBackClick = {},
            onSubmit = {},
            onErrorShown = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PlaceImportUrlScreenInvalidPreview() {
    MoaMapTheme {
        PlaceImportUrlScreen(
            source = PlaceImportSource.MapShare,
            url = "link",
            linkInvalid = true,
            errorMessage = null,
            onUrlChange = {},
            onBackClick = {},
            onSubmit = {},
            onErrorShown = {},
        )
    }
}
