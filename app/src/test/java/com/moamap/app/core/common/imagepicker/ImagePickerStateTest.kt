package com.moamap.app.core.common.imagepicker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImagePickerStateTest {
    @Test
    fun `source menu can be shown and dismissed`() {
        val state = ImagePickerState()

        state.showSourceMenu()
        assertTrue(state.isSourceMenuVisible)

        state.dismissSourceMenu()
        assertFalse(state.isSourceMenuVisible)
    }

    @Test
    fun `메뉴에서 갤러리를 고르면 메뉴는 닫히고 갤러리가 열린다`() {
        val state = ImagePickerState()
        state.showSourceMenu()

        state.showGallery()

        assertFalse(state.isSourceMenuVisible)
        assertTrue(state.isGalleryVisible)
    }

    @Test
    fun `갤러리를 닫아도 메뉴는 다시 뜨지 않는다`() {
        val state = ImagePickerState(initiallyGalleryVisible = true)

        state.dismissGallery()

        assertFalse(state.isGalleryVisible)
        assertFalse(state.isSourceMenuVisible)
    }
}
