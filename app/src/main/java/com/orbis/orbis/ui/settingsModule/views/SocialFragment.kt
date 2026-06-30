package com.orbis.orbis.ui.settingsModule.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseFragment
import com.orbis.orbis.databinding.FragmentSocialBinding
import com.orbis.orbis.models.auth.UserInfo
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.ui.ProfileModule.views.ProfileViewModel
import com.orbis.orbis.ui.groupsModule.adapter.GroupListAdapter
import com.orbis.orbis.ui.groupsModule.views.EditGroupActivity
import com.orbis.orbis.models.subscriptions.Subscription
import com.orbis.orbis.ui.groupsModule.views.ClientPurchaseIDsDialogFragment
import com.orbis.orbis.ui.subscriptionsModule.list.DeleteSubscriptionDialogFragment
import com.orbis.orbis.ui.subscriptionsModule.mySubscriptions.MySubscriptionsAdapter
import com.orbis.orbis.utils.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SocialFragment(private var userInfo: UserInfo) : BaseFragment(),
    GroupListAdapter.GroupsCardInteraction {

    private var groupArrowOpen = false
    private var subscriptionsArrowOpen = false
    lateinit var binding: FragmentSocialBinding
    lateinit var profileViewModel: ProfileViewModel
    val groups: ArrayList<GroupDetails> = ArrayList()
    lateinit var adapter: GroupListAdapter
    private lateinit var subscriptionsAdapter: MySubscriptionsAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_social, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        binding.manageMyGroupsAdminRl.setOnClickListener {
            if (!groupArrowOpen) {
                binding.groupsDropDownIv.setImageResource(R.drawable.ic_drop_up)
                binding.groupAdminRv.visibility = View.VISIBLE
            } else {
                binding.groupsDropDownIv.setImageResource(R.drawable.ic_drop_down)
                binding.groupAdminRv.visibility = View.GONE
            }
            groupArrowOpen = !groupArrowOpen
        }
        binding.groupAdminRv.layoutManager = LinearLayoutManager(requireContext())
        adapter = GroupListAdapter(groups, this, requireContext())
        binding.groupAdminRv.adapter = adapter
        binding.accountPrivacyIv.isChecked = userInfo.accountPrivate
        binding.accountPrivacyIv.setOnCheckedChangeListener { buttonView, isChecked ->
            userInfo.accountPrivate = isChecked
            profileViewModel.updateMyProfile(userInfo)
        }

        binding.mySubscriptionsRl.setOnClickListener {
            if (!subscriptionsArrowOpen) {
                binding.mySubscriptionsDropDownIv.setImageResource(R.drawable.ic_drop_up)
                binding.subscriptionsRv.visibility = View.VISIBLE
            } else {
                binding.mySubscriptionsDropDownIv.setImageResource(R.drawable.ic_drop_down)
                binding.subscriptionsRv.visibility = View.GONE
            }
            subscriptionsArrowOpen = !subscriptionsArrowOpen
        }
        subscriptionsAdapter = MySubscriptionsAdapter(::onCancelSubscriptionClick, ::onCodeListClick, requireContext())
        binding.subscriptionsRv.adapter = subscriptionsAdapter
        profileViewModel.getMySubscriptions()
        profileViewModel.getMyPurchases()
        setupObservers()
    }

    private fun setupObservers() {
        profileViewModel.groupList.observe(viewLifecycleOwner) {
            groups.addAll(it)
            adapter.notifyDataSetChanged()
        }
        profileViewModel.myProfile.observe(viewLifecycleOwner) {
            userInfo = it
            binding.accountPrivacyIv.isChecked = userInfo.accountPrivate
        }
        profileViewModel.mySubscriptions.observe(viewLifecycleOwner) {
            subscriptionsAdapter.addList(it)
        }
        profileViewModel.cancelSubscription.observe(viewLifecycleOwner) {
            subscriptionsAdapter.remove(it)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.placesYouFollowRl.setOnClickListener {
            showSelectPlaceSheet()
        }



        hideKeyboard(requireActivity())
    }


    companion object {
        fun getInstance(userInfo: UserInfo): SocialFragment {
            return SocialFragment(userInfo)
        }
    }

    private fun showSelectPlaceSheet() {
        val selectPlaceDialogFragment = SelectPlaceDialogFragment()
        selectPlaceDialogFragment.show(
            childFragmentManager,
            SelectPlaceDialogFragment::class.java.getSimpleName()
        )
    }

    override fun onItemClicked(position: Int) {
        if (groups.isNotEmpty()) {
            val intent = Intent(requireContext(), EditGroupActivity::class.java)
            intent.putExtra("group", groups[position])
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        groups.clear()
        profileViewModel.getMyAdminGroups(0)
    }

    private fun onCancelSubscriptionClick(position: Int, item: Subscription) {
        val deleteSubscriptionDialog =
            DeleteSubscriptionDialogFragment.newInstance(getString(R.string.unsubscribe_subscription_confirmation))
        deleteSubscriptionDialog.onDeleteClick = {
            profileViewModel.unsubscribeSubscription(position, item.subscriptionKey)
        }
        deleteSubscriptionDialog.show(childFragmentManager, "DeleteSubscriptionDialogFragment")
    }

    private fun onCodeListClick(codeList: ArrayList<String>)
    {
        try
        {
            val clientPurchaseIDsDialogFragment = ClientPurchaseIDsDialogFragment.newInstance(codeList)
            clientPurchaseIDsDialogFragment.show(requireActivity().supportFragmentManager, ClientPurchaseIDsDialogFragment::class.java.simpleName)
        }
        catch (e: Exception) { e.printStackTrace() }
    }
}