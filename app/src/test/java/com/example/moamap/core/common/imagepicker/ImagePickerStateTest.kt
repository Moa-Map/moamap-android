package com.example.moamap.core.common.imagepicker

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


}
