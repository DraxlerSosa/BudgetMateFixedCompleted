package com.budgetmate.app

import android.app.Application
import android.util.Log
import com.budgetmate.app.data.database.AppDatabase
import com.budgetmate.app.util.SessionManager

/**
 * Application class — initialises lazy singletons used across the entire app.
 * Declared in AndroidManifest via android:name=".BudgetMateApp".
 */
class BudgetMateApp : Application() {

    companion object {
        private const val TAG = "BudgetMateApp"
    }

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val sessionManager: SessionManager by lazy { SessionManager(this) }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "BudgetMate application started")
    }
}