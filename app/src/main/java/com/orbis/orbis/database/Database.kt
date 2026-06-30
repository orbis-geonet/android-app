package com.orbis.orbis.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.orbis.orbis.database.dao.FeedDao
import com.orbis.orbis.database.dao.GroupDao
import com.orbis.orbis.database.entities.Converters
import com.orbis.orbis.database.entities.FeedEntity
import com.orbis.orbis.database.entities.GroupEntity

@Database(entities = [FeedEntity::class, GroupEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class Database: RoomDatabase() {

    abstract fun getFeedDao(): FeedDao

    abstract fun getGroupDao(): GroupDao
}