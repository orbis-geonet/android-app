package com.orbis.orbis.di

import android.content.Context
import androidx.room.Room
import com.orbis.orbis.database.Database
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {

    private const val ORBIS_DATABASE_NAME = "orbis_database"

    @Singleton
    @Provides
    fun provideRoom(@ApplicationContext context: Context) =
        Room.databaseBuilder(
            context, Database::class.java,
            ORBIS_DATABASE_NAME
        ).build()

    @Singleton
    @Provides
    fun provideFeedDao(db: Database) = db.getFeedDao()

    @Singleton
    @Provides
    fun provideGroupDao(db: Database) = db.getGroupDao()
}