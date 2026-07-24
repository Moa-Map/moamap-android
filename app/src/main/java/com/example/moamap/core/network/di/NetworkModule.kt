package com.example.moamap.core.network.di

import com.example.moamap.BuildConfig
import com.example.moamap.core.network.EnvelopeConverterFactory
import com.example.moamap.core.network.interceptor.ErrorInterceptor
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

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(json: Json): OkHttpClient = OkHttpClient.Builder()
        // ErrorInterceptor 를 가장 바깥에 둬서 안쪽 인터셉터가 던지는 IOException 까지 감싼다.
        .addInterceptor(ErrorInterceptor(json))
        // TODO(다음 이슈): 토큰을 싣는 AuthInterceptor 를 여기에 추가한다.
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
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(json: Json, okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            // 순서 중요: envelope 을 먼저 벗기고, 남은 페이로드를 kotlinx 가 역직렬화한다.
            .addConverterFactory(EnvelopeConverterFactory(json))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
