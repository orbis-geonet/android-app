package com.orbis.orbis.ui.placesModule.adapter


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.R
import com.orbis.orbis.ui.settingsModule.viewModel.Place
import com.orbis.orbis.utils.ViewUtils
import java.util.*


class PlacesTypesListAdapter(private val interaction: PlaceCardInteraction?, context:Context) :
    RecyclerView.Adapter<PlacesTypesListAdapter.ViewHolder>() {
    private lateinit var list: IntArray
    private var mContext:Context
    var selectedColor = -1
    fun setList(list: IntArray) {
        this.list = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.each_place_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindData(position)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    init {
        mContext=context
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardview: ConstraintLayout
        private val place_iv: ImageView
        fun bindData(position: Int) {

            if (selectedColor == position) {
                ViewUtils.colorTintImageView(
                    place_iv, ContextCompat.getColor(
                        mContext, R.color.black
                    )
                )
                cardview.elevation = 5f;
            }
            else{
                ViewUtils.colorTintImageView(
                    place_iv, ContextCompat.getColor(
                        mContext, R.color.icons_grey_color
                    )
                )
                cardview.elevation = 0f;
            }

                place_iv.setImageResource(list.get(position))
            cardview.setOnClickListener { view: View? ->
                interaction?.onPlaceTypeClicked(position)
                selectedColor = adapterPosition
                notifyDataSetChanged()
            }
        }

        init {
            cardview = itemView.findViewById(R.id.card_id)
            place_iv = itemView.findViewById(R.id.place_iv)
        }
    }

    interface PlaceCardInteraction {
        fun onPlaceTypeClicked(position: Int)
    }
}
