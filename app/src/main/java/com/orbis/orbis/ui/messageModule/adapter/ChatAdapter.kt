package com.orbis.orbis.ui.messageModule.adapter


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.R
import com.orbis.orbis.ui.messageModule.viewModel.Chat
import java.util.*


class ChatAdapter(private val interaction: messageBubbleInteraction?, context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    private var list: ArrayList<Chat>? = null
    private var mContext: Context
    private val LEFT_TEXT_MESSAGE = 1
    private val RIGHT_TEXT_MESSAGE = 2
    private val DATE_TEXT_MESSAGE = 3
    fun setList(list: ArrayList<Chat>?) {
        this.list = list
    }

    override fun getItemViewType(position: Int): Int {
        // Just as an example, return depending on position
        // Note that unlike in ListView adapters, types don't have to be contiguous
        return  getItemViewTypes(position)
    }

    private fun getItemViewTypes(position: Int): Int {
        val baseMessage: Chat = list?.get(position)!!
        return when (baseMessage.type) {
            1 -> LEFT_TEXT_MESSAGE
            2 -> RIGHT_TEXT_MESSAGE
            3 -> DATE_TEXT_MESSAGE
            else -> RIGHT_TEXT_MESSAGE
        }
    }
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View
        when (viewType) {
            LEFT_TEXT_MESSAGE -> {
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.left_message_item, parent, false)
                view.tag = LEFT_TEXT_MESSAGE
                return ViewHolderLeftBubble(
                    view
                )
            }
            RIGHT_TEXT_MESSAGE -> {
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.right_message_item, parent, false)
                view.tag = RIGHT_TEXT_MESSAGE
                return ViewHolderRightBubble(
                    view
                )
            }
            DATE_TEXT_MESSAGE -> {
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.message_list_header, parent, false)
                view.tag = RIGHT_TEXT_MESSAGE
                return DateItemHolder(
                    view
                )
            }
            else -> {
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.right_message_item, parent, false)
                view.tag=-1
                return ViewHolderRightBubble(
                    view
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.getItemViewType()) {
            1 -> {
                val viewHolder = holder as ViewHolderLeftBubble
                viewHolder.bindData(position)
            }
            2 -> {
                val viewHolder = holder as ViewHolderRightBubble
                viewHolder.bindData(position)
            }
            3 -> {
                val viewHolder = holder as DateItemHolder
                viewHolder.bindData(position)
            }
        }

    }


    override fun getItemCount(): Int {
        return list?.size!!
    }

    init {
        mContext = context
    }

    inner class ViewHolderRightBubble(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txt_message: TextView
        private val txt_time: TextView
        fun bindData(position: Int) {
            var messsge:Chat=list?.get(position)!!
            txt_message.text=messsge.text
            txt_time.text=messsge.time
//            cardview.setOnClickListener { view: View? ->
//                interaction?.onItemClicked()
//            }
        }

        init {
            txt_message = itemView.findViewById(R.id.txt_message)
            txt_time = itemView.findViewById(R.id.txt_time)
        }
    }

    inner class ViewHolderLeftBubble(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txt_message: TextView
        private val txt_time: TextView
        fun bindData(position: Int) {
            var messsge:Chat=list?.get(position)!!
            txt_message.text=messsge.text
            txt_time.text=messsge.time
//            cardview.setOnClickListener { view: View? ->
//                interaction?.onItemClicked()
//            }
        }

        init {
            txt_message = itemView.findViewById(R.id.txt_message)
            txt_time = itemView.findViewById(R.id.txt_time)
        }

    }
    inner class DateItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var txtMessageDate: TextView
        fun bindData(position: Int) {
            var messsge:Chat=list?.get(position)!!
            txtMessageDate.text=messsge.time
        }
        init {
            txtMessageDate = itemView.findViewById(R.id.txt_message_date)
        }
    }

    interface messageBubbleInteraction {
        fun onItemClicked()
    }


    interface StickyHeaderAdapter<T : RecyclerView.ViewHolder?> {
        fun getHeaderId(var1: Int): Long
        fun onCreateHeaderViewHolder(var1: ViewGroup?): T
        fun onBindHeaderViewHolder(var1: T, var2: Int, var3: Long)
    }
}
