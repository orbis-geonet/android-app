package com.orbis.orbis.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.orbis.orbis.database.entities.FeedEntity

@Dao
interface FeedDao {

    @Query("SELECT * FROM feed_table WHERE from_nearby = 1 AND city = :city")
    fun getAllCachedFeedsFromNearByOnCity(city: String): List<FeedEntity>

    @Query("SELECT EXISTS(SELECT * FROM feed_table WHERE from_nearby = 1 AND next_page = :page)")
    fun isPageContained(page: String): Boolean

    @Query("DELETE FROM feed_table WHERE from_nearby = 1 AND city = :city")
    fun deleteAllNearByFeeds(city: String)

    @Query("DELETE FROM feed_table WHERE from_nearby = 0 AND city = :city")
    fun deleteAllMyFeeds(city: String)

    @Query("SELECT * FROM feed_table WHERE from_nearby = 0 AND city = :city")
    fun getAllCachedFeedsFromUser(city: String): List<FeedEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFeed(feeds: FeedEntity)

    @Update
    fun updateFeed(feed: FeedEntity)
}