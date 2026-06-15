package com.budgetmate.app.ui.dashboard

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.budgetmate.app.BudgetMateApp
import com.budgetmate.app.data.entity.UserEntity
import com.budgetmate.app.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** ViewModel for the Dashboard screen. Drives the monthly summary card and goals indicator. */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    companion object { private const val TAG = "DashboardViewModel" }

    private val app = application as BudgetMateApp
    private val repo = TransactionRepository(app.database.transactionDao())

    private val _user         = MutableLiveData<UserEntity?>()
    val user: LiveData<UserEntity?> = _user

    private val _monthlyTotal = MutableLiveData(0.0)
    val monthlyTotal: LiveData<Double> = _monthlyTotal

    private val _minGoal = MutableLiveData(0.0)
    val minGoal: LiveData<Double> = _minGoal

    private val _maxGoal = MutableLiveData(0.0)
    val maxGoal: LiveData<Double> = _maxGoal

    private val _xp     = MutableLiveData(0)
    val xp: LiveData<Int> = _xp

    private val _streak = MutableLiveData(0)
    val streak: LiveData<Int> = _streak

    fun load(userId: Int) {
        Log.d(TAG, "Loading dashboard for userId=$userId")

        viewModelScope.launch {
            // Ensure DB queries happen off-thread even if DAO is suspend
            val userData = withContext(Dispatchers.IO) {
                app.database.userDao().getUserById(userId)
            }
            _user.value = userData
        }

        val now   = LocalDate.now()
        val year  = "%04d".format(now.year)
        val month = "%02d".format(now.monthValue)

        viewModelScope.launch {
            repo.observeMonthlyTotal(userId, year, month).collect {
                _monthlyTotal.value = it
            }
        }

        viewModelScope.launch {
            app.sessionManager.minMonthlyGoal.collect { _minGoal.value = it }
        }

        viewModelScope.launch {
            app.sessionManager.maxMonthlyGoal.collect { _maxGoal.value = it }
        }

        viewModelScope.launch {
            app.database.userDao().observeXp(userId).collect {
                _xp.value = it
            }
        }

        viewModelScope.launch {
            app.database.userDao().observeStreak(userId).collect {
                _streak.value = it
            }
        }
    }
}
