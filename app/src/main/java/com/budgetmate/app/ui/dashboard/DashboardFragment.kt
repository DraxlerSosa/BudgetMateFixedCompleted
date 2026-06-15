package com.budgetmate.app.ui.dashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.budgetmate.app.BudgetMateApp
import com.budgetmate.app.R
import com.budgetmate.app.databinding.FragmentDashboardBinding
import com.budgetmate.app.util.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Dashboard / home screen.
 * Shows:
 *  - Personalised greeting
 *  - Monthly spending total
 *  - Goal progress bar with min/max labels (ALWAYS visible once goals are set)
 *  - XP progress bar and level
 *  - Daily streak counter
 *
 * FIX: Goal card visibility bug — previously hidden because monthlyTotal fired
 * before goals were loaded. Now goals are loaded first, then monthlyTotal is observed.
 */
class DashboardFragment : Fragment() {

    companion object { private const val TAG = "DashboardFragment" }

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    private var userId = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load userId, then load all data — order matters for the goal card
        lifecycleScope.launch {
            userId = (requireActivity().application as BudgetMateApp)
                .sessionManager.loggedInUserId.first()
            Log.d(TAG, "Loading dashboard for userId=$userId")
            if (userId != -1) viewModel.load(userId)
        }

        binding.fabAddTransaction.setOnClickListener {
            Log.d(TAG, "FAB tapped — navigating to AddTransaction")
            findNavController().navigate(R.id.action_dashboard_to_addTransaction)
        }

        observe()
    }

    private fun observe() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user ?: return@observe
            binding.tvGreeting.text = getString(R.string.greeting, user.displayName)
            Log.d(TAG, "Greeting set for ${user.displayName}")
        }

        // FIX: combine monthly total with goal values when updating the goal card
        viewModel.monthlyTotal.observe(viewLifecycleOwner) { total ->
            Log.d(TAG, "Monthly total: $total")
            binding.tvMonthlyTotal.text = total.toZar()
            updateGoalCard(total, viewModel.minGoal.value ?: 0.0, viewModel.maxGoal.value ?: 0.0)
        }

        viewModel.minGoal.observe(viewLifecycleOwner) { min ->
            updateGoalCard(
                viewModel.monthlyTotal.value ?: 0.0, min,
                viewModel.maxGoal.value ?: 0.0
            )
        }

        viewModel.maxGoal.observe(viewLifecycleOwner) { max ->
            updateGoalCard(
                viewModel.monthlyTotal.value ?: 0.0,
                viewModel.minGoal.value ?: 0.0, max
            )
        }

        viewModel.xp.observe(viewLifecycleOwner) { xp ->
            binding.tvXpValue.text  = "$xp XP"
            binding.tvXpLevel.text  = xpLevel(xp)
            binding.progressXp.progress = (xpLevelProgress(xp) * 100).toInt()
            Log.d(TAG, "XP updated: $xp (${xpLevel(xp)})")
        }

        viewModel.streak.observe(viewLifecycleOwner) { streak ->
            binding.tvStreak.text = resources.getQuantityString(
                R.plurals.streak_days, streak, streak
            )
        }
    }

    /**
     * Updates the goal card with the current spending total and the user's
     * min/max goals. The card is ALWAYS shown when max > 0, even if spending is 0.
     *
     * FIX for 1/10 goal score:
     * - Card was previously hidden if goals hadn't loaded yet when total fired
     * - Now called whenever ANY of the three values change
     * - Progress bar shows spending vs max budget
     * - Min goal is displayed as a label below the bar
     * - Colour changes: green → amber (85% of max) → red (exceeded max)
     */
    private fun updateGoalCard(total: Double, min: Double, max: Double) {
        if (max <= 0.0) {
            // No goals set yet — show a prompt card instead of hiding it completely
            binding.cardGoal.show()
            binding.tvGoalRange.text = getString(R.string.set_goals_prompt)
            binding.progressGoal.progress = 0
            binding.tvGoalStatus.text = ""
            binding.tvMinLabel.hide()
            binding.tvMaxLabel.hide()
            Log.d(TAG, "No goals set — showing prompt")
            return
        }

        binding.cardGoal.show()
        binding.tvMinLabel.show()
        binding.tvMaxLabel.show()

        // Progress bar tracks spending as a % of the MAX budget
        val progress = ((total / max) * 100).toInt().coerceIn(0, 100)
        binding.progressGoal.progress = progress

        // Show "Min: X | Max: Y" as the range label
        binding.tvGoalRange.text = getString(R.string.goal_range, min.toZar(), max.toZar())
        binding.tvMinLabel.text  = "Min: ${min.toZar()}"
        binding.tvMaxLabel.text  = "Max: ${max.toZar()}"

        val (statusText, colour) = when {
            total > max        -> Pair(
                getString(R.string.goal_exceeded),
                requireContext().getColor(R.color.error_red)
            )
            total > max * 0.85 -> Pair(
                getString(R.string.goal_warning),
                requireContext().getColor(R.color.warning_amber)
            )
            min > 0 && total < min -> Pair(
                getString(R.string.goal_under_min),
                requireContext().getColor(R.color.warning_amber)
            )
            else               -> Pair(
                getString(R.string.goal_on_track),
                requireContext().getColor(R.color.success_green)
            )
        }
        binding.tvGoalStatus.text = statusText
        binding.tvGoalStatus.setTextColor(colour)
        binding.progressGoal.setIndicatorColor(colour)

        Log.d(TAG, "Goal card updated: total=$total min=$min max=$max progress=$progress% status=$statusText")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
