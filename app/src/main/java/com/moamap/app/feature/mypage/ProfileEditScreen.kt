package com.moamap.app.feature.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.moamap.app.R
import com.moamap.app.core.common.imagepicker.rememberImagePickerController
import com.moamap.app.core.common.imagepicker.rememberImagePickerState
import com.moamap.app.core.designsystem.component.ImageSourceMenu
import com.moamap.app.core.designsystem.component.compatibleShadow
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.mypage.presentation.ProfileEditUiState
import com.moamap.app.feature.mypage.presentation.ProfileEditViewModel
import com.moamap.app.feature.mypage.presentation.ProfileLoadState

/** 촬영본이 쌓이는 캐시 위치. `res/xml/profile_image_paths.xml` 의 `cache-path` 와 맞춰야 한다. */
private const val ProfileImageCacheDirectory = "profile_images"
private const val ProfileImageFilePrefix = "profile"

private val ProfileFieldShape = RoundedCornerShape(12.dp)
private val SaveButtonShape = RoundedCornerShape(8.dp)

@Composable
internal fun ProfileEditScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onBackClick()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeError()
        }
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
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }
}

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProfileEditTopBar(onBackClick = onBackClick)
            Spacer(Modifier.height(37.dp))
            ProfileImageEditor(
                imageModel = uiState.pickedImageUri ?: serverImageUrl,
                enabled = !uiState.saving,
                isSourceMenuVisible = pickerState.isSourceMenuVisible,
                onCameraBadgeClick = pickerState::showSourceMenu,
                onMenuDismissRequest = pickerState::dismissSourceMenu,
                onCameraClick = pickerController::requestCamera,
                onGalleryClick = pickerController::requestGallery,
            )
            Spacer(Modifier.height(26.dp))

            when (val load = uiState.load) {
                ProfileLoadState.Loading -> ProfileEditPlaceholder {
                    CircularProgressIndicator(
                        color = MoaMapTheme.colors.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }

                is ProfileLoadState.Error -> ProfileEditPlaceholder {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = load.message,
                            style = MoaMapTheme.typography.body2,
                            color = MoaMapTheme.colors.textAlternative,
                        )
                        Text(
                            text = "다시 시도",
                            style = MoaMapTheme.typography.subtitle2,
                            color = MoaMapTheme.colors.primary,
                            modifier = Modifier.clickable(onClick = onRetryClick),
                        )
                    }
                }

                is ProfileLoadState.Success -> ProfileEditFields(
                    nickname = uiState.nickname,
                    introduction = uiState.introduction,
                    // 카카오 로그인에서 이메일 동의 항목을 못 받고 있어 보여줄 값이 없다.
                    // 권한이 풀리면 아래 인자와 ProfileEditFields 의 이메일 칸을 되살린다.
                    // email = load.email,
                    onNicknameChange = onNicknameChange,
                    onIntroductionChange = onIntroductionChange,
                )
            }
        }

        ProfileSaveButton(
            enabled = uiState.canSave,
            saving = uiState.saving,
            onClick = onSaveClick,
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
private fun ProfileEditTopBar(
    onBackClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(MoaMapPrimitiveColors.White),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .size(48.dp)
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = "뒤로가기",
                tint = MoaMapTheme.colors.textNormal,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = "프로필 편집",
            style = MoaMapTheme.typography.title3,
            color = MoaMapPrimitiveColors.Black,
            modifier = Modifier.align(Alignment.Center),
        )
    }
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
) {
    val density = LocalDensity.current
    val menuOffset = with(density) {
        IntOffset(
            x = 8.dp.roundToPx(),
            y = 142.dp.roundToPx(),
        )
    }

    Box(
        modifier = Modifier
            .width(130.dp)
            .height(134.dp),
    ) {
        ShadowedContainer(
            modifier = Modifier.size(130.dp),
            shape = CircleShape,
            backgroundColor = MoaMapPrimitiveColors.White,
            shadowRadius = 5.dp,
            shadowColor = MoaMapPrimitiveColors.Black.copy(alpha = 0.08f),
        ) {
            imageModel?.let { model ->
                AsyncImage(
                    model = model,
                    contentDescription = "프로필 이미지",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape),
                )
            }
        }

        Box(
            modifier = Modifier
                .offset(x = 98.dp, y = 94.dp)
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
                )
            }
        }
    }
}

@Composable
private fun ProfileEditFields(
    nickname: String,
    introduction: String,
    // email: String,
    onNicknameChange: (String) -> Unit,
    onIntroductionChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ProfileField(
            label = "이름",
            height = 45.dp,
        ) {
            ProfileTextField(
                value = nickname,
                onValueChange = onNicknameChange,
                placeholder = "이름을 작성해주세요",
                singleLine = true,
            )
        }

        ProfileField(
            label = "자기소개",
            optional = true,
            height = 88.dp,
            contentAlignment = Alignment.TopStart,
        ) {
            ProfileTextField(
                value = introduction,
                onValueChange = onIntroductionChange,
                placeholder = "나를 소개하는 한마디를 입력해보세요",
                singleLine = false,
            )
        }

        // 카카오 로그인이 이메일 동의 항목을 못 받아와 서버가 빈 값을 준다. 빈 칸만 덩그러니
        // 보이느니 칸째로 숨긴다. 동의 항목이 풀리면 이 블록과 위의 email 인자를 되살린다.
        // 이메일은 소셜 로그인이 정하는 값이라 되살릴 때도 읽기 전용이어야 하고,
        // "소셜 연동" 배지는 안내일 뿐 누를 수 없다.
        //
        // ProfileField(
        //     label = "이메일",
        //     height = 45.dp,
        //     backgroundColor = MoaMapPrimitiveColors.Yellow50,
        // ) {
        //     Row(
        //         modifier = Modifier.fillMaxWidth(),
        //         verticalAlignment = Alignment.CenterVertically,
        //     ) {
        //         Text(
        //             text = email,
        //             style = MoaMapTheme.typography.body2,
        //             color = MoaMapTheme.colors.textAlternative,
        //             modifier = Modifier.weight(1f),
        //         )
        //         Text(
        //             text = "소셜 연동",
        //             style = MoaMapTheme.typography.caption2,
        //             color = MoaMapPrimitiveColors.Blue700,
        //             modifier = Modifier
        //                 .clip(CircleShape)
        //                 .background(MoaMapPrimitiveColors.Blue100)
        //                 .padding(horizontal = 8.dp, vertical = 4.dp),
        //         )
        //     }
        // }
    }
}

/** [ProfileField] 안에 들어가는 입력칸. 바깥 상자가 배경·그림자·여백을 이미 그린다. */
@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = MoaMapTheme.typography.body2.copy(
            color = MoaMapTheme.colors.textNormal,
        ),
        cursorBrush = SolidColor(MoaMapTheme.colors.primary),
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun ProfileField(
    label: String,
    height: Dp,
    optional: Boolean = false,
    backgroundColor: Color = MoaMapPrimitiveColors.White,
    contentAlignment: Alignment = Alignment.CenterStart,
    content: @Composable BoxScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.height(26.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MoaMapTheme.typography.subtitle1,
                color = MoaMapTheme.colors.textNormal,
            )
            if (optional) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "(선택)",
                    style = MoaMapTheme.typography.body2,
                    color = MoaMapTheme.colors.textAlternative,
                )
            }
        }

        ShadowedContainer(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            shape = ProfileFieldShape,
            backgroundColor = backgroundColor,
            shadowRadius = 5.dp,
            shadowColor = MoaMapPrimitiveColors.Black.copy(alpha = 0.08f),
            contentAlignment = contentAlignment,
            contentPadding = 12.dp,
            horizontalContentPadding = 16.dp,
            content = content,
        )
    }
}

@Composable
private fun ProfileSaveButton(
    enabled: Boolean,
    saving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShadowedContainer(
        modifier = modifier
            .fillMaxWidth()
            .height(49.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = SaveButtonShape,
        backgroundColor = if (enabled) {
            MoaMapTheme.colors.primary
        } else {
            MoaMapPrimitiveColors.Gray100
        },
        shadowRadius = 2.5.dp,
        shadowColor = MoaMapPrimitiveColors.Black.copy(alpha = 0.1f),
        contentAlignment = Alignment.Center,
    ) {
        // 업로드까지 포함하면 저장에 수십 초가 걸릴 수 있어, 버튼이 눌렸다는 것을 계속 보여줘야 한다.
        if (saving) {
            CircularProgressIndicator(
                color = MoaMapTheme.colors.textWhite,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(
                text = "저장하기",
                style = MoaMapTheme.typography.subtitle2,
                color = MoaMapTheme.colors.textWhite,
            )
        }
    }
}

@Composable
private fun ShadowedContainer(
    modifier: Modifier,
    shape: Shape,
    backgroundColor: Color,
    shadowRadius: Dp,
    shadowColor: Color,
    contentAlignment: Alignment = Alignment.CenterStart,
    contentPadding: Dp = 0.dp,
    horizontalContentPadding: Dp = contentPadding,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .compatibleShadow(
                    shape = shape,
                    blurRadius = shadowRadius,
                    color = shadowColor,
                ),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(backgroundColor)
                .padding(
                    horizontal = horizontalContentPadding,
                    vertical = contentPadding,
                ),
            contentAlignment = contentAlignment,
            content = content,
        )
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
