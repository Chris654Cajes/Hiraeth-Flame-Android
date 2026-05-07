package com.hiraeth.flame.ui.reel

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.card.MaterialCardView
import com.hiraeth.flame.data.db.MediaEntity
import com.hiraeth.flame.databinding.ItemReelClipBinding
import com.hiraeth.flame.di.AppContainer

class ReelClipsAdapter(
    private val container: AppContainer,
    private val selectedIds: () -> Set<Long>,
    private val onToggle: (Long) -> Unit,
    private val onOpen: (Long) -> Unit,
) : RecyclerView.Adapter<ReelClipsAdapter.VH>() {

    private var items: List<MediaEntity> = emptyList()

    fun submitList(list: List<MediaEntity>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemReelClipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val file = container.mediaStorage.resolveRelative(item.relativePath)
        holder.binding.thumbnail.load(file) { crossfade(true) }
        holder.binding.title.text = item.displayName
        holder.binding.subtitle.text = if (item.isVideo) "Video · ${item.sizeBytes / 1024} KB" else "Photo · ${item.sizeBytes / 1024} KB"
        val sel = selectedIds().contains(item.id)
        val card = holder.binding.root as MaterialCardView
        val ctx = holder.itemView.context
        val d = ctx.resources.displayMetrics.density
        card.strokeWidth = ((if (sel) 3 else 1) * d).toInt()
        card.strokeColor = if (sel)
            ctx.getColor(com.hiraeth.flame.R.color.neon_violet)
        else
            ctx.getColor(com.hiraeth.flame.R.color.border_subtle)
        holder.itemView.setOnClickListener { onToggle(item.id) }
        holder.binding.btnOpen.setOnClickListener { onOpen(item.id) }
    }

    override fun getItemCount(): Int = items.size

    class VH(val binding: ItemReelClipBinding) : RecyclerView.ViewHolder(binding.root)
}
