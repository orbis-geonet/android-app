package com.orbis.orbis.ui.messageModule.adapter


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.R
import com.orbis.orbis.ui.messageModule.viewModel.Message
import java.util.*


class MessageSearchListAdapter(private val interaction: MessageSearchInteraction?, context:Context) :
    RecyclerView.Adapter<MessageSearchListAdapter.ViewHolder>() {
    private var list: ArrayList<Message>? = null
    private var mContext:Context

    fun setList(list: ArrayList<Message>?) {
        this.list = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.each_search_receiver_layout, parent, false)
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
        private val user_name_tv: TextView
        private val user_icon_iv: ImageView
        private val send_iv: ImageView
        fun bindData(position: Int) {
           var messages: Message=list?.get(position)!!
            user_icon_iv.setImageResource(messages.image)
            user_name_tv.text = messages.name
            cardview.setOnClickListener { view: View? ->
                interaction?.onItemClicked()
            }
        }

        init {
            cardview = itemView.findViewById(R.id.card_id)
            user_name_tv = itemView.findViewById(R.id.user_name_tv)
            user_icon_iv = itemView.findViewById(R.id.user_icon_iv)
            send_iv = itemView.findViewById(R.id.send_iv)
        }
    }

    interface MessageSearchInteraction {
        fun onItemClicked()
    }
}
