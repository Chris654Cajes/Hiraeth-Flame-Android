package com.hiraeth.flame.ui.albums

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.hiraeth.flame.data.db.AlbumWithMedia
import com.hiraeth.flame.databinding.ItemAlbumBinding
import com.hiraeth.flame.di.AppContainer

class AlbumsAdapter(
    private val container: AppContainer,
    private val onMediaClick: (Long) -> Unit,
) : RecyclerView.Adapter<AlbumsAdapter.VH>() {

    private var items: List<AlbumWithMedia> = emptyList()

    fun submitList(list: List<AlbumWithMedia>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = items[position]
        val binding = holder.binding
        binding.albumName.text = row.album.name
        binding.albumCategory.text = "Category: ${row.album.category}"
        binding.albumCount.text = "${row.media.size} item(s)"

        binding.thumbRow.removeAllViews()
        val d = holder.itemView.resources.displayMetrics.density
        val side = (64 * d).toInt()
        val margin = (6 * d).toInt()
        for (m in row.media.take(4)) {
            val iv = ImageView(holder.itemView.context).apply {
                layoutParams = LinearLayout.LayoutParams(side, side).apply { marginEnd = margin }
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            iv.load(container.mediaStorage.resolveRelative(m.relativePath)) { crossfade(true) }
            iv.setOnClickListener { onMediaClick(m.id) }
            binding.thumbRow.addView(iv)
        }
    }

    override fun getItemCount(): Int = items.size

    class VH(val binding: ItemAlbumBinding) : RecyclerView.ViewHolder(binding.root)
}
