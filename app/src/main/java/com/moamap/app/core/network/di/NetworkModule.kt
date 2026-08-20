package com.moamap.app.core.network.di

import com.moamap.app.BuildConfig
import com.moamap.app.core.auth.AuthTokenStore
import com.moamap.app.core.network.EnvelopeConverterFactory
import com.moamap.app.core.network.authenticator.TokenAuthenticator
import com.moamap.app.core.network.interceptor.AuthInterceptor
import com.moamap.app.core.network.interceptor.ErrorInterceptor
import com.moamap.app.core.network.interceptor.LongRunningTimeoutInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val CONNECT_TIMEOUT_SECONDS = 10L
    private const val READ_TIMEOUT_SECONDS = 15L
    private const val WRITE_TIMEOUT_SECONDS = 15L

    /** 사진 한 장이 몇 MB 라 기본 쓰기 제한으로는 모자란다. */
    private const val UPLOAD_WRITE_TIMEOUT_SECONDS = 60L

    private const val KAKAO_LOCAL_BASE_URL = "https://dapi.kakao.com/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /**
     * 갱신 전용 클라이언트. 인증 인터셉터도 Authenticator 도 붙이지 않는다.
     * 붙이면 갱신 요청이 다시 인증 경로를 타면서 순환한다.
     */
    @Provides
    @Singleton
    @TokenRefreshClient
    fun provideTokenRefreshOkHttpClient(json: Json): OkHttpClient = baseClientBuilder(json).build()

    @Provides
    @Singleton
    @TokenRefreshClient
    fun provideTokenRefreshRetrofit(
        json: Json,
        @TokenRefreshClient okHttpClient: OkHttpClient,
    ): Retrofit = retrofitBuilder(json, okHttpClient)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        json: Json,
        tokenStore: AuthTokenStore,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient = baseClientBuilder(json)
        // ErrorInterceptor 다음에 둬야 한다. ErrorInterceptor 가 가장 바깥에서 실패를 정규화한다.
        .addInterceptor(AuthInterceptor(tokenStore))
        .addInterceptor(LongRunningTimeoutInterceptor())
        // 401 을 받으면 여기서 토큰을 갱신하고 원요청을 재시도한다.
        .authenticator(tokenAuthenticator)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(json: Json, okHttpClient: OkHttpClient): Retrofit =
        retrofitBuilder(json, okHttpClient)

    /** 카카오 로컬 API 는 우리 토큰이 아니라 REST 키를 헤더로 받는다. */
    @Provides
    @Singleton
    @KakaoLocalClient
    fun provideKakaoLocalOkHttpClient(json: Json): OkHttpClient = baseClientBuilder(json)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("Authorization", "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}")
                    .build(),
            )
        }
        .build()

    /**
     * 봉투 변환기를 붙이지 않는다. 카카오는 `{documents, meta}` 를 그대로 주므로
     * `{success, data, error}` 를 벗기려 들면 역직렬화가 깨진다.
     */
    @Provides
    @Singleton
    @KakaoLocalClient
    fun provideKakaoLocalRetrofit(
        json: Json,
        @KakaoLocalClient okHttpClient: OkHttpClient,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(KAKAO_LOCAL_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    /**
     * 로깅을 붙이지 않는다.
     *
     * OkHttp 의 로깅 인터셉터는 바이너리 본문을 찍지는 않지만, 그걸 판별하려고 본문을
     * 버퍼로 한 번 복사한다. 사진을 스트리밍으로 흘려보내는 의미가 없어지고 메모리도
     * 그만큼 더 든다.
     */
    @Provides
    @Singleton
    @PresignedUploadClient
    fun providePresignedUploadOkHttpClient(json: Json): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(ErrorInterceptor(json))
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(UPLOAD_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * 두 클라이언트가 공유하는 최소 구성.
     *
     * ErrorInterceptor 를 가장 바깥에 둬서 안쪽 인터셉터가 던지는 IOException 까지 감싼다.
     * 로깅은 인증 헤더가 붙은 뒤의 최종 요청을 찍도록 가장 안쪽에 둔다.
     */
    private fun baseClientBuilder(json: Json): OkHttpClient.Builder = OkHttpClient.Builder()
        .addInterceptor(ErrorInterceptor(json))
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
        )
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)

    private fun retrofitBuilder(json: Json, okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            // 순서 중요: envelope 을 먼저 벗기고, 남은 페이로드를 kotlinx 가 역직렬화한다.
            .addConverterFactory(EnvelopeConverterFactory(json))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
