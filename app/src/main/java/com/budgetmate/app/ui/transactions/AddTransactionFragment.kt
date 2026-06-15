package com.budgetmate.app.ui.transactions

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.budgetmate.app.BudgetMateApp
import com.budgetmate.app.R
import com.budgetmate.app.data.entity.CategoryEntity
import com.budgetmate.app.databinding.FragmentAddTransactionBinding
import com.budgetmate.app.util.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Modal bottom sheet for adding a new transaction.
 * Captures: amount, category, date, start/end time, description, optional photo.
 */
class AddTransactionFragment : BottomSheetDialogFragment() {

    companion object { private const val TAG = "AddTransactionFragment" }

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddTransactionViewModel by viewModels()
    private var userId = -1
    private var cameraUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        viewModel.photoUri = uri
        showPhoto(uri)
        Log.d(TAG, "Gallery photo: $uri")
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && cameraUri != null) { viewModel.photoUri = cameraUri; showPhoto(cameraUri!!) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            userId = (requireActivity().application as BudgetMateApp).sessionManager.loggedInUserId.first()
            viewModel.loadCategories(userId)
        }
        setupTypeToggle()
        setupDateTimePickers()
        setupPhoto()
        setupSave()
        observe()
    }

    private fun setupTypeToggle() {
        binding.toggleType.addOnButtonCheckedListener { _, id, checked ->
            if (checked) viewModel.transactionType = if (id == R.id.btnExpense) "EXPENSE" else "INCOME"
        }
        binding.toggleType.check(R.id.btnExpense)
    }

    private fun setupDateTimePickers() {
        binding.btnDate.text = viewModel.selectedDate.toReadableDate()
        binding.btnDate.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                val date = "%04d-%02d-%02d".format(y, m + 1, d)
                viewModel.selectedDate = date
                binding.btnDate.text = date.toReadableDate()
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
                .apply { datePicker.maxDate = System.currentTimeMillis() }
                .show()
        }

        binding.btnStartTime.text = viewModel.selectedStartTime
        binding.btnStartTime.setOnClickListener {
            val (h, m) = viewModel.selectedStartTime.split(":").map { it.toInt() }
            TimePickerDialog(requireContext(), { _, hr, min ->
                viewModel.selectedStartTime = "%02d:%02d".format(hr, min)
                binding.btnStartTime.text   = viewModel.selectedStartTime
            }, h, m, true).show()
        }

        binding.btnEndTime.text = viewModel.selectedEndTime
        binding.btnEndTime.setOnClickListener {
            val (h, m) = viewModel.selectedEndTime.split(":").map { it.toInt() }
            TimePickerDialog(requireContext(), { _, hr, min ->
                viewModel.selectedEndTime = "%02d:%02d".format(hr, min)
                binding.btnEndTime.text   = viewModel.selectedEndTime
            }, h, m, true).show()
        }
    }

    private fun setupPhoto() {
        binding.btnAddPhoto.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_photo)
                .setItems(arrayOf(getString(R.string.take_photo), getString(R.string.choose_from_gallery))) { _, which ->
                    if (which == 0) launchCamera() else galleryLauncher.launch("image/*")
                }.show()
        }
        binding.btnRemovePhoto.setOnClickListener {
            viewModel.photoUri = null
            binding.ivPhotoPreview.hide()
            binding.btnRemovePhoto.hide()
            binding.btnAddPhoto.show()
        }
    }

    private fun launchCamera() {
        val file = File.createTempFile(
            "BM_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}_",
            ".jpg",
            requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        )
        cameraUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
        cameraLauncher.launch(cameraUri)
    }

    private fun showPhoto(uri: Uri) {
        binding.ivPhotoPreview.show()
        binding.btnRemovePhoto.show()
        binding.btnAddPhoto.hide()
        Glide.with(this).load(uri).centerCrop().into(binding.ivPhotoPreview)
    }

    private fun setupSave() {
        binding.btnSave.setOnClickListener {
            binding.tilAmount.error = null
            viewModel.save(userId, binding.etAmount.text.toString(), binding.etDescription.text.toString())
        }
    }

    private fun observe() {
        viewModel.categories.observe(viewLifecycleOwner) { cats -> setupSpinner(cats) }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            val loading = state is AddTransactionViewModel.SaveState.Loading
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnSave.isEnabled = !loading

            when (state) {
                is AddTransactionViewModel.SaveState.Success -> {
                    val e = state.event
                    val msg = buildString {
                        append("Saved! +${e.xpEarned} XP")
                        if (e.streakUpdated) append(" 🔥 ${e.newStreak} day streak!")
                        e.newBadges.firstOrNull()?.let { append(" | ${it.badgeEmoji} ${it.badgeName}!") }
                    }
                    com.google.android.material.snackbar.Snackbar.make(requireView(), msg, com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
                    Log.i(TAG, "Transaction saved")
                    dismiss()
                }
                AddTransactionViewModel.SaveState.InvalidAmount   -> binding.tilAmount.error = getString(R.string.error_invalid_amount)
                AddTransactionViewModel.SaveState.InvalidTime     -> binding.root.snack(getString(R.string.error_end_time))
                AddTransactionViewModel.SaveState.MissingCategory -> binding.root.snack(getString(R.string.error_select_category))
                AddTransactionViewModel.SaveState.MissingDate     -> binding.root.snack(getString(R.string.error_select_date))
                is AddTransactionViewModel.SaveState.Error        -> binding.root.snack(state.msg)
                else -> {}
            }
        }
    }

    private fun setupSpinner(cats: List<CategoryEntity>) {
        val labels = cats.map { "${it.iconEmoji} ${it.name}" }
        binding.spinnerCategory.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, labels)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        val idx = cats.indexOfFirst { it.categoryId == viewModel.selectedCategoryId }
        if (idx >= 0) binding.spinnerCategory.setSelection(idx)
        binding.spinnerCategory.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                viewModel.selectedCategoryId = cats[pos].categoryId
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}