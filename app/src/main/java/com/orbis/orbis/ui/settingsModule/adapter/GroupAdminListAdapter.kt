package com.orbis.orbis.ui.settingsModule.adapter


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
import com.orbis.orbis.ui.groupsModule.viewModel.Groups
import com.orbis.orbis.utils.ViewUtils.Companion.colorDrawable
import java.util.*


class GroupAdminListAdapter(private val interaction: GroupsCardInteraction?, context:Context) :
    RecyclerView.Adapter<GroupAdminListAdapter.ViewHolder>() {
    private var list: ArrayList<Groups>? = null
    private var mContext:Context

    fun setList(list: ArrayList<Groups>?) {
        this.list = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.each_group_admin_layout, parent, false)
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
        private val group_name_tv: TextView
        private val group_icon_iv: ImageView
        fun bindData(position: Int) {
           var groups: Groups=list?.get(position)!!
            when (position) {
                0 -> {
                    group_icon_iv.setImageDrawable(
                        ContextCompat.getDrawable(
                            mContext,
                            R.drawable.group1
                        )
                    )

                    group_name_tv.text = groups.name

                }
                1 -> {
                    group_icon_iv.setImageDrawable(
                        ContextCompat.getDrawable(
                            mContext,
                            R.drawable.group2
                        )
                    )

                    group_name_tv.text = groups.name
                    colorDrawable(group_icon_iv, ContextCompat.getColor(
                        mContext,R.color.g_color3))
                }
                2 -> {
                    group_icon_iv.setImageDrawable(
                        ContextCompat.getDrawable(
                            mContext,
                            R.drawable.group3
                        )
                    )

                    colorDrawable(group_icon_iv, ContextCompat.getColor(
                        mContext,R.color.g_color2))
                    group_name_tv.text = groups.name
                }
                3 -> {
                    group_icon_iv.setImageDrawable(
                        ContextCompat.getDrawable(
                            mContext,
                            R.drawable.group2
                        )
                    )

                    colorDrawable(group_icon_iv, ContextCompat.getColor(
                        mContext,R.color.g_color1))

                    group_name_tv.text = groups.name
                }
                4 -> {
                    group_icon_iv.setImageDrawable(
                        ContextCompat.getDrawable(
                            mContext,
                            R.drawable.group3
                        )
                    )

                    group_name_tv.text = groups.name
                }
                else -> { // Note the block

                    group_icon_iv.setImageDrawable(
                        ContextCompat.getDrawable(
                            mContext,
                            R.drawable.group3
                        )
                    )
                    group_name_tv.text = groups.name
                }
            }

            cardview.setOnClickListener { view: View? ->
                interaction?.onItemClicked()
            }
        }

        init {
            cardview = itemView.findViewById(R.id.card_id)
            group_name_tv = itemView.findViewById(R.id.group_name_tv)
            group_icon_iv = itemView.findViewById(R.id.group_icon_iv)
        }
    }

    interface GroupsCardInteraction {
        fun onItemClicked()
    }
}
