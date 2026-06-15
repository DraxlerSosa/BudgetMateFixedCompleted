package com.budgetmate.app.ui.gamification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.budgetmate.app.BudgetMateApp
import com.budgetmate.app.databinding.FragmentGamificationBinding
import com.budgetmate.app.util.xpLevel
import com.budgetmate.app.util.xpLevelProgress
import com.budgetmate.app.util.xpToNextLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GamificationFragment : Fragment() {

    private var _binding: FragmentGamificationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GamificationViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGamificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvBadges.layoutManager = GridLayoutManager(requireContext(), 2)

        lifecycleScope.launch {
            val userId = (requireActivity().application as BudgetMateApp).sessionManager.loggedInUserId.first()
            viewModel.load(userId)
        }
        observe()
    }

    private fun observe() {
        viewModel.xp.observe(viewLifecycleOwner) { xp ->
            binding.tvLevel.text    = xpLevel(xp)
            binding.tvXp.text      = "$xp XP"
            binding.tvXpToNext.text = "${xpToNextLevel(xp)} XP to next level"
            binding.progressXp.progress = (xpLevelProgress(xp) * 100).toInt()
        }
        viewModel.streak.observe(viewLifecycleOwner) { streak ->
            binding.tvStreak.text = "$streak day streak 🔥"
        }
        viewModel.badges.observe(viewLifecycleOwner) { badges ->
            binding.tvBadgeCount.text = "${badges.size} badges earned"
            binding.rvBadges.adapter = BadgeAdapter(badges)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
