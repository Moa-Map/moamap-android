package com.moamap.app.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.moamap.app.R

/**
 * 로고 그림 크기. 그림자가 그림 밖으로 번져 로고 본체(43×34.25)보다 크고,
 * 본체는 그림 안 왼쪽 0.75 에서 시작한다.
 */
private val LogoImageSize = DpSize(width = 43.75.dp, height = 35.75.dp)

/**
 * 탭(탐색·모음) 상단 바 왼쪽의 로고 버튼. 누르면 [onClick].
 *
 * 시안 「메인 화면」: 로고 본체가 화면 왼쪽 24, 높이 52 상단 바의 위 9. 상단 바가 좌우 20 을
 * 이미 줬다고 보고, 그림 안 여백만큼 덜 밀어 본체를 그 자리에 맞춘다. 위아래 여백을 합쳐 52 를
 * 채우므로 상단 바가 가운데 정렬이어도 위 9 가 지켜진다.
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
            .padding(start = 3.25.dp, top = 9.dp, bottom = 7.25.dp)
            .size(LogoImageSize)
            .clickable(role = Role.Button, onClick = onClick),
    )
}
