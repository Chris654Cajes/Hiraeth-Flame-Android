package com.hiraeth.flame.ui.library

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.hiraeth.flame.data.db.MediaEntity
import com.hiraeth.flame.databinding.ItemMediaGridBinding
import com.hiraeth.flame.databinding.ItemMediaListBinding
import com.hiraeth.flame.di.AppContainer

class MediaLibraryAdapter(
    private val container: AppContainer,
    private var gridMode: Boolean,
    private val onItemClick: (Long) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<MediaEntity> = emptyList()

    companion object {
        private const val TYPE_GRID = 0
        private const val TYPE_LIST = 1
    }

    fun setGridMode(grid: Boolean) {
        if (gridMode != grid) {
            gridMode = grid
            notifyDataSetChanged()
        }
    }

    fun submitList(list: List<MediaEntity>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (gridMode) TYPE_GRID else TYPE_LIST

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_GRID) {
            GridVH(ItemMediaGridBinding.inflate(inflater, parent, false))
        } else {
            ListVH(ItemMediaListBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        val file = container.mediaStorage.resolveRelative(item.relativePath)
        when (holder) {
            is GridVH -> {
                holder.binding.thumbnail.load(file) { crossfade(true) }
                holder.binding.title.text = item.displayName
                holder.binding.subtitle.text = if (item.isVideo) "Video" else "Photo"
                holder.itemView.setOnClickListener { onItemClick(item.id) }
            }
            is ListVH -> {
                holder.binding.thumbnail.load(file) { crossfade(true) }
                holder.binding.title.text = item.displayName
                holder.binding.subtitle.text =
                    if (item.isVideo) "Video · ${item.sizeBytes / 1024} KB" else "Photo · ${item.sizeBytes / 1024} KB"
                holder.itemView.setOnClickListener { onItemClick(item.id) }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class GridVH(val binding: ItemMediaGridBinding) : RecyclerView.ViewHolder(binding.root)
    class ListVH(val binding: ItemMediaListBinding) : RecyclerView.ViewHolder(binding.root)
}
