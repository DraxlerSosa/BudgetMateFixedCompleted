package com.budgetmate.app.ui.categories

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.budgetmate.app.BudgetMateApp
import com.budgetmate.app.R
import com.budgetmate.app.databinding.FragmentCategoriesBinding
import com.budgetmate.app.util.snack
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Displays the user's spending categories in a 2-column grid.
 *
 * FIX: Uses activityViewModels() so the same ViewModel instance is shared with
 * AddCategoryBottomSheet. This ensures the bottom sheet always has a valid userId
 * without having to re-load it from DataStore on every open.
 */
class CategoriesFragment : Fragment() {

    companion object { private const val TAG = "CategoriesFragment" }

    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!

    // Shared with AddCategoryBottomSheet via activityViewModels
    private val viewModel: CategoriesViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "CategoriesFragment started")

        binding.rvCategories.layoutManager = GridLayoutManager(requireContext(), 2)

        lifecycleScope.launch {
            val userId = (requireActivity().application as BudgetMateApp)
                .sessionManager.loggedInUserId.first()
            Log.d(TAG, "Loading categories for userId=$userId")
            viewModel.load(userId)
        }

        binding.fabAddCategory.setOnClickListener {
            Log.d(TAG, "FAB tapped — opening AddCategoryBottomSheet")
            AddCategoryBottomSheet().show(parentFragmentManager, "ADD_CAT")
        }

        observe()
    }

    private fun observe() {
        viewModel.categories.observe(viewLifecycleOwner) { cats ->
            Log.d(TAG, "Categories updated: ${cats.size} items")

            if (cats.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvCategories.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.rvCategories.visibility = View.VISIBLE
            }

            binding.rvCategories.adapter = CategoryAdapter(
                cats,
                onEditClick = { cat ->
                    Log.d(TAG, "Edit tapped: ${cat.name}")
                    AddCategoryBottomSheet.newInstance(cat).show(parentFragmentManager, "EDIT_CAT")
                },
                onDeleteClick = { cat ->
                    if (cat.isDefault) {
                        Log.w(TAG, "Cannot delete default: ${cat.name}")
                        binding.root.snack(getString(R.string.cannot_delete_default))
                    } else {
                        Log.i(TAG, "Deleting: ${cat.name}")
                        viewModel.deleteCategory(cat)
                    }
                }
            )
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            msg ?: return@observe
            binding.root.snack(msg)
            viewModel.clearError()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
