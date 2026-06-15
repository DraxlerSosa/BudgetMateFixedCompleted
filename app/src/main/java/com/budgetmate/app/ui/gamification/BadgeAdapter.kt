package com.budgetmate.app.ui.gamification

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.budgetmate.app.data.entity.BadgeEntity
import com.budgetmate.app.databinding.ItemBadgeBinding
import com.budgetmate.app.util.toReadableDate

class BadgeAdapter(private val badges: List<BadgeEntity>) :
    RecyclerView.Adapter<BadgeAdapter.VH>() {

    inner class VH(private val b: ItemBadgeBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(badge: BadgeEntity) {
            b.tvBadgeEmoji.text = badge.badgeEmoji
            b.tvBadgeName.text  = badge.badgeName
            b.tvEarnedDate.text = badge.earnedDate.toReadableDate()
            b.tvXpReward.text   = "+${badge.xpReward} XP"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemBadgeBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(badges[position])
    override fun getItemCount() = badges.size
}
