package com.hiraeth.flame.ui.library

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
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
) : ListAdapter<MediaEntity, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private const val TYPE_GRID = 0
        private const val TYPE_LIST = 1

        private val DIFF = object : DiffUtil.ItemCallback<MediaEntity>() {
            override fun areItemsTheSame(old: MediaEntity, new: MediaEntity) = old.id == new.id
            override fun areContentsTheSame(old: MediaEntity, new: MediaEntity) = old == new
        }
    }

    fun setGridMode(grid: Boolean) {
        if (gridMode != grid) {
            gridMode = grid
            notifyDataSetChanged()
        }
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
        val item = getItem(position)
        val file = container.mediaStorage.resolveRelative(item.relativePath)
        when (holder) {
            is GridVH -> {
                holder.binding.thumbnail.load(file) { crossfade(300) }
                holder.binding.title.text = item.displayName
                holder.binding.subtitle.text = if (item.isVideo) "VIDEO" else "PHOTO"
                holder.itemView.setOnClickListener { onItemClick(item.id) }
            }
            is ListVH -> {
                holder.binding.thumbnail.load(file) { crossfade(300) }
                holder.binding.title.text = item.displayName
                val sizeKb = item.sizeBytes / 1024
                holder.binding.subtitle.text =
                    if (item.isVideo) "Video · $sizeKb KB" else "Photo · $sizeKb KB"
                holder.itemView.setOnClickListener { onItemClick(item.id) }
            }
        }
    }

    class GridVH(val binding: ItemMediaGridBinding) : RecyclerView.ViewHolder(binding.root)
    class ListVH(val binding: ItemMediaListBinding) : RecyclerView.ViewHolder(binding.root)
}
