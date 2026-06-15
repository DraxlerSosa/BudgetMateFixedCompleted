package com.budgetmate.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.budgetmate.app.BudgetMateApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BudgetMateApp

    private val _minGoal = MutableLiveData(0.0)
    val minGoal: LiveData<Double> = _minGoal

    private val _maxGoal = MutableLiveData(0.0)
    val maxGoal: LiveData<Double> = _maxGoal

    fun loadGoals() {
        viewModelScope.launch {
            _minGoal.value = app.sessionManager.minMonthlyGoal.first()
            _maxGoal.value = app.sessionManager.maxMonthlyGoal.first()
        }
    }

    fun saveGoals(min: Double, max: Double) {
        viewModelScope.launch { app.sessionManager.setGoals(min, max) }
    }

    fun logout() {
        viewModelScope.launch { app.sessionManager.logout() }
    }
}
