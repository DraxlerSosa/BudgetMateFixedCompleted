package com.budgetmate.app.ui.reports

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.budgetmate.app.BudgetMateApp
import com.budgetmate.app.R
import com.budgetmate.app.databinding.FragmentReportsBinding
import com.budgetmate.app.ui.transactions.TransactionAdapter
import com.budgetmate.app.util.*
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Reports screen — shows filterable transaction list, pie chart by category,
 * and a goal progress indicator comparing spending against min/max goals.
 *
 * NEW for final POE:
 * - Goal banner (progressGoalReport) with min/max labels and colour-coded status
 * - cardChart wrapper for the pie chart
 * - Improved header and layout hierarchy
 */
class ReportsFragment : Fragment() {

    companion object { private const val TAG = "ReportsFragment" }

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReportsViewModel by viewModels()
    private var userId = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "ReportsFragment started")

        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransactions.isNestedScrollingEnabled = false

        lifecycleScope.launch {
            userId = (requireActivity().application as BudgetMateApp)
                .sessionManager.loggedInUserId.first()
            Log.d(TAG, "Reports for userId=$userId")
            viewModel.load(userId, firstDayOfMonth(), lastDayOfMonth())
        }

        setupDateButtons()
        observe()
    }

    private fun setupDateButtons() {
        binding.btnStartDate.text = firstDayOfMonth().toReadableDate()
        binding.btnEndDate.text   = lastDayOfMonth().toReadableDate()

        binding.btnStartDate.setOnClickListener {
            showDatePicker { date ->
                viewModel.startDate = date
                binding.btnStartDate.text = date.toReadableDate()
                reload()
            }
        }
        binding.btnEndDate.setOnClickListener {
            showDatePicker { date ->
                viewModel.endDate = date
                binding.btnEndDate.text = date.toReadableDate()
                reload()
            }
        }
    }

    private fun reload() {
        if (userId != -1) {
            Log.d(TAG, "Reload: ${viewModel.startDate} → ${viewModel.endDate}")
            viewModel.load(userId, viewModel.startDate, viewModel.endDate)
        }
    }

    private fun showDatePicker(onDate: (String) -> Unit) {
        val c = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            onDate("%04d-%02d-%02d".format(y, m + 1, d))
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun observe() {
        // Transaction list
        viewModel.transactions.observe(viewLifecycleOwner) { list ->
            Log.d(TAG, "Transactions: ${list.size}")
            binding.tvNoData.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            val catMap = viewModel.categoryMap.value ?: emptyMap()
            val adapter = TransactionAdapter(
                catMap,
                onPhotoClick = { uri ->
                    Log.d(TAG, "Photo: $uri")
                    findNavController().navigate(
                        R.id.action_reports_to_photoViewer,
                        bundleOf("photoUri" to uri.toString())
                    )
                },
                onDeleteClick = { t ->
                    Log.i(TAG, "Delete transaction ID=${t.transactionId}")
                    viewModel.deleteTransaction(t)
                }
            )
            binding.rvTransactions.adapter = adapter
            adapter.submitList(list)
        }

        // Total spent label
        viewModel.total.observe(viewLifecycleOwner) { total ->
            Log.d(TAG, "Total: $total")
            binding.tvTotal.text = getString(R.string.total_spent, total.toZar())
            updateGoalBanner(total)
        }

        // Pie chart
        viewModel.categoryTotals.observe(viewLifecycleOwner) { totals ->
            if (totals.isEmpty()) {
                binding.cardChart.hide()
                return@observe
            }
            Log.d(TAG, "Pie chart: ${totals.size} categories")
            binding.cardChart.show()

            val entries = totals.map { PieEntry(it.total.toFloat(), it.categoryName) }
            val colours = totals.map { android.graphics.Color.parseColor(it.colourHex) }
            val dataSet = PieDataSet(entries, "").apply {
                colors = colours
                valueTextSize = 11f
                valueTextColor = android.graphics.Color.WHITE
                sliceSpace = 2f
            }
            binding.pieChart.apply {
                data = PieData(dataSet)
                description.isEnabled = false
                legend.isEnabled = true
                legend.textColor = requireContext().getColor(R.color.text_primary)
                setUsePercentValues(true)
                setDrawEntryLabels(false)
                animateY(600)
                invalidate()
            }
        }

        // Goal values from DataStore (collected once)
        lifecycleScope.launch {
            val app = requireActivity().application as BudgetMateApp
            app.sessionManager.minMonthlyGoal.collect { min ->
                viewModel.minGoal = min
                updateGoalBanner(viewModel.total.value ?: 0.0)
            }
        }
        lifecycleScope.launch {
            val app = requireActivity().application as BudgetMateApp
            app.sessionManager.maxMonthlyGoal.collect { max ->
                viewModel.maxGoal = max
                updateGoalBanner(viewModel.total.value ?: 0.0)
            }
        }
    }

    /**
     * Updates the goal progress banner in the Reports summary card.
     * Shows min and max budget as labels under the progress bar.
     * Colour-coded: green = on track, amber = near limit / under min, red = exceeded.
     */
    private fun updateGoalBanner(total: Double) {
        val min = viewModel.minGoal
        val max = viewModel.maxGoal
        if (max <= 0.0) {
            binding.layoutGoalBanner.hide()
            return
        }

        binding.layoutGoalBanner.show()
        val progress = ((total / max) * 100).toInt().coerceIn(0, 100)
        binding.progressGoalReport.progress = progress
        binding.tvGoalMin.text = "Min: ${min.toZar()}"
        binding.tvGoalMax.text = "Max: ${max.toZar()}"

        val (statusText, colour) = when {
            total > max             -> Pair("🚨 Budget exceeded!", requireContext().getColor(R.color.error_red))
            total > max * 0.85      -> Pair("⚠️ Approaching limit", requireContext().getColor(R.color.warning_amber))
            min > 0 && total < min  -> Pair("📉 Below minimum goal", requireContext().getColor(R.color.warning_amber))
            else                    -> Pair("✅ On track", requireContext().getColor(R.color.success_green))
        }
        binding.tvGoalStatus.text = statusText
        binding.tvGoalStatus.setTextColor(colour)
        binding.progressGoalReport.setIndicatorColor(colour)

        Log.d(TAG, "Goal banner: total=$total min=$min max=$max → $statusText")
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
