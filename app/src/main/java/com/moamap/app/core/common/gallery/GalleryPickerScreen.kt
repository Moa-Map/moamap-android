package com.moamap.app.core.common.gallery

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.moamap.app.R
import com.moamap.app.core.common.upload.ALLOWED_IMAGE_CONTENT_TYPES
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme

/** 한 번에 읽어 오는 사진 수. 끝에 가까워지면 다음 묶음을 읽는다. */
private const val PageSize = 60

/** 끝에서 이만큼 남으면 다음 묶음을 읽는다. */
private const val LoadMoreThreshold = 12

private const val Columns = 4
private val CellGap = 2.dp
private val SelectionMarkSize = 20.dp

/**
 * 앱 안 갤러리. 기기 사진을 격자로 보여주고 여러 장을 고른다 (시안 `2583:15163`).
 *
 * 첫 칸은 카메라다. 시스템 사진 선택기와 달리 사진 접근 권한이 필요해, 권한이 없으면 목록 대신
 * 안내를 띄운다.
 */
@Composable
internal fun GalleryPickerScreen(
    maxSelectable: Int,
    onCameraClick: () -> Unit,
    onConfirm: (List<Uri>) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "새 게시물",
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasGalleryAccess(context)) }
    var partial by remember { mutableStateOf(hasPartialGalleryAccess(context)) }
    /** 권한을 물어본 적이 있는지. 묻기 전에는 "거부됨" 안내를 띄우지 않는다. */
    var asked by remember { mutableStateOf(false) }

    var images by remember { mutableStateOf<List<DeviceImage>>(emptyList()) }
    var loadedPages by remember { mutableStateOf(0) }
    var endReached by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        asked = true
        granted = hasGalleryAccess(context)
        partial = hasPartialGalleryAccess(context)
        // 허용 범위가 바뀌었다. 보고 있던 목록은 버리고 처음부터 읽는다.
        images = emptyList()
        loadedPages = 0
        endReached = false
    }

    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(galleryPermissions())
    }

    // 첫 묶음과 다음 묶음을 같은 자리에서 읽는다.
    LaunchedEffect(granted, loadedPages) {
        if (!granted || endReached) return@LaunchedEffect

        val loaded = loadDeviceImages(
            context = context,
            mimeTypes = ALLOWED_IMAGE_CONTENT_TYPES,
            limit = PageSize,
            offset = loadedPages * PageSize,
        )
        if (loaded.isEmpty()) {
            endReached = true
        } else {
            images = images + loaded
        }
    }

    val gridState = rememberLazyGridState()
    LaunchedEffect(gridState, images.size) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { lastVisible ->
                if (!endReached && images.isNotEmpty() && lastVisible >= images.size - LoadMoreThreshold) {
                    loadedPages = images.size / PageSize
                }
            }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary)
            // 뒤에 깔린 화면으로 터치가 새지 않게 빈 자리의 탭을 여기서 받는다.
            .pointerInput(Unit) { detectTapGestures() }
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GalleryTopBar(
                title = title,
                confirmEnabled = selected.isNotEmpty(),
                onBackClick = onBackClick,
                onConfirmClick = { onConfirm(selected) },
            )

            Text(
                text = "최근 항목",
                style = MoaMapTheme.typography.subtitle2,
                color = MoaMapTheme.colors.textNormal,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )

            if (partial) {
                MorePhotosRow(onClick = { permissionLauncher.launch(galleryPermissions()) })
            }

            when {
                !granted && asked -> GalleryNotice(
                    message = "사진을 보려면 사진 접근 권한이 필요해요",
                    actionLabel = "설정으로 가기",
                    onActionClick = { context.openAppSettings() },
                )

                !granted -> GalleryNotice(message = "사진 접근 권한을 확인하고 있어요")

                granted && endReached && images.isEmpty() -> GalleryNotice(
                    message = "보여줄 사진이 없어요",
                )

                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(Columns),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(CellGap),
                    verticalArrangement = Arrangement.spacedBy(CellGap),
                ) {
                    item(key = "camera") { CameraCell(onClick = onCameraClick) }

                    items(items = images, key = { image -> image.id }) { image ->
                        val index = selected.indexOf(image.uri)
                        ImageCell(
                            image = image,
                            selected = index >= 0,
                            onClick = {
                                selected = toggleSelection(selected, image.uri, maxSelectable)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryTopBar(
    title: String,
    confirmEnabled: Boolean,
    onBackClick: () -> Unit,
    onConfirmClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
                .size(48.dp)
                .clickable(role = Role.Button, onClick = onBackClick),
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
            text = title,
            style = MoaMapTheme.typography.title3,
            color = MoaMapTheme.colors.textNormal,
            modifier = Modifier.align(Alignment.Center),
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .size(48.dp)
                .clickable(enabled = confirmEnabled, role = Role.Button, onClick = onConfirmClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward),
                contentDescription = "고른 사진 넣기",
                tint = if (confirmEnabled) {
                    MoaMapTheme.colors.textNormal
                } else {
                    MoaMapTheme.colors.textDisable
                },
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** 14 이상에서 일부 사진만 허용한 경우. 여기서 다시 고르게 한다. */
@Composable
private fun MorePhotosRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "허용한 사진만 보여요. 사진 더 고르기",
            style = MoaMapTheme.typography.body2,
            color = MoaMapPrimitiveColors.Blue600,
        )
        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = MoaMapPrimitiveColors.Blue600,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun GalleryNotice(
    message: String,
    actionLabel: String? = null,
    onActionClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = message,
            style = MoaMapTheme.typography.body2,
            color = MoaMapTheme.colors.textAssistive,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null) {
            Text(
                text = actionLabel,
                style = MoaMapTheme.typography.subtitle2,
                color = MoaMapTheme.colors.primary,
                modifier = Modifier.clickable(role = Role.Button, onClick = onActionClick),
            )
        }
    }
}

@Composable
private fun CameraCell(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(MoaMapPrimitiveColors.Gray800)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_photo_camera),
            contentDescription = "사진 찍기",
            tint = MoaMapPrimitiveColors.White,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun ImageCell(
    image: DeviceImage,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(MoaMapPrimitiveColors.Gray50)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { contentDescription = if (selected) "고른 사진" else "사진" },
    ) {
        AsyncImage(
            model = image.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(SelectionMarkSize)
                .clip(CircleShape)
                .background(
                    if (selected) {
                        MoaMapTheme.colors.primary
                    } else {
                        MoaMapPrimitiveColors.Gray200.copy(alpha = 0.6f)
                    },
                )
                .border(width = 1.dp, color = MoaMapPrimitiveColors.White, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = MoaMapPrimitiveColors.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/** 권한을 두 번 거부하면 팝업이 다시 뜨지 않는다. 그때는 설정에서 직접 켜야 한다. */
private fun android.content.Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun GalleryPickerScreenPreview() {
    MoaMapTheme {
        GalleryPickerScreen(
            maxSelectable = 5,
            onCameraClick = {},
            onConfirm = {},
            onBackClick = {},
        )
    }
}
