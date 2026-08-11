package com.example.moamap.feature.footprint.di

import android.content.Context
import com.example.moamap.feature.footprint.data.WalkSessionFileStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FootprintModule {

    @Provides
    @Singleton
    fun provideWalkSessionFileStore(@ApplicationContext context: Context): WalkSessionFileStore =
        WalkSessionFileStore(File(context.filesDir, "walk-sessions"))
}
