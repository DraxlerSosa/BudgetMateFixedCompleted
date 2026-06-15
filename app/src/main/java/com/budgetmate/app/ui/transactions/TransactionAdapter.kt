package com.budgetmate.app.ui.transactions

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.budgetmate.app.R
import com.budgetmate.app.data.entity.CategoryEntity
import com.budgetmate.app.data.entity.TransactionEntity
import com.budgetmate.app.databinding.ItemTransactionBinding
import com.budgetmate.app.util.hide
import com.budgetmate.app.util.show
import com.budgetmate.app.util.toReadableDate
import com.budgetmate.app.util.toZar

/**
 * RecyclerView adapter for the transaction list.
 * Uses DiffUtil for efficient animated updates.
 * Photo thumbnails load via Glide; tapping opens the full-screen viewer.
 */
class TransactionAdapter(
    private val categoryMap: Map<Int, CategoryEntity>,
    private val onPhotoClick: (Uri) -> Unit,
    private val onDeleteClick: (TransactionEntity) -> Unit
) : ListAdapter<TransactionEntity, TransactionAdapter.VH>(Diff()) {

    inner class VH(private val b: ItemTransactionBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(t: TransactionEntity) {
            val cat = categoryMap[t.categoryId]
            b.tvCategoryIcon.text = cat?.iconEmoji ?: "💰"
            b.tvCategoryName.text = cat?.name ?: "Unknown"
            b.tvDate.text = t.date.toReadableDate()
            b.tvTime.text = "${t.startTime} – ${t.endTime}"

            if (t.description.isNullOrBlank()) b.tvDescription.hide()
            else { b.tvDescription.show(); b.tvDescription.text = t.description }

            val isExpense = t.type == "EXPENSE"
            b.tvAmount.text = if (isExpense) "- ${t.amount.toZar()}" else "+ ${t.amount.toZar()}"
            b.tvAmount.setTextColor(b.root.context.getColor(
                if (isExpense) R.color.expense_red else R.color.income_green
            ))

            if (!t.photoUri.isNullOrBlank()) {
                b.ivThumbnail.show()
                Glide.with(b.root).load(Uri.parse(t.photoUri)).centerCrop()
                    .placeholder(R.drawable.ic_photo_placeholder).into(b.ivThumbnail)
                b.ivThumbnail.setOnClickListener { onPhotoClick(Uri.parse(t.photoUri)) }
            } else {
                b.ivThumbnail.hide()
            }

            b.btnDelete.setOnClickListener { onDeleteClick(t) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    private class Diff : DiffUtil.ItemCallback<TransactionEntity>() {
        override fun areItemsTheSame(a: TransactionEntity, b: TransactionEntity) = a.transactionId == b.transactionId
        override fun areContentsTheSame(a: TransactionEntity, b: TransactionEntity) = a == b
    }
}
