package com.budgetmate.app.ui.categories

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.budgetmate.app.data.entity.CategoryEntity
import com.budgetmate.app.databinding.ItemCategoryBinding
import com.budgetmate.app.util.toColorInt
import com.budgetmate.app.util.toZar

class CategoryAdapter(
    private val categories: List<CategoryEntity>,
    private val onEditClick: (CategoryEntity) -> Unit,
    private val onDeleteClick: (CategoryEntity) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.VH>() {

    inner class VH(private val b: ItemCategoryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(cat: CategoryEntity) {
            b.tvIcon.text = cat.iconEmoji
            b.tvName.text = cat.name
            b.tvCap.text  = if (cat.monthlyBudgetCap != null) "Cap: ${cat.monthlyBudgetCap.toZar()}" else "No cap set"
            (b.viewIconBg.background.mutate() as GradientDrawable).setColor(cat.colourHex.toColorInt())
            b.btnEdit.setOnClickListener   { onEditClick(cat) }
            b.btnDelete.setOnClickListener { onDeleteClick(cat) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(categories[position])
    override fun getItemCount() = categories.size
}