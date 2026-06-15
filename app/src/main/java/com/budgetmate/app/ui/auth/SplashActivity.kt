package com.budgetmate.app.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.budgetmate.app.BudgetMateApp
import com.budgetmate.app.R
import com.budgetmate.app.ui.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Entry point. Checks for an active session then routes to
 * MainActivity (logged in) or AuthActivity (not logged in).
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SplashActivity"
        private const val SPLASH_MS = 1400L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // FIX: Set the layout so the logo actually shows up!
        setContentView(R.layout.activity_splash)
        Log.d(TAG, "Splash started")

        lifecycleScope.launch {
            delay(SPLASH_MS)
            val userId = (application as BudgetMateApp).sessionManager.loggedInUserId.first()
            if (userId != -1) {
                Log.i(TAG, "Session found userId=$userId → MainActivity")
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            } else {
                Log.i(TAG, "No session → AuthActivity")
                startActivity(Intent(this@SplashActivity, AuthActivity::class.java))
            }
            finish()
        }
    }
}