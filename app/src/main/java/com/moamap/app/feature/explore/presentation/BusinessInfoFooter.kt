package com.moamap.app.feature.explore.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moamap.app.R
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight

/**
 * 탐색 탭 맨 아래 사업자 정보. 시안 `3258:17182`.
 *
 * 「사업자 정보」를 누르면 아래 링크 두 줄이 펼쳐진다. 링크는 눌리기는 하지만 이어질 화면이
 * 아직 정해지지 않아 아무 데도 가지 않는다. 시안 맨 아래 「모아맵 서비스~」 글도 내용이 정해지면
 * 펼쳐지는 자리에 들어간다.
 *
 * @param bottomPadding 떠 있는 하단 탭에 가리지 않도록 바탕을 아래로 늘리는 만큼.
 */
@Composable
internal fun BusinessInfoFooter(
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MoaMapTheme.colors.backgroundSecondary)
            .padding(
                start = MoaMapDimens.ScreenHorizontalPadding,
                end = MoaMapDimens.ScreenHorizontalPadding,
                top = 25.dp,
                bottom = bottomPadding,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.img_moa_logo),
                contentDescription = "모아맵",
                modifier = Modifier.size(width = 40.dp, height = 24.dp),
            )
            Row(
                modifier = Modifier.clickable(
                    role = Role.Button,
                    onClickLabel = if (expanded) "접기" else "펼치기",
                ) { expanded = !expanded },
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "사업자 정보",
                    style = MoaMapTheme.typography.button4.withDesignLineHeight(),
                    color = MoaMapTheme.colors.textAlternative,
                )
                // 시안의 dropdown 아이콘은 오른쪽 화살표를 돌린 모양과 같다. 펼치면 위를 본다.
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = MoaMapTheme.colors.textAlternative,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (expanded) -90f else 90f),
                )
            }
        }

        if (expanded) {
            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FooterLinkRow(first = "이용약관", second = "개인정보 처리방침")
                FooterLinkRow(first = "사업자 정보확인", second = "콘텐츠산업진흥법에 의한 표시")
            }
        }
    }
}

@Composable
private fun FooterLinkRow(first: String, second: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FooterLink(text = first)
        Box(
            modifier = Modifier
                .size(width = 1.dp, height = 12.dp)
                .background(MoaMapTheme.colors.textAssistive),
        )
        FooterLink(text = second)
    }
}

/** 이어질 화면이 아직 없다. 눌리기는 하되 아무 데도 가지 않는다. */
@Composable
private fun FooterLink(text: String) {
    Text(
        text = text,
        style = MoaMapTheme.typography.caption0.withDesignLineHeight(),
        color = MoaMapTheme.colors.textAssistive,
        modifier = Modifier.clickable(role = Role.Button, onClick = {}),
    )
}

@Preview(showBackground = true, widthDp = 393)
@Composable
private fun BusinessInfoFooterPreview() {
    MoaMapTheme {
        BusinessInfoFooter(bottomPadding = 25.dp)
    }
}
