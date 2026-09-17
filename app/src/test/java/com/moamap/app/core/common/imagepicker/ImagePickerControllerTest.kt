package com.moamap.app.core.common.imagepicker

import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import com.moamap.app.core.common.upload.ALLOWED_IMAGE_CONTENT_TYPES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 사진 선택기에 넘기는 형식 규칙. 선택기가 형식을 하나만 받는다. */
class ImagePickerControllerTest {

    @Test
    fun `형식이 하나면 그 형식만 보여준다`() {
        val type = visualMediaTypeOf(arrayOf("image/png"))

        assertTrue(type is PickVisualMedia.SingleMimeType)
        assertEquals("image/png", (type as PickVisualMedia.SingleMimeType).mimeType)
    }

    @Test
    fun `여러 형식을 받으면 사진 전체를 보여준다`() {
        // 서버가 받는 형식이 셋(JPG·PNG·WEBP)이라 이 경로를 탄다. 거르는 일은 업로드 전 검증이 한다.
        assertEquals(
            PickVisualMedia.ImageOnly,
            visualMediaTypeOf(ALLOWED_IMAGE_CONTENT_TYPES.toTypedArray()),
        )
    }

    @Test
    fun `형식을 좁히지 않았으면 사진 전체를 보여준다`() {
        assertEquals(PickVisualMedia.ImageOnly, visualMediaTypeOf(arrayOf("image/*")))
        assertEquals(PickVisualMedia.ImageOnly, visualMediaTypeOf(emptyArray()))
    }
}
