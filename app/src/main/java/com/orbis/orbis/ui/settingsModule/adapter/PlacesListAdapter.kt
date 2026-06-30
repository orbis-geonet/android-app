package com.orbis.orbis.ui.settingsModule.adapter


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.R
import com.orbis.orbis.ui.groupsModule.viewModel.Groups
import com.orbis.orbis.ui.settingsModule.viewModel.Place
import com.orbis.orbis.utils.ViewUtils.Companion.colorDrawable
import java.util.*


class PlacesListAdapter(private val interaction: PlaceCardInteraction?, context:Context) :
    RecyclerView.Adapter<PlacesListAdapter.ViewHolder>() {
    private var list: ArrayList<Place>? = null
    private var mContext:Context

    fun setList(list: ArrayList<Place>?) {
        this.list = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.each_place_layout, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindData(position)
    }

    override fun getItemCount(): Int {
        return list?.size!!
    }

    init {
        mContext=context
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardview: ConstraintLayout
        fun bindData(position: Int) {


            cardview.setOnClickListener { view: View? ->
                interaction?.onItemClicked()
            }
        }

        init {
            cardview = itemView.findViewById(R.id.card_id)
        }
    }

    interface PlaceCardInteraction {
        fun onItemClicked()
    }
}
