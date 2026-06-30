package com.lassi.presentation.mediadirectory.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lassi.R
import com.lassi.common.extenstions.hide
import com.lassi.common.extenstions.loadImage
import com.lassi.common.extenstions.show
import com.lassi.data.media.MiItemMedia
import com.lassi.databinding.ItemMediaBinding

class FolderAdapter(
    private val onItemClick: (bucket: MiItemMedia) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    private var buckets = ArrayList<MiItemMedia>()

    fun setList(buckets: ArrayList<MiItemMedia>?) {
        buckets?.let {
            this.buckets.clear()
            this.buckets.addAll(it)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FolderViewHolder(binding)
    }

    override fun getItemCount() = buckets.size

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(buckets[position])
    }

    fun clear() {
        val size = buckets.size
        buckets.clear()
        notifyItemRangeRemoved(0, size)
    }

    inner class FolderViewHolder(private val binding: ItemMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(bucket: MiItemMedia) {
            with(bucket) {
                binding.tvFolderName.show()
                binding.tvDuration.hide()
                binding.ivFolderThumbnail.loadImage(bucket.latestItemPathForBucket)
                binding.tvFolderName.text = String.format(
                    binding.tvFolderName.context.getString(R.string.directory_with_item_count),
                    bucketName,
                    totalItemSizeForBucket.toString()
                )
                binding.root.setOnClickListener {
                    onItemClick(bucket)
                }
            }
        }
    }
}