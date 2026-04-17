package com.dualsubtv.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dualsubtv.R
import com.dualsubtv.m3u.M3uGroup

class GroupAdapter(
    private val groups: List<M3uGroup>,
    private val onSelect: (M3uGroup, Int) -> Unit
) : RecyclerView.Adapter<GroupAdapter.VH>() {

    private var selectedPos = 0

    inner class VH(val tv: TextView) : RecyclerView.ViewHolder(tv)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val tv = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false) as TextView
        return VH(tv)
    }

    override fun getItemCount() = groups.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val group = groups[position]
        holder.tv.text = "${group.name}  (${group.entries.size})"
        holder.tv.setBackgroundColor(
            if (position == selectedPos)
                holder.tv.context.getColor(R.color.primary)
            else
                android.graphics.Color.TRANSPARENT
        )
        holder.tv.setOnClickListener {
            val old = selectedPos
            selectedPos = holder.adapterPosition
            notifyItemChanged(old)
            notifyItemChanged(selectedPos)
            onSelect(group, selectedPos)
        }
    }
}
