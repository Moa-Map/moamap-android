package com.example.moamap.feature.officialmap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.moamap.R
import com.example.moamap.core.designsystem.component.MapCard
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme

private val ScreenHorizontalPadding = 20.dp

@Immutable
private data class OfficialMapUiModel(
    val id: Long,
    val title: String,
    val description: String,
    val memberCount: String,
    val placeCount: String,
    val joined: Boolean,
)

// TODO: ViewModel 연결 전까지 사용하는 임시 데이터
private val sampleOfficialMaps = List(6) { index ->
    OfficialMapUiModel(
        id = index + 1L,
        title = "서울 팝업스토어 맵",
        description = "매주 업데이트 되는 서울 팝업스토어 정보, 패션, 아트, 뷰티",
        memberCount = "2.3천명",
        placeCount = "128곳",
        joined = index == 1 || index == 4,
    )
}

@Composable
fun OfficialMapScreen(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundPrimary)
            .statusBarsPadding(),
    ) {
        OfficialMapTopBar(onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // TODO: 실제 목록은 ViewModel 연결 시 교체한다.
            sampleOfficialMaps.forEach { officialMap ->
                MapCard(
                    title = officialMap.title,
                    description = officialMap.description,
                    memberCount = officialMap.memberCount,
                    placeCount = officialMap.placeCount,
                    joined = officialMap.joined,
                    verified = true,
                    descriptionStyle = MoaMapTheme.typography.caption0,
                    onClick = {},
                    onJoinClick = {},
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun OfficialMapTopBar(
    onBackClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
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
            text = "공식지도",
            style = MoaMapTheme.typography.title3,
            color = MoaMapPrimitiveColors.Black,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun OfficialMapScreenPreview() {
    MoaMapTheme {
        OfficialMapScreen()
    }
}
