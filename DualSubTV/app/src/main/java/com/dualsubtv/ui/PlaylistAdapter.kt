package com.dualsubtv.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dualsubtv.R
import com.dualsubtv.m3u.SavedPlaylist

class PlaylistAdapter(
    private var playlists: MutableList<SavedPlaylist>,
    private val onOpen: (SavedPlaylist) -> Unit,
    private val onDelete: (SavedPlaylist, Int) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.VH>() {

    inner class VH(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val name:   TextView    = itemView.findViewById(R.id.tvPlaylistName)
        val source: TextView    = itemView.findViewById(R.id.tvPlaylistSource)
        val count:  TextView    = itemView.findViewById(R.id.tvPlaylistCount)
        val delete: ImageButton = itemView.findViewById(R.id.btnDeletePlaylist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist, parent, false)
        return VH(v)
    }

    override fun getItemCount() = playlists.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val pl = playlists[position]
        holder.name.text   = pl.name
        holder.source.text = pl.source
        holder.count.text  = ""
        holder.itemView.setOnClickListener { onOpen(pl) }
        holder.delete.setOnClickListener  { onDelete(pl, holder.adapterPosition) }
    }

    fun removeAt(index: Int) {
        playlists.removeAt(index)
        notifyItemRemoved(index)
    }

    fun update(newList: List<SavedPlaylist>) {
        playlists.clear()
        playlists.addAll(newList)
        notifyDataSetChanged()
    }
}
