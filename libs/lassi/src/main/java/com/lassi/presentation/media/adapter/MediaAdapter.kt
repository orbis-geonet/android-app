package com.lassi.presentation.media.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.lassi.common.extenstions.loadImage
import com.lassi.common.utils.DurationUtils.getDuration
import com.lassi.common.utils.ImageUtils
import com.lassi.common.utils.Logger
import com.lassi.data.media.MiMedia
import com.lassi.databinding.ItemMediaBinding
import com.lassi.domain.media.LassiConfig

class MediaAdapter(
    private val onItemClick: (selectedMedias: ArrayList<MiMedia>) -> Unit
) : RecyclerView.Adapter<MediaAdapter.MyViewHolder>() {

    private val logTag = "LassiMediaAdapter"
    private val images = ArrayList<MiMedia>()

    fun setList(images: ArrayList<MiMedia>?) {
        if (images != null) {
            this.images.clear()
            this.images.addAll(images.reversed())
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun getItemCount() = images.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(images[position])
    }

    private fun addSelected(image: MiMedia, position: Int) {
        with(LassiConfig.getConfig()) {
            if (selectedMedias.size != maxCount) {
                selectedMedias.add(image)
                notifyItemChanged(position)
            }
        }
    }

    fun removeSelected(image: MiMedia, position: Int) {
        if (LassiConfig.getConfig().selectedMedias.remove(image)) {
            Logger.d(logTag, "removeSelected ${image.path}")
            notifyItemChanged(position)
        } else {
            Logger.d(logTag, "not removeSelected ${image.path}")
        }
    }

    inner class MyViewHolder(private val binding: ItemMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(miMedia: MiMedia) {
            with(miMedia) {
                var isSelect = isSelected(this)
                binding.tvFolderName.text = miMedia.name
                binding.viewAlpha.alpha = if (isSelect) 0.5f else 0.0f
                binding.ivSelect.setImageResource(LassiConfig.getConfig().selectionDrawable)
                binding.ivSelect.isVisible = isSelect
                binding.ivFolderThumbnail.loadImage(ImageUtils.getThumb(this))
                if (duration != 0L) {
                    binding.tvDuration.visibility = View.VISIBLE
                    binding.tvDuration.text = getDuration(duration)
                }

                binding.root.setOnClickListener {
                    if (LassiConfig.getConfig().maxCount > 1) {
                        isSelect = !isSelect
                        if (!isSelect) {
                            removeSelected(miMedia, absoluteAdapterPosition)
                        } else {
                            addSelected(miMedia, absoluteAdapterPosition)
                        }
                    } else {
                        with(LassiConfig.getConfig()) {
                            if (selectedMedias.size != maxCount) {
                                selectedMedias.add(0, miMedia)
                            } else {
                                selectedMedias[0] = miMedia
                            }
                        }
                    }
                    onItemClick(LassiConfig.getConfig().selectedMedias)
                }
            }
        }

        private fun isSelected(image: MiMedia): Boolean {
            return LassiConfig.getConfig().selectedMedias.any { it.path == image.path }
        }
    }
}