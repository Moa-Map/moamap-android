package com.moamap.app.feature.mypage.di

import com.moamap.app.feature.mypage.data.remote.InquiryService
import com.moamap.app.feature.mypage.data.remote.NoticeService
import com.moamap.app.feature.mypage.data.remote.UserService
import com.moamap.app.feature.mypage.data.repository.InquiryRepositoryImpl
import com.moamap.app.feature.mypage.data.repository.NoticeRepositoryImpl
import com.moamap.app.feature.mypage.data.repository.UserRepositoryImpl
import com.moamap.app.feature.mypage.domain.repository.InquiryRepository
import com.moamap.app.feature.mypage.domain.repository.NoticeRepository
import com.moamap.app.feature.mypage.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MyPageModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindNoticeRepository(impl: NoticeRepositoryImpl): NoticeRepository

    @Binds
    @Singleton
    abstract fun bindInquiryRepository(impl: InquiryRepositoryImpl): InquiryRepository

    companion object {
        @Provides
        @Singleton
        fun provideNoticeService(retrofit: Retrofit): NoticeService =
            retrofit.create(NoticeService::class.java)

        @Provides
        @Singleton
        fun provideInquiryService(retrofit: Retrofit): InquiryService =
            retrofit.create(InquiryService::class.java)

        // 맵 상세도 작성자 이름을 얻으려고 UserService 를 주입받는다. 여기서 지우면 그쪽이 깨진다.
        @Provides
        @Singleton
        fun provideUserService(retrofit: Retrofit): UserService =
            retrofit.create(UserService::class.java)
    }
}
