package com.moamap.app.feature.mypage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.ButtonShadowBlurRadius
import com.moamap.app.core.designsystem.component.ButtonShadowColor
import com.moamap.app.core.designsystem.component.MoaMapInputSurface
import com.moamap.app.core.designsystem.component.MoaMapTitleTopBar
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.modifier.dismissKeyboardOnBackgroundTap
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import com.moamap.app.feature.mypage.presentation.InquiryType
import com.moamap.app.feature.mypage.presentation.isInquiryComplete
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/** 칸 묶음 좌우 여백·위 여백·칸 사이. 시안 값. */
private val FieldsHorizontalPadding = 20.dp
private val FieldsTopPadding = 20.dp
private val FieldsGap = 20.dp

/** 칸 제목 ↔ 칸, 칸 제목 왼쪽 안쪽. 시안 「InputField」. */
private val FieldTitleGap = 8.dp
private val FieldTitleStartPadding = 2.dp

private val FieldShape = RoundedCornerShape(12.dp)

/** 칸 그림자. 시안 「문의하기」: 0 0 10 8%. */
private val FieldShadowBlurRadius = 10.dp
private val FieldShadowColor = MoaMapPrimitiveColors.Black.copy(alpha = 0.08f)

/** 문의 내용 칸 높이. 시안 값. 넘치는 글은 칸 안에서 스크롤한다. */
private val ContentFieldHeight = 200.dp

/** 아래 버튼. 시안 「Button」 Large: 높이 54, 모서리 8, 화면 끝 20. */
private val SubmitButtonHeight = 54.dp
private val SubmitButtonShape = RoundedCornerShape(8.dp)
private val SubmitButtonVerticalPadding = 12.dp

/** 스크롤 영역 아래 여백. 마지막 칸이 버튼(54 + 위아래 12)에 가리지 않게 칸 사이만큼 더 둔다. */
private val FieldsBottomGap = SubmitButtonHeight + SubmitButtonVerticalPadding * 2 + FieldsGap

private val TypeMenuShape = RoundedCornerShape(12.dp)
private val TypeMenuRowHeight = 50.dp

/** 시안 유형 목록 「모달창」 바탕: #4A4F52 의 60%. */
private val TypeMenuScrim = MoaMapPrimitiveColors.Gray500.copy(alpha = 0.6f)

/**
 * 유형 목록 바탕 한 겹: 뒤를 흐리고 회색을 덮는다. 흐림이 아예 안 되는 기기에서만 회색을 깐다.
 *
 * 시안 값은 흐림 5 인데 Haze 에 5 를 주면 시안보다 덜 흐려 밑의 글자가 읽힌다. 시안 그림과 나란히
 * 놓고 맞춰 보니 10 이 같았다(10-05, 에뮬레이터 API 30). 줄에 걸린 흐림 10 은 칠한 색이 없어 시안에
 * 안 보이므로 뺀다(프로필 메뉴와 같다).
 */
private val TypeMenuBackdropStyle = HazeStyle(
    tint = HazeTint(TypeMenuScrim),
    blurRadius = 10.dp,
    noiseFactor = 0f,
    fallbackTint = HazeTint(TypeMenuScrim),
)

/**
 * 설정 「문의하기」. 유형·답변 받을 이메일·내용을 다 채우면 아래 버튼이 켜진다.
 *
 * 문의 API 가 없어 버튼을 눌러도 아직 아무것도 하지 않는다(10-05 사용자 결정). 보낼 곳이 없으니
 * 입력값도 ViewModel 없이 화면에 둔다 - API 가 생기면 ViewModel 로 옮겨 보낸다.
 */
@Composable
internal fun InquiryScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var type by rememberSaveable { mutableStateOf<InquiryType?>(null) }
    var email by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var typeMenuVisible by rememberSaveable { mutableStateOf(false) }

    InquiryContent(
        type = type,
        email = email,
        content = content,
        typeMenuVisible = typeMenuVisible,
        onBackClick = onBackClick,
        onTypeFieldClick = { typeMenuVisible = !typeMenuVisible },
        onTypeSelected = { selected ->
            type = selected
            typeMenuVisible = false
        },
        onTypeMenuDismiss = { typeMenuVisible = false },
        onEmailChange = { email = it },
        onContentChange = { content = it },
        modifier = modifier,
    )
}

@Composable
private fun InquiryContent(
    type: InquiryType?,
    email: String,
    content: String,
    typeMenuVisible: Boolean,
    onBackClick: () -> Unit,
    onTypeFieldClick: () -> Unit,
    onTypeSelected: (InquiryType) -> Unit,
    onTypeMenuDismiss: () -> Unit,
    onEmailChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hazeState = rememberHazeState()
    val focusManager = LocalFocusManager.current

    // 유형 목록은 화면 맨 위에 띄워 아래 칸들을 덮는다. 붙일 자리(유형 칸)를 이 화면 기준으로 구한다.
    var screenOrigin by remember { mutableStateOf(Offset.Zero) }
    var typeFieldBounds by remember { mutableStateOf(Rect.Zero) }

    BackHandler(enabled = typeMenuVisible, onBack = onTypeMenuDismiss)

    Box(
        modifier = modifier
            .dismissKeyboardOnBackgroundTap()
            .fillMaxSize()
            .onGloballyPositioned { coordinates -> screenOrigin = coordinates.positionInRoot() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(MoaMapTheme.colors.backgroundSecondary)
                .statusBarsPadding(),
        ) {
            MoaMapTitleTopBar(title = "문의하기", onBackClick = onBackClick)

            // 키보드가 뜨면 이 영역만 줄어들고 스크롤로 입력 칸이 올라온다. 버튼은 화면 아래 그대로 둔다.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = FieldsHorizontalPadding)
                    .padding(top = FieldsTopPadding, bottom = FieldsBottomGap),
                verticalArrangement = Arrangement.spacedBy(FieldsGap),
            ) {
                InquiryHeading()

                InquiryField(
                    label = "문의 유형",
                    highlighted = typeMenuVisible,
                    onClick = {
                        // 이메일·내용을 입력하던 키보드를 내려야 목록이 가리지 않는다.
                        focusManager.clearFocus()
                        onTypeFieldClick()
                    },
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        typeFieldBounds = coordinates.boundsInRoot()
                    },
                ) {
                    InquiryTypeValue(type = type, expanded = typeMenuVisible)
                }

                InquiryField(label = "답변 받을 이메일") {
                    InquiryTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        placeholder = "답변 받을 이메일을 입력해주세요",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }

                InquiryField(
                    label = "문의 내용",
                    modifier = Modifier.height(ContentFieldHeight),
                ) {
                    InquiryTextField(
                        value = content,
                        onValueChange = onContentChange,
                        placeholder = "궁금한 점이나 불편한 점을 자세히 적어주세요",
                        singleLine = false,
                        keyboardOptions = KeyboardOptions.Default,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }

        InquirySubmitButton(
            enabled = isInquiryComplete(type = type, email = email, content = content),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = FieldsHorizontalPadding, vertical = SubmitButtonVerticalPadding),
        )

        if (typeMenuVisible) {
            // 목록 밖을 누르면 닫는다. 밑의 칸·스크롤로는 넘기지 않는다.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onTypeMenuDismiss() })
                    },
            )

            // 시안: 유형 칸 바로 아래에 띄움 없이 붙고, 폭은 칸과 같다.
            val menuWidth = with(LocalDensity.current) { typeFieldBounds.width.toDp() }
            InquiryTypeMenu(
                hazeState = hazeState,
                onTypeClick = onTypeSelected,
                modifier = Modifier
                    .offset { (typeFieldBounds.bottomLeft - screenOrigin).round() }
                    .width(menuWidth),
            )
        }
    }
}

/** 시안: 제목 subtitle1 ↔ 안내 body2 사이 8, 왼쪽 안쪽 4. */
@Composable
private fun InquiryHeading() {
    Column(
        modifier = Modifier.padding(start = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "무엇을 도와드릴까요?",
            style = MoaMapTheme.typography.subtitle1.withDesignLineHeight(),
            color = MoaMapTheme.colors.textNormal,
        )
        Text(
            text = "궁금한 점이나 불편한 점을 알려주세요",
            style = MoaMapTheme.typography.body2.withDesignLineHeight(),
            color = MoaMapTheme.colors.textNormal,
        )
    }
}

/**
 * 제목 + 흰 칸. 시안: 제목 subtitle2(왼쪽 안쪽 2) ↔ 칸 8, 칸은 흰 바탕·모서리 12·그림자 0 0 10 8%.
 * 칸 안 여백은 칸마다 달라 [content] 가 정한다.
 *
 * 안에서 입력하고 있거나 [highlighted] 이면(유형 목록을 열었을 때) 하늘색 테두리를 두른다([MoaMapInputSurface]).
 */
@Composable
private fun InquiryField(
    label: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(FieldTitleGap)) {
        Text(
            text = label,
            style = MoaMapTheme.typography.subtitle2.withDesignLineHeight(),
            color = MoaMapTheme.colors.textNormal,
            modifier = Modifier.padding(start = FieldTitleStartPadding),
        )
        MoaMapInputSurface(
            modifier = modifier.fillMaxWidth(),
            shape = FieldShape,
            shadowBlurRadius = FieldShadowBlurRadius,
            shadowColor = FieldShadowColor,
            highlighted = highlighted,
            onClick = onClick,
        ) {
            content()
        }
    }
}

/** 유형 칸 안. 고른 유형(없으면 안내 문구)과 아래 화살표 24. 목록을 열면 화살표가 위로 뒤집힌다. */
@Composable
private fun InquiryTypeValue(
    type: InquiryType?,
    expanded: Boolean,
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = type?.label ?: "문의 유형을 선택해주세요",
            style = MoaMapTheme.typography.body2.withDesignLineHeight(),
            color = if (type == null) MoaMapTheme.colors.textAssistive else MoaMapTheme.colors.textNormal,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.ic_arrow_down),
            contentDescription = null,
            tint = MoaMapTheme.colors.textAssistive,
            modifier = Modifier
                .size(24.dp)
                .rotate(if (expanded) 180f else 0f),
        )
    }
}

/** [InquiryField] 안에 들어가는 입력칸. 바깥 칸이 배경·그림자·테두리를 이미 그린다. */
@Composable
private fun InquiryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = MoaMapTheme.typography.body2.withDesignLineHeight().copy(
            color = MoaMapTheme.colors.textNormal,
        ),
        cursorBrush = SolidColor(MoaMapTheme.colors.primary),
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        modifier = modifier.fillMaxWidth(),
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

/**
 * 유형 목록. 시안 「모달창」: 바탕 회색 60% + 흐림 5, 모서리 12, 좌우 안쪽 4, 줄 높이 50·안쪽 16,
 * 흰 글자 body2, 줄 사이 1px 선.
 */
@Composable
private fun InquiryTypeMenu(
    hazeState: HazeState,
    onTypeClick: (InquiryType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(TypeMenuShape)
                .hazeEffect(hazeState, TypeMenuBackdropStyle) {
                    // Haze 는 안드로이드 12 미만에서 흐림을 끄고 회색만 깐다. 그러면 밑의 칸 제목이 목록
                    // 글자와 겹쳐 읽히지 않아(10-05 에뮬레이터 API 30 에서 발견), 그 기기에서도 흐린다.
                    blurEnabled = true
                },
        )

        // 첫·마지막 줄을 누른 물결이 둥근 모서리 밖으로 나가지 않게 함께 자른다.
        Column(
            modifier = Modifier
                .clip(TypeMenuShape)
                .padding(horizontal = 4.dp),
        ) {
            InquiryType.entries.forEachIndexed { index, type ->
                if (index > 0) {
                    // 시안 구분선은 높이 0 인 1px 선이라 줄 사이에 자리를 차지하지 않는다.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawLine(
                                    color = MoaMapPrimitiveColors.LineNormal,
                                    start = Offset.Zero,
                                    end = Offset(size.width, 0f),
                                    strokeWidth = 1.dp.toPx(),
                                )
                            },
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TypeMenuRowHeight)
                        .clickable { onTypeClick(type) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = type.label,
                        style = MoaMapTheme.typography.body2,
                        color = MoaMapPrimitiveColors.White,
                    )
                }
            }
        }
    }
}

/** 시안 「Button」 Large. 비활성 Gray200, 활성 primary, 그림자 0 0 10 10%, 글자 button0. */
@Composable
private fun InquirySubmitButton(
    enabled: Boolean,
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
        // 문의 API 가 없어 눌러도 아직 아무것도 하지 않는다(10-05 사용자 결정).
        onClick = if (enabled) ({}) else null,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "문의하기",
                style = MoaMapTheme.typography.button0,
                color = MoaMapTheme.colors.textWhite,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun InquiryScreenPreview() {
    MoaMapTheme {
        InquiryContent(
            type = InquiryType.TECHNICAL,
            email = "moamap@example.com",
            content = "지도를 열면 앱이 멈춥니다.",
            typeMenuVisible = false,
            onBackClick = {},
            onTypeFieldClick = {},
            onTypeSelected = {},
            onTypeMenuDismiss = {},
            onEmailChange = {},
            onContentChange = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393)
@Composable
private fun InquiryTypeMenuPreview() {
    MoaMapTheme {
        val hazeState = rememberHazeState()
        Box(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .hazeSource(hazeState)
                    .background(MoaMapTheme.colors.backgroundSecondary),
            )
            InquiryTypeMenu(
                hazeState = hazeState,
                onTypeClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
