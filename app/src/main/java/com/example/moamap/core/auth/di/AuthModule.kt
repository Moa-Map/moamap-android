package com.example.moamap.core.auth.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.moamap.core.auth.AuthTokenStore
import com.example.moamap.core.auth.DataStoreAuthTokenStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/** 토큰 전용 DataStore. 다른 Preferences 저장소가 생겨도 서로 섞이지 않게 한정자를 붙인다. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthPreferences

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthTokenStore(impl: DataStoreAuthTokenStore): AuthTokenStore

    companion object {
        private const val AUTH_PREFERENCES_NAME = "auth"

        @Provides
        @Singleton
        @AuthPreferences
        fun provideAuthPreferences(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { context.preferencesDataStoreFile(AUTH_PREFERENCES_NAME) },
        )
    }
}
