package com.moamap.app.core.common.gallery

/**
 * 고른 사진 목록을 바꾼다. 이미 고른 것이면 빼고, 아니면 뒤에 붙인다.
 *
 * [max] 를 채운 뒤 새로 누른 건 무시한다. 조용히 가장 오래된 걸 빼면 방금 고른 게 아니라
 * 엉뚱한 사진이 사라진 것처럼 보인다.
 *
 * 고른 **순서를 유지**한다. 게시물 사진은 첫 장이 카드 썸네일이 되므로 순서가 눈에 보인다.
 *
 * 한 장만 고를 때는 새로 누른 것으로 바꾼다. 무시하면 먼저 고른 걸 빼야 해서 왜 안 눌리는지 모른다.
 */
internal fun <T> toggleSelection(selected: List<T>, item: T, max: Int): List<T> = when {
    item in selected -> selected - item
    max == 1 -> listOf(item)
    selected.size >= max -> selected
    else -> selected + item
}
