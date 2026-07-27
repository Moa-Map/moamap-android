package com.example.moamap.feature.onboarding.di

import com.example.moamap.core.auth.TokenRefresher
import com.example.moamap.core.network.di.TokenRefreshClient
import com.example.moamap.feature.onboarding.data.remote.AuthService
import com.example.moamap.feature.onboarding.data.remote.AuthTokenRefresher
import com.example.moamap.feature.onboarding.data.remote.KakaoAuthClient
import com.example.moamap.feature.onboarding.data.remote.KakaoAuthClientImpl
import com.example.moamap.feature.onboarding.data.repository.AuthRepositoryImpl
import com.example.moamap.feature.onboarding.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class OnboardingModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindKakaoAuthClient(impl: KakaoAuthClientImpl): KakaoAuthClient

    @Binds
    @Singleton
    abstract fun bindTokenRefresher(impl: AuthTokenRefresher): TokenRefresher

    companion object {

        /** 로그인·로그아웃용. 로그아웃은 인증이 필요하므로 기본(인증) 클라이언트를 쓴다. */
        @Provides
        @Singleton
        fun provideAuthService(retrofit: Retrofit): AuthService =
            retrofit.create(AuthService::class.java)

        /** 갱신 전용. 인증 인터셉터가 붙지 않은 클라이언트여야 순환하지 않는다. */
        @Provides
        @Singleton
        @TokenRefreshClient
        fun provideTokenRefreshAuthService(
            @TokenRefreshClient retrofit: Retrofit,
        ): AuthService = retrofit.create(AuthService::class.java)
    }
}
