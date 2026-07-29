package com.example.moamap.core.common.upload.di

import com.example.moamap.core.common.upload.PhotoUploader
import com.example.moamap.core.common.upload.PresignedPhotoUploader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * presigned 업로드는 장소 사진과 지도 커버가 함께 쓴다.
 *
 * 한쪽 feature 모듈이 제공하고 있으면 그 feature 를 정리할 때 다른 쪽이 같이 깨진다.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class UploadModule {

    @Binds
    @Singleton
    abstract fun bindPhotoUploader(impl: PresignedPhotoUploader): PhotoUploader
}
