package com.example.gotouchthatgrass_3.db

// db/ChallengeDao.kt

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.gotouchthatgrass_3.models.Challenge
import java.util.*

@Dao
interface ChallengeDao {
    @Query("SELECT * FROM challenges ORDER BY timestamp DESC")
    fun getAllChallenges(): LiveData<List<Challenge>>

    @Query("SELECT * FROM challenges WHERE isSuccessful = 1 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastSuccessfulChallenge(): Challenge?

    @Insert
    suspend fun insert(challenge: Challenge): Long

    @Update
    suspend fun update(challenge: Challenge): Int

    @Delete
    suspend fun delete(challenge: Challenge): Int

    @Query("SELECT COUNT(*) FROM challenges WHERE isSuccessful = 1")
    suspend fun getTotalSuccessfulChallenges(): Int

    @Query("SELECT COUNT(DISTINCT date) FROM challenges WHERE isSuccessful = 1")
    suspend fun getCurrentStreak(): Int
}