package com.budgetmate.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.budgetmate.app.R
import com.budgetmate.app.databinding.FragmentSettingsBinding
import com.budgetmate.app.ui.MainActivity
import com.budgetmate.app.util.snack
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-fill saved goals
        viewModel.minGoal.observe(viewLifecycleOwner) { min ->
            if (min > 0) binding.etMinGoal.setText(min.toString())
        }
        viewModel.maxGoal.observe(viewLifecycleOwner) { max ->
            if (max > 0) binding.etMaxGoal.setText(max.toString())
        }
        viewModel.loadGoals()

        binding.btnSaveGoals.setOnClickListener {
            val min = binding.etMinGoal.text.toString().toDoubleOrNull() ?: 0.0
            val max = binding.etMaxGoal.text.toString().toDoubleOrNull() ?: 0.0
            if (min > max && max > 0) {
                binding.root.snack(getString(R.string.error_min_exceeds_max))
            } else {
                viewModel.saveGoals(min, max)
                binding.root.snack(getString(R.string.goals_saved))
            }
        }

        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirm)
                .setPositiveButton(R.string.logout) { _, _ ->
                    viewModel.logout()
                    (requireActivity() as MainActivity).logout()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
