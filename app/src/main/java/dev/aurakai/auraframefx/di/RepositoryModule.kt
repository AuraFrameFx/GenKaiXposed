package dev.aurakai.auraframefx.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.aurakai.auraframefx.cascade.trinity.TrinityRepository
import dev.aurakai.auraframefx.network.AuraApiService
import javax.inject.Singleton

/**
 * Hilt module that provides repository dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /**
     * Provides the [TrinityRepository] implementation.
     *
     * @param apiService The API service for network operations.
     * @return An instance of [TrinityRepository].
     */
    @Provides
    @Singleton
    fun provideTrinityRepository(
        apiService: AuraApiService,
    ): TrinityRepository {
        return TrinityRepository(apiService)
    }
}
