package com.moamap.app.core.common.upload

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** 줄인 사진을 잠깐 두는 곳. 업로드가 끝나면 지우고, 남더라도 시스템이 정리할 수 있는 캐시다. */
private const val UPLOAD_CACHE_DIRECTORY = "photo-upload"

/**
 * 사진을 [COMPRESSION_STEPS] 순서로 줄여 JPEG 임시 파일로 만든다.
 *
 * 원본 해상도로 읽지 않는다. 최근 기기 사진은 원본을 Bitmap 으로 올리면 수십 MB 라, 여러 장을 이어 올리는
 * 저사양 기기에서 터진다. 처음부터 긴 변 [COMPRESSION_STEPS] 첫 단계 크기 근처로 줄여 읽는다.
 *
 * JPEG 로 다시 쓰면서 EXIF 는 남지 않는다. 위치가 그대로 올라가지 않게 하려는 것이고, 대신 회전은 픽셀에
 * 미리 반영한다 - 안 그러면 세로 사진이 눕는다.
 */
@Singleton
internal class PhotoCompressor @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun compress(uri: Uri, maxFileSize: Long): PhotoSpec = withContext(Dispatchers.IO) {
        var step = COMPRESSION_STEPS.first()
        // 다음 단계는 해상도를 낮추기만 하므로 원본 대신 첫 결과에서 줄인다.
        val base = decode(uri, step.maxLongEdge).let { decoded ->
            render(decoded, step.maxLongEdge).also { if (it !== decoded) decoded.recycle() }
        }
        var frame = base

        val file = File.createTempFile("upload-", ".jpg", uploadCacheDirectory())
        try {
            while (true) {
                ensureActive()
                writeJpeg(frame, step.quality, file)

                val next = nextCompressionStep(step, encodedSize = file.length(), maxFileSize = maxFileSize) ?: break
                if (next.maxLongEdge != step.maxLongEdge) {
                    if (frame !== base) frame.recycle()
                    frame = render(base, next.maxLongEdge)
                }
                step = next
            }

            PhotoSpec(file = file, contentType = COMPRESSED_IMAGE_CONTENT_TYPE, size = file.length()).also { photo ->
                validateImageUpload(contentType = photo.contentType, fileSize = photo.size, maxFileSize = maxFileSize)
            }
        } catch (e: Throwable) {
            file.delete()
            throw e
        } finally {
            if (frame !== base) frame.recycle()
            base.recycle()
        }
    }

    private fun uploadCacheDirectory(): File =
        File(context.cacheDir, UPLOAD_CACHE_DIRECTORY).apply { mkdirs() }

    /** 회전을 반영한, 긴 변이 대략 [maxLongEdge] 인 Bitmap. 정확한 크기는 [render] 가 맞춘다. */
    private fun decode(uri: Uri, maxLongEdge: Int): Bitmap = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeWithImageDecoder(uri, maxLongEdge)
        } else {
            decodeWithBitmapFactory(uri, maxLongEdge)
        }
    } catch (e: IOException) {
        throw ImageUploadException.Unreadable()
    }

    /** 목표 크기로 곧바로 읽는다. EXIF 회전은 ImageDecoder 가 반영해 준다. */
    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeWithImageDecoder(uri: Uri, maxLongEdge: Int): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            // 흰 배경에 그려야 해서 하드웨어 Bitmap 은 쓸 수 없다.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val (width, height) = scaledSize(info.size.width, info.size.height, maxLongEdge)
            decoder.setTargetSize(width, height)
        }
    }

    /**
     * API 28 미만. 2의 거듭제곱으로만 줄여 읽을 수 있어 남는 만큼은 읽은 뒤에 맞추고, EXIF 회전은 직접 반영한다.
     *
     * 회전은 줄인 뒤에 한다. 큰 그림을 돌리면 같은 크기를 한 벌 더 만든다.
     */
    private fun decodeWithBitmapFactory(uri: Uri, maxLongEdge: Int): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream -> BitmapFactory.decodeStream(stream, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw ImageUploadException.Unreadable()

        val options = BitmapFactory.Options().apply {
            inSampleSize = decodeSampleSize(bounds.outWidth, bounds.outHeight, maxLongEdge)
        }
        val sampled = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw ImageUploadException.Unreadable()

        val (width, height) = scaledSize(sampled.width, sampled.height, maxLongEdge)
        val scaled = if (width == sampled.width && height == sampled.height) {
            sampled
        } else {
            sampled.scale(width, height).also { sampled.recycle() }
        }

        val matrix = orientationMatrix(readExifOrientation(uri)) ?: return scaled
        return Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true)
            .also { rotated -> if (rotated !== scaled) scaled.recycle() }
    }

    /** 읽지 못하면 회전이 없다고 본다. 회전 정보 때문에 사진을 못 올리게 하지는 않는다. */
    private fun readExifOrientation(uri: Uri): Int = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

    /**
     * 긴 변을 [maxLongEdge] 이하로 맞추고, 투명한 곳은 흰색으로 채운다.
     *
     * JPEG 에는 투명이 없어 그대로 저장하면 투명한 곳이 검게 나온다. 이미 맞는 크기의 불투명한 사진은 그대로 돌려준다.
     */
    private fun render(source: Bitmap, maxLongEdge: Int): Bitmap {
        val (width, height) = scaledSize(source.width, source.height, maxLongEdge)
        if (width == source.width && height == source.height && !source.hasAlpha()) return source

        return createBitmap(width, height).apply {
            Canvas(this).apply {
                drawColor(Color.WHITE)
                drawBitmap(source, null, Rect(0, 0, width, height), Paint(Paint.FILTER_BITMAP_FLAG))
            }
            // 흰 배경을 깔았으니 불투명하다. 다음 단계에서 다시 그리지 않게 표시해 둔다.
            setHasAlpha(false)
        }
    }

    private fun writeJpeg(bitmap: Bitmap, quality: Int, file: File) {
        val written = FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        }
        if (!written) throw ImageUploadException.Unreadable()
    }
}

/** EXIF 방향을 픽셀에 반영할 변환. 그대로 두면 되면 `null`. */
private fun orientationMatrix(orientation: Int): Matrix? = Matrix().apply {
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            setRotate(90f)
            postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            setRotate(-90f)
            postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
        else -> return null
    }
}
