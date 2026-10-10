package com.moamap.app.feature.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.R
import com.moamap.app.core.common.imagepicker.rememberImagePickerController
import com.moamap.app.core.common.imagepicker.rememberImagePickerState
import com.moamap.app.core.designsystem.component.ErrorSnackbar
import com.moamap.app.core.designsystem.component.ImageSourceMenu
import com.moamap.app.core.designsystem.component.MoaMapErrorNotice
import com.moamap.app.core.designsystem.component.MoaMapLargeButton
import com.moamap.app.core.designsystem.component.MoaMapTitleTopBar
import com.moamap.app.core.designsystem.component.MoaMapInputSurface
import com.moamap.app.core.designsystem.component.PhotoThumbnail
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.modifier.dismissKeyboardOnBackgroundTap
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import com.moamap.app.feature.mypage.presentation.ProfileEditUiState
import com.moamap.app.feature.mypage.presentation.ProfileEditViewModel
import com.moamap.app.feature.mypage.presentation.ProfileLoadState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/** 촬영본이 쌓이는 캐시 위치. `res/xml/profile_image_paths.xml` 의 `cache-path` 와 맞춰야 한다. */
private const val ProfileImageCacheDirectory = "profile_images"
private const val ProfileImageFilePrefix = "profile"

private val ProfileFieldShape = RoundedCornerShape(12.dp)

@Composable
internal fun ProfileEditScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onBackClick()
    }

    Box(modifier = modifier.fillMaxSize()) {
        ProfileEditContent(
            uiState = uiState,
            onBackClick = onBackClick,
            onNicknameChange = viewModel::onNicknameChange,
            onIntroductionChange = viewModel::onIntroductionChange,
            onImageSelected = viewModel::onImageSelected,
            onRetryClick = viewModel::load,
            onSaveClick = viewModel::save,
        )
        ErrorSnackbar(
            message = uiState.errorMessage,
            onShown = viewModel::consumeError,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }
}

/** 자기소개가 자라도 이 줄 수까지만 보이고, 그 뒤로는 칸 안에서 스크롤한다. */
private const val IntroductionMaxLines = 6

/** 스크롤 영역 아래 여백. 마지막 칸이 저장 버튼(54 + 위아래 12)에 가리지 않게 한다. */
private val FieldsBottomGap = 101.dp

/** 입력 칸 묶음 좌우 여백·칸 사이. 시안 값. */
private val FieldsHorizontalPadding = 20.dp
private val FieldsGap = 20.dp

/** 칸 제목 ↔ 칸, 칸 제목 왼쪽 안쪽. 시안 「InputField」. */
private val FieldTitleGap = 8.dp
private val FieldTitleStartPadding = 2.dp

/** 프로필 사진 원 지름. 시안 「Home/마이페이지」. */
private val ProfileImageSize = 120.dp

/** 상단 바(58) 아래 → 사진 원. 시안 값. */
private val ProfileImageTopGap = 20.dp

/** 사진 원 → 입력 칸. */
private val ProfileImageBottomGap = 40.dp

@Composable
private fun ProfileEditContent(
    uiState: ProfileEditUiState,
    onBackClick: () -> Unit,
    onNicknameChange: (String) -> Unit,
    onIntroductionChange: (String) -> Unit,
    onImageSelected: (String) -> Unit,
    onRetryClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 고른 사진도 편집 중인 이름·자기소개와 같은 곳(ViewModel)에 둔다.
    val pickerState = rememberImagePickerState()
    val pickerController = rememberImagePickerController(
        state = pickerState,
        cacheDirectoryName = ProfileImageCacheDirectory,
        fileNamePrefix = ProfileImageFilePrefix,
        onImageSelected = { uri -> onImageSelected(uri.toString()) },
    )
    val serverImageUrl = (uiState.load as? ProfileLoadState.Success)?.profileImageUrl
    // 사진 고르기 메뉴가 이 화면을 흐려 바탕으로 깐다.
    val photoMenuHazeState = rememberHazeState()

    Box(
        modifier = modifier
            .dismissKeyboardOnBackgroundTap()
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(photoMenuHazeState)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MoaMapTitleTopBar(title = "프로필 편집", onBackClick = onBackClick)

            // 키보드가 뜨면 이 영역만 줄어든다. 스크롤이 있어야 포커스된 입력칸이 가려지지
            // 않게 스스로 올라온다. 저장 버튼은 화면 아래 자리에 그대로 둔다.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = FieldsBottomGap),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(ProfileImageTopGap))
                ProfileImageEditor(
                    imageModel = uiState.pickedImageUri ?: serverImageUrl,
                    enabled = !uiState.saving,
                    isSourceMenuVisible = pickerState.isSourceMenuVisible,
                    onCameraBadgeClick = pickerState::showSourceMenu,
                    onMenuDismissRequest = pickerState::dismissSourceMenu,
                    onCameraClick = pickerController::requestCamera,
                    onGalleryClick = pickerController::requestGallery,
                    menuHazeState = photoMenuHazeState,
                )
                Spacer(Modifier.height(ProfileImageBottomGap))

                when (val load = uiState.load) {
                    ProfileLoadState.Loading -> ProfileEditPlaceholder {
                        CircularProgressIndicator(
                            color = MoaMapTheme.colors.primary,
                            modifier = Modifier.size(28.dp),
                        )
                    }

                    is ProfileLoadState.Error -> ProfileEditPlaceholder {
                        MoaMapErrorNotice(message = load.message, onRetryClick = onRetryClick)
                    }

                    is ProfileLoadState.Success -> ProfileEditFields(
                        nickname = uiState.nickname,
                        introduction = uiState.introduction,
                        onNicknameChange = onNicknameChange,
                        onIntroductionChange = onIntroductionChange,
                    )
                }
            }
        }

        // 업로드까지 포함하면 저장에 수십 초가 걸릴 수 있어, 버튼이 눌렸다는 것을 계속 보여준다.
        MoaMapLargeButton(
            label = "저장하기",
            onClick = onSaveClick,
            enabled = uiState.canSave,
            submitting = uiState.saving,
            disabledColor = MoaMapPrimitiveColors.Gray100,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

/** 조회가 끝나기 전/실패했을 때 입력 필드 자리를 채운다. */
@Composable
private fun ProfileEditPlaceholder(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

@Composable
private fun ProfileImageEditor(
    /** 고른 사진의 `Uri` 문자열이거나 서버가 준 URL. AsyncImage 가 둘 다 받는다. */
    imageModel: String?,
    enabled: Boolean,
    isSourceMenuVisible: Boolean,
    onCameraBadgeClick: () -> Unit,
    onMenuDismissRequest: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    /** 사진 고르기 메뉴가 흐려 바탕으로 깔 화면. */
    menuHazeState: HazeState? = null,
) {
    val density = LocalDensity.current
    // 사진 고르기 메뉴는 카메라 버튼 아래 8 에, 오른쪽 끝을 버튼(= 원의 네모 칸) 끝에 맞춘다.
    val menuOffset = with(density) {
        IntOffset(
            x = 0,
            y = (ProfileImageSize + 8.dp).roundToPx(),
        )
    }

    // 시안: 원 120, 그림자 0 0 10 8%, 카메라 버튼 40 이 원의 네모 칸 오른쪽 아래 끝에 딱 붙는다.
    Box(modifier = Modifier.size(ProfileImageSize)) {
        ShadowedSurface(
            modifier = Modifier.size(ProfileImageSize),
            shape = CircleShape,
            shadowBlurRadius = 10.dp,
            shadowColor = MoaMapPrimitiveColors.Black.copy(alpha = 0.08f),
        ) {
            // 사진이 없으면 시안 기본 사진. 테두리는 그림자 원이 대신한다.
            PhotoThumbnail(
                imageUrl = imageModel,
                size = ProfileImageSize,
                shape = CircleShape,
                bordered = false,
                contentDescription = "프로필 이미지",
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(40.dp)
                .clip(CircleShape)
                // 저장 중에는 눌러도 반영되지 않으니(ViewModel 가드), 저장 버튼과 같은 죽은 색으로
                // 눌리지 않는다는 것을 보여준다.
                .background(if (enabled) MoaMapTheme.colors.primary else MoaMapPrimitiveColors.Gray100)
                .clickable(enabled = enabled, onClick = onCameraBadgeClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_photo_camera),
                contentDescription = "프로필 사진 변경",
                tint = MoaMapPrimitiveColors.White,
                modifier = Modifier.size(24.dp),
            )
        }

        if (isSourceMenuVisible) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = menuOffset,
                onDismissRequest = onMenuDismissRequest,
                properties = PopupProperties(focusable = true),
            ) {
                ImageSourceMenu(
                    onCameraClick = onCameraClick,
                    onGalleryClick = onGalleryClick,
                    hazeState = menuHazeState,
                )
            }
        }
    }
}

@Composable
private fun ProfileEditFields(
    nickname: String,
    introduction: String,
    onNicknameChange: (String) -> Unit,
    onIntroductionChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FieldsHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(FieldsGap),
    ) {
        ProfileField(label = "이름") {
            ProfileTextField(
                value = nickname,
                onValueChange = onNicknameChange,
                placeholder = "이름을 작성해주세요",
                singleLine = true,
            )
        }

        ProfileField(label = "자기소개", optional = true) {
            // 시안처럼 한 줄 높이에서 시작해 쓰는 만큼 칸이 자란다. [IntroductionMaxLines] 줄부터는
            // 칸 안에서 스크롤한다.
            ProfileTextField(
                value = introduction,
                onValueChange = onIntroductionChange,
                placeholder = "나를 소개하는 한마디를 입력해주세요",
                singleLine = false,
                maxLines = IntroductionMaxLines,
            )
        }

        // 이메일 칸은 두지 않는다. 카카오 로그인이 이메일 동의 항목을 못 받아와 서버가 빈 값을 준다.
        // 동의 항목이 풀리면 읽기 전용 칸 + 「소셜 연동」 배지(#239 시안)로 되살린다.
    }
}

/** [ProfileField] 안에 들어가는 입력칸. 바깥 상자가 배경·그림자·여백을 이미 그린다. */
@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = MoaMapTheme.typography.body2.withDesignLineHeight().copy(
            color = MoaMapTheme.colors.textNormal,
        ),
        cursorBrush = SolidColor(MoaMapTheme.colors.primary),
        singleLine = singleLine,
        maxLines = maxLines,
        modifier = Modifier.fillMaxWidth(),
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
 * 제목 + 입력 칸. 시안 「InputField」: 제목 subtitle2(왼쪽 안쪽 2) ↔ 칸 8, 칸은 흰 바탕·모서리 12·
 * 그림자 0 0 8 4%·안쪽 위아래 12 좌우 16. 높이는 내용이 정한다(한 줄이면 45).
 *
 * 안에서 입력하고 있으면 하늘색 테두리를 두른다([MoaMapInputSurface]).
 */
@Composable
private fun ProfileField(
    label: String,
    optional: Boolean = false,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(FieldTitleGap)) {
        Row(
            modifier = Modifier.padding(start = FieldTitleStartPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MoaMapTheme.typography.subtitle2.withDesignLineHeight(),
                color = MoaMapTheme.colors.textNormal,
            )
            if (optional) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "(선택)",
                    style = MoaMapTheme.typography.body2.withDesignLineHeight(),
                    color = MoaMapTheme.colors.textAlternative,
                )
            }
        }

        MoaMapInputSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = ProfileFieldShape,
        ) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                content()
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun ProfileEditScreenPreview() {
    MoaMapTheme {
        ProfileEditContent(
            uiState = ProfileEditUiState(
                load = ProfileLoadState.Success(
                    email = "moa@example.com",
                    profileImageUrl = null,
                ),
                nickname = "모아맵",
                introduction = "지도 모으는 사람",
            ),
            onBackClick = {},
            onNicknameChange = {},
            onIntroductionChange = {},
            onImageSelected = {},
            onRetryClick = {},
            onSaveClick = {},
        )
    }
}
