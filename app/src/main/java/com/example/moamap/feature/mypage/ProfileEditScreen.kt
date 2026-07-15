package com.example.moamap.feature.mypage

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.moamap.R
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme

private val ProfileFieldShape = RoundedCornerShape(12.dp)
private val SaveButtonShape = RoundedCornerShape(8.dp)

@Composable
internal fun ProfileEditScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCameraClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
) {
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
            ProfileImageEditor(onCameraClick = onCameraClick)
            Spacer(Modifier.height(26.dp))
            ProfileEditFields()
        }

        ProfileSaveButton(
            onClick = onSaveClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
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
        Icon(
            painter = painterResource(R.drawable.ic_arrow_left),
            contentDescription = "뒤로가기",
            tint = MoaMapTheme.colors.textNormal,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp)
                .size(24.dp)
                .clickable(onClick = onBackClick),
        )
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
    onCameraClick: () -> Unit,
) {
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
        ) {}

        Box(
            modifier = Modifier
                .offset(x = 98.dp, y = 94.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(MoaMapTheme.colors.primary)
                .clickable(onClick = onCameraClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_photo_camera),
                contentDescription = "프로필 사진 변경",
                tint = MoaMapPrimitiveColors.White,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun ProfileEditFields() {
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
            Text(
                text = "이름을 작성해주세요",
                style = MoaMapTheme.typography.body2,
                color = MoaMapTheme.colors.textAssistive,
            )
        }

        ProfileField(
            label = "자기소개",
            optional = true,
            height = 88.dp,
            contentAlignment = Alignment.TopStart,
        ) {
            Text(
                text = "나를 소개하는 한마디를 입력해보세요",
                style = MoaMapTheme.typography.body2,
                color = MoaMapTheme.colors.textAssistive,
            )
        }

        ProfileField(
            label = "이메일",
            height = 45.dp,
            backgroundColor = MoaMapPrimitiveColors.Yellow50,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "user@example.com",
                    style = MoaMapTheme.typography.body2,
                    color = MoaMapTheme.colors.textAssistive,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "소셜 연동",
                    style = MoaMapTheme.typography.caption2,
                    color = MoaMapPrimitiveColors.Blue700,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MoaMapPrimitiveColors.Blue100)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShadowedContainer(
        modifier = modifier
            .fillMaxWidth()
            .height(49.dp)
            .clickable(onClick = onClick),
        shape = SaveButtonShape,
        backgroundColor = MoaMapTheme.colors.primary,
        shadowRadius = 2.5.dp,
        shadowColor = MoaMapPrimitiveColors.Black.copy(alpha = 0.1f),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "저장하기",
            style = MoaMapTheme.typography.subtitle2,
            color = MoaMapTheme.colors.textWhite,
        )
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
                .blur(
                    radius = shadowRadius,
                    edgeTreatment = BlurredEdgeTreatment.Unbounded,
                )
                .background(
                    color = shadowColor,
                    shape = shape,
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
        ProfileEditScreen(onBackClick = {})
    }
}
