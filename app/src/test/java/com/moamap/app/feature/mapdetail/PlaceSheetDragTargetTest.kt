package com.moamap.app.feature.mapdetail

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceSheetDragTargetTest {

    private fun target(offset: Float, velocity: Float = 0f) = placeSheetDragTarget(
        offset = offset,
        velocity = velocity,
        sheetOffset = SHEET,
        pageLine = PAGE_LINE,
        distanceThreshold = DISTANCE,
        velocityThreshold = VELOCITY,
    )

    @Test
    fun `시트 자리와 거의 끝 사이에서 천천히 놓으면 그 자리에 멈춘다`() {
        assertEquals(PlaceSheetTarget.Stay, target(SHEET - 1f))
        assertEquals(PlaceSheetTarget.Stay, target(PAGE_LINE + 1f))
    }

    @Test
    fun `거의 끝까지 올리고 놓으면 페이지가 된다`() {
        assertEquals(PlaceSheetTarget.Page, target(PAGE_LINE))
        assertEquals(PlaceSheetTarget.Page, target(0f))
    }

    @Test
    fun `시트 자리보다 조금 내리고 놓으면 시트로, 기준 넘게 내리면 닫힌다`() {
        assertEquals(PlaceSheetTarget.Sheet, target(SHEET + DISTANCE - 1f))
        assertEquals(PlaceSheetTarget.Hidden, target(SHEET + DISTANCE))
    }

    @Test
    fun `위로 튕기면 어디서든 페이지로 간다`() {
        assertEquals(PlaceSheetTarget.Page, target(SHEET - 1f, velocity = -VELOCITY))
    }

    @Test
    fun `아래로 튕기면 시트보다 내려와 있을 때만 닫힌다`() {
        assertEquals(PlaceSheetTarget.Hidden, target(SHEET + 1f, velocity = VELOCITY))
        // 올려 둔 시트를 아래로 튕기면 닫히지 않고 시트 자리로 돌아온다.
        assertEquals(PlaceSheetTarget.Sheet, target(PAGE_LINE + 1f, velocity = VELOCITY))
    }

    private companion object {
        const val SHEET = 1000f
        const val PAGE_LINE = 200f
        const val DISTANCE = 150f
        const val VELOCITY = 300f
    }
}
