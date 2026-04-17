package com.dualsubtv.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dualsubtv.R
import com.dualsubtv.m3u.ContentType
import com.dualsubtv.m3u.M3uEntry

class EntryAdapter(
    private var entries: List<M3uEntry>,
    private val onPlay: (M3uEntry) -> Unit
) : RecyclerView.Adapter<EntryAdapter.VH>() {

    inner class VH(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val badge: TextView       = itemView.findViewById(R.id.tvTypeBadge)
        val title: TextView       = itemView.findViewById(R.id.tvEntryTitle)
        val group: TextView       = itemView.findViewById(R.id.tvEntryGroup)
        val playBtn: ImageButton  = itemView.findViewById(R.id.btnPlayEntry)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_entry, parent, false)
        return VH(v)
    }

    override fun getItemCount() = entries.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val entry = entries[position]
        holder.title.text = entry.title
        holder.group.text = entry.group.ifEmpty { "Sin grupo" }
        holder.badge.text = when (entry.contentType) {
            ContentType.LIVE -> "📡"
            ContentType.VOD  -> "🎬"
            else             -> "▶"
        }
        holder.playBtn.setOnClickListener { onPlay(entry) }
        holder.itemView.setOnClickListener { onPlay(entry) }
    }

    fun updateEntries(newEntries: List<M3uEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }
}
