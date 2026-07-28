package com.example.moamap.core.common.imagepicker

import android.Manifest
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

    @Test
    fun `gallery permission follows the Android media permission model`() {
        assertEquals(
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            ),
            galleryPermissionsFor(sdkInt = 34),
        )
        assertEquals(
            listOf(Manifest.permission.READ_MEDIA_IMAGES),
            galleryPermissionsFor(sdkInt = 33),
        )
        assertEquals(
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            galleryPermissionsFor(sdkInt = 32),
        )
    }
}
