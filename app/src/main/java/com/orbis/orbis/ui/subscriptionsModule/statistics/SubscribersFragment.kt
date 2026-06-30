package com.orbis.orbis.ui.subscriptionsModule.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseFragment
import com.orbis.orbis.databinding.FragmentSubscriptionSubscribersBinding
import com.orbis.orbis.models.BigDataSharConstants
import com.orbis.orbis.models.user.User
import com.orbis.orbis.ui.groupsModule.adapter.MembersAdapter
import com.orbis.orbis.ui.groupsModule.views.ClientPurchaseIDsDialogFragment
import com.orbis.orbis.utils.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SubscribersFragment : BaseFragment(), MembersAdapter.MemberInteraction {

    companion object {
        fun newInstance(groupKey: String): SubscribersFragment {
            val args = Bundle()
            args.putString("groupKey", groupKey)
            val fragment = SubscribersFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var binding: FragmentSubscriptionSubscribersBinding
    private lateinit var viewModel: SubscribersViewModel
    private lateinit var membersAdapter: MembersAdapter
    private val members: ArrayList<User> = ArrayList()
    private val membersBackup: ArrayList<User> = ArrayList()
    private var groupKey = ""
    private var currentPage = 0
    private var showingSearchResult = false
    private var screenResumed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupKey = arguments?.getString("groupKey") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_subscription_subscribers,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean)
    {
        super.setUserVisibleHint(isVisibleToUser)
    }
    override fun onResume()
    {
        super.onResume()

        screenResumed = true

        currentPage = 0

        members.clear()
        membersBackup.clear()
        membersAdapter.notifyDataSetChanged()

        getData()
    }

    private fun getData()
    {
        if(BigDataSharConstants.isOneTimePurchase)
        {
            viewModel.getPurchases(groupKey, BigDataSharConstants.subscriptionKey, currentPage)
        }
        else
        {
            viewModel.getSubscribers(groupKey, BigDataSharConstants.subscriptionKey, currentPage)
        }
    }

    private fun initView() {
        setupViewModel()
        setupMembersList()
        setupObservers()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[SubscribersViewModel::class.java]
    }

    private fun setupMembersList() {
        membersAdapter = MembersAdapter(requireContext(), members, arrayListOf(), true, this)
        binding.rvSubscribers.adapter = membersAdapter
        binding.rvSubscribers.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
                val total = layoutManager!!.itemCount
                val currentLastItem = layoutManager!!.findLastVisibleItemPosition()
                if (currentLastItem == total - 1 && !showingSearchResult && !screenResumed)
                {
                    getData()
                }
            }
        })
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty()) {
                    members.clear()
                    showingSearchResult = false
                    members.addAll(membersBackup)

                    membersAdapter.notifyDataSetChanged()
                    hideKeyboard(requireActivity())
                }
                return false
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                showingSearchResult = true
                members.clear()
                for (member in membersBackup) {

                    if (member.displayName.lowercase().contains(query.lowercase())) {
                        members.add(member)
                    }
                }
                membersAdapter.notifyDataSetChanged()
                hideKeyboard(requireActivity())
                return false
            }

        })
    }

    private fun setupObservers() {
        viewModel.membersLiveData.observe(viewLifecycleOwner) {
            screenResumed = false
            currentPage++
            val index = members.size
            members.addAll(it)
            membersBackup.addAll(it)
            membersAdapter.notifyItemRangeInserted(index, members.size)
        }
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.loading = it
        }
        viewModel.error.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBlock(position: Int) {

    }

    override fun addAdmin(position: Int) {

    }

    override fun adminRemoveAdmin(position: Int) {

    }

    override fun onClick(position: Int) {

    }

    override fun onCodeListClick(codeList: ArrayList<String>)
    {
        try
        {
            val clientPurchaseIDsDialogFragment = ClientPurchaseIDsDialogFragment.newInstance(codeList)
            clientPurchaseIDsDialogFragment.show(requireActivity().supportFragmentManager, ClientPurchaseIDsDialogFragment::class.java.simpleName)
        }
        catch (e: Exception) { e.printStackTrace() }
    }
}