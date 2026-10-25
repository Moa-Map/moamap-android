package com.moamap.app.feature.mapdetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.moamap.app.R
import com.moamap.app.feature.mapdetail.domain.model.MapRole

/**
 * 역할 뱃지(피그마 「역할뱃지」 `4598:27899`). 모음 카드와 멤버 관리가 함께 쓴다.
 *
 * 방장은 파란 원에 점 셋 달린 왕관, 관리자는 노란 원에 점 없는 왕관. 그 밖의 역할은 그리지 않는다.
 *
 * @param size 그림 크기. 그림은 원(원본 20)에 바깥 흰 테두리를 더한 23 칸이라 원 지름의 1.15배다.
 */
@Composable
internal fun RoleBadge(role: MapRole, size: Dp, modifier: Modifier = Modifier) {
    val (iconRes, label) = when (role) {
        MapRole.Owner -> R.drawable.ic_role_badge_owner to "방장"
        MapRole.Admin -> R.drawable.ic_role_badge_admin to "관리자"
        MapRole.Member, MapRole.None -> return
    }
    Image(
        painter = painterResource(iconRes),
        contentDescription = label,
        modifier = modifier.size(size),
    )
}
