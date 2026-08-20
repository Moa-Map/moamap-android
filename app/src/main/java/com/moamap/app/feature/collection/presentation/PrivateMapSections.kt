package com.moamap.app.feature.collection.presentation

import com.moamap.app.feature.collection.domain.model.MyMap

/** 프라이빗 탭의 두 섹션. */
internal data class PrivateMapSections(
    val personal: List<MyMap>,
    val others: List<MyMap>,
)

/**
 * 프라이빗 목록을 "나만의 지도" 와 "전체" 로 나눈다.
 *
 * **나만의 지도는 "전체" 에서 뺀다.** 같은 카드가 한 화면에 두 번 뜨면 지도가 두 개인 줄 안다.
 * "전체" 라는 제목과 내용이 어긋나지만, 중복해서 보이는 쪽이 더 나쁘다.
 *
 * `Composable` 안에서 바로 거르지 않고 밖으로 뺐다. 화면 안에 두면 테스트할 수 없다.
 */
internal fun List<MyMap>.splitPersonal(): PrivateMapSections {
    val (personal, others) = partition { map -> map.personal }
    return PrivateMapSections(personal = personal, others = others)
}
