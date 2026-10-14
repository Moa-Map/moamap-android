package com.moamap.app.feature.onboarding.di

import com.moamap.app.core.auth.TokenRefresher
import com.moamap.app.core.network.di.TokenRefreshClient
import com.moamap.app.feature.onboarding.data.remote.AuthService
import com.moamap.app.feature.onboarding.data.remote.AuthTokenRefresher
import com.moamap.app.feature.onboarding.data.remote.KakaoAuthClient
import com.moamap.app.feature.onboarding.data.remote.KakaoAuthClientImpl
import com.moamap.app.feature.onboarding.data.repository.AuthRepositoryImpl
import com.moamap.app.feature.onboarding.domain.repository.AuthRepository
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

        /** 로그인·로그아웃용. 기본 클라이언트지만 두 경로 모두 `AuthInterceptor` 가 토큰을 싣지 않는다(서버도 로그인 없이 받음). */
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
