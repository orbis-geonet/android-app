package com.orbis.orbis.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.orbis.orbis.database.entities.GroupEntity

@Dao
interface GroupDao {

    @Query("SELECT * FROM groups_table WHERE city = :city AND from_logged_user = :loggedIn")
    fun getCachedGroups(city: String, loggedIn: Boolean): List<GroupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGroups(groups: List<GroupEntity>)

    @Query("DELETE FROM groups_table WHERE city = :city AND from_logged_user = :loggedIn")
    fun deleteCachedGroups(city: String, loggedIn: Boolean)

    @Update
    fun updateGroups(groups: List<GroupEntity>)

    @Update
    fun updateGroup(group: GroupEntity)
}