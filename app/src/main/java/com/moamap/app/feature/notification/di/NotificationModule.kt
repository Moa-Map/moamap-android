package com.moamap.app.feature.notification.di

import com.moamap.app.feature.notification.data.remote.NotificationService
import com.moamap.app.feature.notification.data.repository.NotificationRepositoryImpl
import com.moamap.app.feature.notification.domain.repository.NotificationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    companion object {
        @Provides
        @Singleton
        fun provideNotificationService(retrofit: Retrofit): NotificationService =
            retrofit.create(NotificationService::class.java)
    }
}
