package com.orbis.orbis.ui.groupsModule.views

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.orbis.orbis.R
import com.orbis.orbis.databinding.FragmentClientPurchaseIdsBinding
import com.orbis.orbis.databinding.FragmentClientPurchaseIdsBindingImpl
import com.orbis.orbis.databinding.FragmentSubscriptionImageBinding
import com.orbis.orbis.databinding.RowPurchaseIdBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ClientPurchaseIDsDialogFragment : BottomSheetDialogFragment()
{
    //region variables
    lateinit var binding: FragmentClientPurchaseIdsBinding
    lateinit var itemList: ArrayList<String>
    private lateinit var listViewManager: LinearLayoutManager
    private lateinit var listRecycleAdapter: ListAdapter
    //endregion

    //region lifeCycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemList = arguments?.getStringArrayList("ids")!!
    }

    override fun onActivityCreated(savedInstanceState: Bundle?)
    {
        super.onActivityCreated(savedInstanceState)

        initViews()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_client_purchase_ids, container, false)

        return binding.root
    }

    companion object {
        fun newInstance(ids: ArrayList<String>): ClientPurchaseIDsDialogFragment {
            val args = Bundle()
            args.putStringArrayList("ids", ids)
            val fragment = ClientPurchaseIDsDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
    //endregion

    //region init
    private fun initViews()
    {
        binding.toolbar.titleTv.text = getString(R.string.code_list)

        setOnClicked()
        startInitiation()
    }

    private fun setOnClicked()
    {
        binding.toolbar.backArrowIv.setOnClickListener { dismiss() }
    }
    //endregion

    //region recycler view
    private fun startInitiation()
    {
        listViewManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        listRecycleAdapter = ListAdapter(context = requireContext())

        binding.categoryRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = listViewManager
            adapter = listRecycleAdapter
        }
    }

    inner class ListAdapter(val context: Context) : RecyclerView.Adapter<ListAdapter.ViewHolder>()
    {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
        {

            val binding = RowPurchaseIdBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) { holder.bindItems(itemList[position]) }

        override fun getItemCount(): Int { return itemList.size }

        // populate each cell with the data
        inner class ViewHolder(val rowBinding: RowPurchaseIdBinding) : RecyclerView.ViewHolder(rowBinding.root)
        {
            fun bindItems(row: String)
            {
                rowBinding.textView6.text = row
            }
        }
    }
    //endregion
}