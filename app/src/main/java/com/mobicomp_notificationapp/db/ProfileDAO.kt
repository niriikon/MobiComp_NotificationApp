package com.mobicomp_notificationapp.db

import androidx.room.*

@Dao
interface ProfileDAO {
    @Transaction
    @Insert
    fun insert(profile: ProfileTable): Long

    @Update
    fun update(profile: ProfileTable)

    @Query("SELECT uid FROM profiles WHERE username = :name")
    fun getIdByName(name: String): Int

    @Query("SELECT * FROM profiles WHERE uid = :uid")
    fun getProfile(uid: Int): ProfileTable

    @Query("SELECT username FROM profiles")
    fun getUsers(): List<String>

    @Query("DELETE FROM profiles WHERE uid = :uid")
    fun delete(uid: Int)
}