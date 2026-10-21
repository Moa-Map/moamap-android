package com.moamap.app.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.moamap.app.R

/** 탭(탐색·모음) 상단 바 높이. 시안 「탐색탭_앱바」·「모음탭_앱바」. */
val MoaMapTabTopBarHeight = 58.dp

/** 로고 그림 크기. 그림자까지 담은 시안 틀 그대로라 본체(약 39×32)는 그림 안 왼쪽 4.6·위 7.8 에서 시작한다. */
private val LogoImageSize = 48.dp

/**
 * 탭(탐색·모음) 상단 바 왼쪽의 로고 버튼. 누르면 [onClick].
 *
 * 시안: 높이 58 상단 바의 화면 왼쪽 20·위 5 에 48 틀. 상단 바가 좌우 20 을 주고 가운데 정렬이면 그 자리다.
 */
@Composable
fun MoaMapTopBarLogo(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.img_moa_symbol),
        contentDescription = "홈으로",
        modifier = modifier
            .size(LogoImageSize)
            .clickable(role = Role.Button, onClick = onClick),
    )
}
