package com.orbis.orbis.ui.groupsModule.adapter


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.R
import com.orbis.orbis.utils.ViewUtils
import com.orbis.orbis.utils.customViews.RoundLinerLayoutNormal


class GroupColorsAdapter(
    private val interaction: ColorCardInteraction?,
    context: Context,
    private var selectedColor: Int = -1
) :
    RecyclerView.Adapter<GroupColorsAdapter.ViewHolder>() {
    private lateinit var list: IntArray
    private var mContext: Context


    fun setList(list: IntArray) {
        this.list = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.each_color_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindData(position)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    init {
        mContext = context
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardview: ConstraintLayout
        private val inner_circle: RoundLinerLayoutNormal
        private val shadow_circle: RoundLinerLayoutNormal
        fun bindData(position: Int) {

            if (selectedColor == position) {

                shadow_circle.updateShadowColor(
                    ContextCompat.getColor(
                        mContext,
                        list.get(position)
                    )
                )
                shadow_circle.updateCurrElevation(mContext.resources.getDimensionPixelSize(R.dimen.elevation_6dp))

            } else {
                shadow_circle.updateShadowColor(
                    ContextCompat.getColor(
                        mContext,
                        R.color.white
                    )
                )
                shadow_circle.updateCurrElevation(mContext.resources.getDimensionPixelSize(R.dimen.elevation_2dp))


            }
            inner_circle.updateShadowBkgColor(
                ContextCompat.getColor(
                    mContext,
                    list.get(position)
                )
            )
            cardview.setOnClickListener { view: View? ->
                interaction?.onItemClicked(adapterPosition)
                selectedColor = adapterPosition
                notifyDataSetChanged()
            }
        }

        init {
            cardview = itemView.findViewById(R.id.card_id)
            inner_circle = itemView.findViewById(R.id.inner_circle)
            shadow_circle = itemView.findViewById(R.id.shadow_circle)
        }
    }

    interface ColorCardInteraction {
        fun onItemClicked(position: Int)
    }
}
