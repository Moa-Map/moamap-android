package com.moamap.app.feature.mypage.di

import com.moamap.app.feature.mypage.data.remote.UserService
import com.moamap.app.feature.mypage.data.repository.UserRepositoryImpl
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

    companion object {
        // 맵 상세도 작성자 이름을 얻으려고 UserService 를 주입받는다. 여기서 지우면 그쪽이 깨진다.
        @Provides
        @Singleton
        fun provideUserService(retrofit: Retrofit): UserService =
            retrofit.create(UserService::class.java)
    }
}
