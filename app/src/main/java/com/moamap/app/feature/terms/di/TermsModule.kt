package com.moamap.app.feature.terms.di

import com.moamap.app.feature.terms.data.repository.BundledTermsRepository
import com.moamap.app.feature.terms.domain.repository.TermsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class TermsModule {

    @Binds
    @Singleton
    abstract fun bindTermsRepository(impl: BundledTermsRepository): TermsRepository
}
