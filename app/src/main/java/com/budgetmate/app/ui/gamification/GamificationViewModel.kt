package com.budgetmate.app.ui.gamification

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.budgetmate.app.BudgetMateApp
import com.budgetmate.app.data.entity.BadgeEntity
import com.budgetmate.app.data.repository.GamificationRepository
import com.budgetmate.app.data.repository.TransactionRepository
import kotlinx.coroutines.launch

class GamificationViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BudgetMateApp
    private val gam = GamificationRepository(
        app.database.userDao(),
        app.database.badgeDao(),
        TransactionRepository(app.database.transactionDao())
    )

    private val _xp     = MutableLiveData(0)
    val xp: LiveData<Int> = _xp

    private val _streak = MutableLiveData(0)
    val streak: LiveData<Int> = _streak

    private val _badges = MutableLiveData<List<BadgeEntity>>(emptyList())
    val badges: LiveData<List<BadgeEntity>> = _badges

    fun load(userId: Int) {
        viewModelScope.launch {
            gam.observeXp(userId).collect    { _xp.value = it }
        }
        viewModelScope.launch {
            gam.observeStreak(userId).collect { _streak.value = it }
        }
        viewModelScope.launch {
            gam.observeBadges(userId).collect { _badges.value = it }
        }
    }
}
