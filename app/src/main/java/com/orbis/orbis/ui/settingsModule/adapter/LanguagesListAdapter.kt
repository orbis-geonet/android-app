package com.orbis.orbis.ui.settingsModule.adapter


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.R
import com.orbis.orbis.ui.settingsModule.viewModel.Language
import java.util.*


class LanguagesListAdapter(private val interaction: LanguageCardInteraction?, context: Context) :
    RecyclerView.Adapter<LanguagesListAdapter.ViewHolder>() {
    private var list: ArrayList<Language>? = null
    private var mContext:Context
    var selectedLang = -1
    fun setList(list: ArrayList<Language>?) {
        this.list = list
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.each_language_layout, parent, false)
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
        private val radio_btn: RadioButton
        private val language_name_tv: TextView
        fun bindData(position: Int) {

            language_name_tv.text=list?.get(position)?.name

            radio_btn.isChecked = selectedLang == position

            cardview.setOnClickListener { view: View? ->
                interaction?.onItemClicked(adapterPosition)
                selectedLang = adapterPosition
                notifyDataSetChanged()
            }
            radio_btn.setOnClickListener { view: View? ->
                interaction?.onItemClicked(adapterPosition)
                selectedLang = adapterPosition
                notifyDataSetChanged()
            }
        }

        init {
            cardview = itemView.findViewById(R.id.card_id)
            radio_btn = itemView.findViewById(R.id.radio_btn)
            language_name_tv = itemView.findViewById(R.id.language_name_tv)
        }
    }

    interface LanguageCardInteraction {
        fun onItemClicked(position: Int)
    }

    fun setSelectedLanguage(selectedLang: Int) {
        this.selectedLang = selectedLang
        notifyDataSetChanged()
    }
}
