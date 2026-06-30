package com.orbis.orbis.helpers.dagger

import android.content.Context
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.network.ApiInterface
import com.orbis.orbis.network.SwaggerApiClient
import com.orbis.orbis.repositories.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DaggerProviders {
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideRetrofitClient(context: Context): Retrofit {
        return SwaggerApiClient.getClient(context)
    }

    @Provides
    @Singleton
    fun provideAuthInterface(retrofit: Retrofit): ApiInterface {
        return retrofit.create(ApiInterface::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthRepo(apiInterface: ApiInterface, prefManager: PrefManager): AuthRepositories {
        return AuthRepositories(apiInterface, prefManager)
    }

    @Provides
    @Singleton
    fun providePlaceRepo(apiInterface: ApiInterface, prefManager: PrefManager): PlaceRepositories {
        return PlaceRepositories(apiInterface, prefManager)
    }

    @Provides
    @Singleton
    fun providePrefManager(context: Context): PrefManager {
        return PrefManager(context)
    }

    @Provides
    @Singleton
    fun provideProfileRepo(
        apiInterface: ApiInterface,
        prefManager: PrefManager
    ): ProfileRepositories {
        return ProfileRepositories(apiInterface, prefManager)
    }

    @Provides
    @Singleton
    fun provideSubscriptionRepo(
        apiInterface: ApiInterface,
        prefManager: PrefManager
    ): SubscriptionRepositories {
        return SubscriptionRepositories(apiInterface, prefManager)
    }
}