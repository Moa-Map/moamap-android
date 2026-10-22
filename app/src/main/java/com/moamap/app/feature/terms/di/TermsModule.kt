package com.moamap.app.feature.terms.di

import com.moamap.app.feature.terms.data.remote.TermsService
import com.moamap.app.feature.terms.data.repository.TermsRepositoryImpl
import com.moamap.app.feature.terms.domain.repository.TermsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class TermsModule {

    @Binds
    @Singleton
    abstract fun bindTermsRepository(impl: TermsRepositoryImpl): TermsRepository

    companion object {
        @Provides
        @Singleton
        fun provideTermsService(retrofit: Retrofit): TermsService =
            retrofit.create(TermsService::class.java)
    }
}
