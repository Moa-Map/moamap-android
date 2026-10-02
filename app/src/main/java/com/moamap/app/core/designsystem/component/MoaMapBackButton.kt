package com.moamap.app.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.moamap.app.R
import com.moamap.app.core.designsystem.theme.MoaMapTheme

/**
 * 상단 바 아이콘이 화면 끝에서 20 에 서도록 누르는 자리(48) 바깥에 두는 여백.
 *
 * 48 칸 가운데에 32 아이콘이 서서 안쪽이 8 이라, 바깥 12 를 더하면 시안 GNB 의 20 이 된다.
 */
val MoaMapTopBarIconEdgePadding = 12.dp

/**
 * 상단 바 뒤로가기. 시안 GNB 「Arrow_left」 32(꺾쇠 자체는 약 8×14).
 *
 * 누르는 자리는 48 이고 아이콘은 그 가운데에 선다. 부르는 쪽이 [MoaMapTopBarIconEdgePadding]
 * 만 띄우면 시안 자리(왼쪽 20)에 온다.
 */
@Composable
fun MoaMapBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MoaMapTheme.colors.textNormal,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_left),
            contentDescription = "뒤로가기",
            tint = tint,
            modifier = Modifier.size(32.dp),
        )
    }
}
