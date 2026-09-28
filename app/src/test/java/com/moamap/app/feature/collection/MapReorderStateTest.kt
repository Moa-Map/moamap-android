package com.moamap.app.feature.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class MapReorderStateTest {

    private val moves = mutableListOf<Pair<Long, Long>>()
    private lateinit var state: MapReorderState

    /** 카드 높이 100, 간격 10. 이웃을 넘으려면 (100 + 10) / 2 = 55 를 넘게 끌어야 한다. */
    @Before
    fun setUp() {
        state = MapReorderState(spacingPx = 10f) { mapId, targetId -> moves += mapId to targetId }
        state.sync(listOf(1L, 2L, 3L))
        listOf(1L, 2L, 3L).forEach { id -> state.onSizeChanged(id, 100) }
    }

    @Test
    fun `이웃의 절반을 넘기 전에는 순서를 바꾸지 않는다`() {
        state.start(1L)
        state.drag(50f)

        assertEquals(emptyList<Pair<Long, Long>>(), moves)
        assertEquals(50f, state.dragOffset)
    }

    @Test
    fun `이웃의 절반을 넘으면 자리를 바꾸고 그만큼 오프셋을 되돌린다`() {
        state.start(1L)
        state.drag(60f)

        assertEquals(listOf(1L to 2L), moves)
        assertEquals(-50f, state.dragOffset)
    }

    @Test
    fun `한 번에 크게 끌면 여러 칸을 넘는다`() {
        state.start(1L)
        state.drag(230f)

        assertEquals(listOf(1L to 2L, 1L to 3L), moves)
        assertEquals(0f, state.dragOffset)
    }

    @Test
    fun `목록 끝을 넘어서는 따라가지 않는다`() {
        state.start(1L)
        state.drag(-80f)
        assertEquals(0f, state.dragOffset)

        state.end(1L)
        state.start(3L)
        state.drag(80f)
        assertEquals(0f, state.dragOffset)
        assertEquals(emptyList<Pair<Long, Long>>(), moves)
    }

    @Test
    fun `다른 카드를 놓아도 끌던 카드는 그대로다`() {
        state.start(1L)
        state.drag(20f)

        state.end(2L)
        assertEquals(1L, state.draggingId)

        state.end(1L)
        assertNull(state.draggingId)
        assertEquals(0f, state.dragOffset)
    }
}
