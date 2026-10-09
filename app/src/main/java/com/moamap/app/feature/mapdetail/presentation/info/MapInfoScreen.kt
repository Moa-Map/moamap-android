package com.moamap.app.feature.mapdetail.presentation.info

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.MoaMapBackButton
import com.moamap.app.core.designsystem.component.MoaMapTopBarIconEdgePadding
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import com.moamap.app.feature.collection.domain.model.MapType
import com.moamap.app.feature.mapdetail.domain.model.MapDetail
import com.moamap.app.feature.mapdetail.domain.model.MapRole
import com.moamap.app.feature.mapdetail.presentation.intro.MapIntroHero
import com.moamap.app.feature.mapdetail.presentation.intro.MapIntroSectionTitle
import com.moamap.app.feature.mapdetail.presentation.intro.MapIntroTagRow

/** 시안 히어로 높이. 지도 소개 화면보다 낮다. */
private val MapInfoHeroHeight = 262.dp

/** 히어로 아래 끝에서 제작자 줄까지. 시안: 글 묶음 위 187 + 제목 31.2 + 4 + 제작자 21. */
private val MapInfoHeroTextBottomPadding = 18.8.dp

/** 상단 바 높이. 시안 GNB. */
private val MapInfoTopBarHeight = 58.dp

/**
 * 지도 정보. 지도 상세의 메뉴 「지도 정보」로 들어온다. 고칠 수 있는 사람(방장)에게만 열린다.
 *
 * 지도 소개 화면의 윗부분과 「지도 소개」만 있다 - 작은 지도·장소 목록·참여 버튼은 없다.
 * 지도 상세가 이미 읽어 둔 지도를 그대로 그린다. 제작자는 방장 닉네임이다.
 */
@Composable
internal fun MapInfoScreen(
    map: MapDetail,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary)
            // 뒤에 깔린 지도로 터치가 새지 않게 빈 자리의 탭을 여기서 받는다.
            .pointerInput(Unit) { detectTapGestures() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            MapIntroHero(
                title = map.title,
                ownerName = map.ownerName,
                imageUrl = map.imageUrl,
                height = MapInfoHeroHeight,
                textBottomPadding = MapInfoHeroTextBottomPadding,
            )

            // 소개할 게 없으면 제목만 남은 빈 섹션이 된다. 지도 소개 화면처럼 통째로 뺀다.
            if (map.tags.isNotEmpty() || map.description != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 40.dp, bottom = 20.dp),
                ) {
                    MapIntroSectionTitle("지도 소개")
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (map.tags.isNotEmpty()) {
                            MapIntroTagRow(tags = map.tags)
                        }
                        if (map.description != null) {
                            Text(
                                text = map.description,
                                style = MoaMapTheme.typography.body3.withDesignLineHeight(),
                                color = MoaMapTheme.colors.textAlternative,
                            )
                        }
                    }
                }
            }
        }

        // 히어로 위에 겹치는 상단 바. 흰 아이콘이라야 사진 위에서 읽힌다(지도 소개 화면과 같다).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(MapInfoTopBarHeight),
        ) {
            MoaMapBackButton(
                onClick = onBackClick,
                tint = MoaMapTheme.colors.textWhite,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = MoaMapTopBarIconEdgePadding),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = MoaMapTopBarIconEdgePadding)
                    .size(48.dp)
                    .clickable(role = Role.Button, onClick = onEditClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = "지도 정보 수정",
                    tint = MoaMapTheme.colors.textWhite,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

private val PreviewMap = MapDetail(
    id = 1L,
    title = "숭실대 주변 맛집",
    description = "학교 주변에서 함께 찾아본 맛집을 모아봤어요",
    imageUrl = null,
    ownerName = "모아",
    type = MapType.Community,
    role = MapRole.Owner,
    tags = listOf("맛집", "숭실대", "점심"),
    memberCount = 3,
    placeCount = 12,
    joined = true,
    personal = false,
    inviteCode = null,
)

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MapInfoScreenPreview() {
    MoaMapTheme {
        MapInfoScreen(map = PreviewMap, onBackClick = {}, onEditClick = {})
    }
}
