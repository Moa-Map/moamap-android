package com.example.moamap.core.common.imagepicker

import org.junit.Assert.assertEquals
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
    fun `selecting an image replaces the preview and closes the menu`() {
        val state = ImagePickerState(initiallyVisible = true)

        state.selectImage("content://profile/selected")

        assertEquals("content://profile/selected", state.selectedImageUri)
        assertFalse(state.isSourceMenuVisible)
    }

}
