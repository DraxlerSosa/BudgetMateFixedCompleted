package com.budgetmate.app.ui.categories

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import com.budgetmate.app.data.entity.CategoryEntity
import com.budgetmate.app.databinding.FragmentAddCategoryBinding
import com.budgetmate.app.util.snack
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip

/**
 * Bottom sheet for adding or editing a spending category.
 *
 * FIX for 5.4/10 category score:
 * Uses activityViewModels() so it shares the SAME CategoriesViewModel instance
 * as CategoriesFragment — meaning userId is already loaded and valid.
 * Previously used viewModels() which created a fresh ViewModel with userId=-1,
 * causing all saves and edits to silently fail.
 */
class AddCategoryBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val TAG = "AddCategoryBottomSheet"
        private const val ARG_ID       = "categoryId"
        private const val ARG_NAME     = "categoryName"
        private const val ARG_EMOJI    = "emoji"
        private const val ARG_COLOUR   = "colour"
        private const val ARG_CAP      = "cap"
        private const val ARG_DEFAULT  = "isDefault"

        fun newInstance(cat: CategoryEntity) = AddCategoryBottomSheet().apply {
            arguments = bundleOf(
                ARG_ID      to cat.categoryId,
                ARG_NAME    to cat.name,
                ARG_EMOJI   to cat.iconEmoji,
                ARG_COLOUR  to cat.colourHex,
                ARG_CAP     to (cat.monthlyBudgetCap ?: -1.0),
                ARG_DEFAULT to cat.isDefault
            )
        }
    }

    private var _binding: FragmentAddCategoryBinding? = null
    private val binding get() = _binding!!

    // KEY FIX: share the Activity's ViewModel so userId is already set
    private val viewModel: CategoriesViewModel by activityViewModels()

    private var editId: Int = 0
    private var isDefault: Boolean = false
    private var selectedEmoji  = "💰"
    private var selectedColour = "#00C9A7"

    private val emojis = listOf(
        "💰","🛒","🚗","💡","🎬","🍽️","❤️","🛍️","📚","📌",
        "✈️","🏠","🐾","💻","🎵","🏋️","☕","🎮","🌿","🎁"
    )
    private val colours = listOf(
        "#00C9A7","#00B4D8","#FFB703","#8338EC",
        "#FB5607","#E63946","#F72585","#2DC653","#3A86FF","#6C757D"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-fill for edit mode
        editId    = arguments?.getInt(ARG_ID, 0) ?: 0
        isDefault = arguments?.getBoolean(ARG_DEFAULT, false) ?: false

        if (editId > 0) {
            binding.etCategoryName.setText(arguments?.getString(ARG_NAME) ?: "")
            selectedEmoji  = arguments?.getString(ARG_EMOJI)  ?: "💰"
            selectedColour = arguments?.getString(ARG_COLOUR) ?: "#00C9A7"
            val cap = arguments?.getDouble(ARG_CAP, -1.0) ?: -1.0
            if (cap > 0) binding.etCap.setText(cap.toBigDecimal().toPlainString())
            Log.d(TAG, "Edit mode: ID=$editId name=${arguments?.getString(ARG_NAME)}")
        }

        binding.tvSelectedEmoji.text = selectedEmoji
        setupEmojiChips()
        setupColourChips()
        setupSave()

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            msg ?: return@observe
            binding.root.snack(msg)
            viewModel.clearError()
        }
    }

    private fun setupSave() {
        binding.btnSaveCategory.setOnClickListener {
            val name   = binding.etCategoryName.text.toString().trim()
            val capStr = binding.etCap.text.toString().trim()
            val cap    = if (capStr.isBlank()) null else capStr.toDoubleOrNull()

            if (name.isBlank()) {
                binding.tilCategoryName.error = "Name cannot be empty"
                return@setOnClickListener
            }
            binding.tilCategoryName.error = null

            if (editId > 0) {
                Log.i(TAG, "Updating category ID=$editId → $name")
                viewModel.updateCategory(
                    CategoryEntity(categoryId = editId, userId = 0, name = name,
                        iconEmoji = selectedEmoji, colourHex = selectedColour,
                        monthlyBudgetCap = cap, isDefault = isDefault),
                    name, selectedEmoji, selectedColour, cap
                )
            } else {
                Log.i(TAG, "Adding new category: $name")
                viewModel.addCategory(name, selectedEmoji, selectedColour, cap)
            }
            dismiss()
        }
    }

    private fun setupEmojiChips() {
        binding.chipGroupEmoji.removeAllViews()
        emojis.forEach { emoji ->
            val chip = Chip(requireContext()).apply {
                text = emoji
                isCheckable = true
                isChecked = (emoji == selectedEmoji)
                setOnClickListener {
                    selectedEmoji = emoji
                    binding.tvSelectedEmoji.text = emoji
                }
            }
            binding.chipGroupEmoji.addView(chip)
        }
    }

    private fun setupColourChips() {
        binding.chipGroupColour.removeAllViews()
        colours.forEach { hex ->
            val chip = Chip(requireContext()).apply {
                text = "  "
                isCheckable = true
                isChecked = (hex == selectedColour)
                try {
                    chipBackgroundColor = android.content.res.ColorStateList.valueOf(Color.parseColor(hex))
                } catch (e: Exception) { Log.e(TAG, "Bad colour: $hex") }
                setOnClickListener { selectedColour = hex }
            }
            binding.chipGroupColour.addView(chip)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
