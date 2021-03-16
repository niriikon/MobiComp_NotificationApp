package com.mobicomp_notificationapp.db

import androidx.room.*
import java.util.*

@Dao
interface LocationDAO {
    @Transaction
    @Insert
    fun insert(reminder: LocationTable): Long

    @Update
    fun update(reminder: LocationTable)

    @Query("SELECT * FROM locations WHERE profile_id = :profile")
    fun getLocationsByUser(profile: Int): List<LocationTable>

    @Query("SELECT * FROM locations WHERE profile_id = :profile ORDER BY name")
    fun getLocationsByUserSorted(profile: Int): List<LocationTable>

    @Query("SELECT latitude FROM locations WHERE id = :id")
    fun getLatitudeById(id: Int): Float

    @Query("SELECT longitude FROM locations WHERE id = :id")
    fun getLongitudeById(id: Int): Float

    @Query("SELECT * FROM locations WHERE id = :id")
    fun getLocationById(id: Int): LocationTable

    @Query("SELECT id FROM locations WHERE name LIKE :name")
    fun getIdByName(name: String): Int

    @Query("SELECT name FROM locations WHERE profile_id = :id ORDER BY name")
    fun getNames(id: Int): MutableList<String>

    @Query("DELETE FROM locations WHERE id = :id")
    fun delete(id: Int)
}