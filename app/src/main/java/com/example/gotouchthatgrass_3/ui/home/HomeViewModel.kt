package com.gotouchthatgrass.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.gotouchthatgrass_3.db.AppDatabase
import com.example.gotouchthatgrass_3.models.BlockedApp

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val appDatabase = AppDatabase.getDatabase(application)
    private val blockedAppDao = appDatabase.blockedAppDao()

    fun getBlockedApps(): LiveData<List<BlockedApp>> {
        return blockedAppDao.getCurrentlyBlockedApps()
    }
}