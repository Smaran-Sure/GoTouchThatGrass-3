package com.example.gotouchthatgrass_3.ui.stats


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.gotouchthatgrass_3.db.AppDatabase
import com.example.gotouchthatgrass_3.models.Challenge

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val appDatabase = AppDatabase.getDatabase(application)
    private val challengeDao = appDatabase.challengeDao()

    fun getAllChallenges(): LiveData<List<Challenge>> {
        return challengeDao.getAllChallenges()
    }
}