package com.moamap.app.feature.mapdetail

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceSheetDragTargetTest {

    private fun target(offset: Float, velocity: Float = 0f) = placeSheetDragTarget(
        offset = offset,
        velocity = velocity,
        sheetOffset = SHEET,
        distanceThreshold = DISTANCE,
        velocityThreshold = VELOCITY,
    )

    @Test
    fun `조금만 끌고 놓으면 시트 자리로 돌아간다`() {
        assertEquals(PlaceSheetTarget.Sheet, target(SHEET - DISTANCE + 1f))
        assertEquals(PlaceSheetTarget.Sheet, target(SHEET + DISTANCE - 1f))
    }

    @Test
    fun `기준보다 위로 끌면 페이지, 아래로 끌면 닫힌다`() {
        assertEquals(PlaceSheetTarget.Page, target(SHEET - DISTANCE))
        assertEquals(PlaceSheetTarget.Hidden, target(SHEET + DISTANCE))
    }

    @Test
    fun `위로 튕기면 조금만 끌어도 페이지로 간다`() {
        assertEquals(PlaceSheetTarget.Page, target(SHEET - 1f, velocity = -VELOCITY))
    }

    @Test
    fun `아래로 튕기면 시트보다 내려와 있을 때만 닫힌다`() {
        assertEquals(PlaceSheetTarget.Hidden, target(SHEET + 1f, velocity = VELOCITY))
        // 위로 한참 끌었다가 아래로 튕기면 페이지가 아니라 시트로 돌아온다.
        assertEquals(PlaceSheetTarget.Sheet, target(SHEET - DISTANCE * 2, velocity = VELOCITY))
    }

    private companion object {
        const val SHEET = 1000f
        const val DISTANCE = 150f
        const val VELOCITY = 300f
    }
}
