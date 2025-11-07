package com.example.dogregistration.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DogDao {
    /**
     * Inserts a new dog profile. If a dog with the same ID
     * already exists, it will be replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDog(dog: DogProfile)

    /**
     * Gets all dog profiles from the database.
     * This is what your "Identify" screen will use to get the list of
     * all known embeddings for comparison.
     */
    @Query("SELECT * FROM dog_profiles")
    suspend fun getAllDogProfiles(): List<DogProfile>
}