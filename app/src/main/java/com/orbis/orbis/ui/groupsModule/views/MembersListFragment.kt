package com.orbis.orbis.ui.groupsModule.views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.databinding.FragmentMembersListBinding
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.user.User
import com.orbis.orbis.ui.ProfileModule.views.ProfileActivity
import com.orbis.orbis.ui.groupsModule.adapter.MembersAdapter
import com.orbis.orbis.ui.groupsModule.viewModel.GroupViewModel
import com.orbis.orbis.ui.subscriptionsModule.list.SubscriptionImageDialogFragment
import com.orbis.orbis.utils.Utils
import com.orbis.orbis.utils.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MembersListFragment : AppCompatActivity(), MembersAdapter.MemberInteraction {
    lateinit var binding: FragmentMembersListBinding
    lateinit var viewModel: GroupViewModel
    var currentPage = 0
    val members: ArrayList<User> = ArrayList()
    val membersBackup: ArrayList<User> = ArrayList()
    val admins: ArrayList<User> = ArrayList()
    var groupKey = ""
    lateinit var membersAdapter: MembersAdapter
    private var showingSearchResult = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout for this fragment
        binding = DataBindingUtil.setContentView(this, R.layout.fragment_members_list)
        initView()
    }

    private fun initView() {
        viewModel = ViewModelProvider(this).get(GroupViewModel::class.java)
        val title = binding.toolbar.titleTv
        val backArrow = binding.toolbar.backArrowIv
        title.text = getString(R.string.members)
        backArrow.setOnClickListener {
            finish()
        }
        groupKey = intent.getStringExtra("groupKey")!!
        val isAdmin = intent.getBooleanExtra("isAdmin", false)
        membersAdapter = MembersAdapter(this, members, admins, isAdmin, this)
        binding.membersRv.layoutManager = LinearLayoutManager(this)
        binding.membersRv.adapter = membersAdapter
        setupObservers()
        viewModel.getGroupMembers(groupKey, currentPage)
        viewModel.getGroupAdmins(groupKey)
        binding.membersRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
                val total = layoutManager!!.itemCount
                val currentLastItem = layoutManager!!.findLastVisibleItemPosition()
                Log.d("paginationCalling", total.toString() + " " + currentLastItem)
                if (currentLastItem == total - 1 && !showingSearchResult) {
                    viewModel.getGroupMembers(groupKey, currentPage)
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
                    hideKeyboard(this@MembersListFragment)
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
                hideKeyboard(this@MembersListFragment)
                return false
            }

        })

    }

    private fun setupObservers() {
        viewModel.groupMembers.observe(this) {
            currentPage++
            val index = members.size
            members.addAll(it)
            membersBackup.addAll(it)
            membersAdapter.notifyItemRangeInserted(index, members.size)
        }
        viewModel.isLoading.observe(this) {
            binding.loading = it
        }
        viewModel.groupAdmins.observe(this) {
            admins.addAll(it)
            membersAdapter.notifyDataSetChanged()
        }
        viewModel.addAdmin.observe(this) {
            if (it) {
                admins.add(members[pendingAddAdmin])
                membersAdapter.notifyItemChanged(pendingAddAdmin)
            }
        }
        viewModel.addBan.observe(this) {
            if (it) {
                members.removeAt(pendingBlock)
                membersAdapter.notifyItemRemoved(pendingBlock)
            }
        }
        viewModel.adminRemoveAdmin.observe(this) {
            if (it) {
                val userKey = members[adminRemoveAdmin].userKey
                for (i in 0 until admins.size) {
                    if (admins[i].userKey == userKey) {
                        admins.removeAt(i)
                        break
                    }
                }
                membersAdapter.notifyItemChanged(adminRemoveAdmin)
            }
        }
        viewModel.error.observe(this) {
            if (!it.isNullOrEmpty()) {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    var pendingBlock = -1
    var pendingAddAdmin = -1
    var adminRemoveAdmin = -1

    override fun onBlock(position: Int) {
        pendingBlock = position
        viewModel.addBan(groupKey, members[position].userKey)
    }

    override fun addAdmin(position: Int) {
        pendingAddAdmin = position
        viewModel.addAdmin(groupKey, members[position].userKey)
    }

    override fun adminRemoveAdmin(position: Int) {
        adminRemoveAdmin = position
        viewModel.adminRemoveAdmin(groupKey, members[position].userKey)
    }

    override fun onClick(position: Int) {
        val intent = Intent(this, ProfileActivity::class.java)
        intent.putExtra(
            "displayName",
            members[position].displayName
        )
        intent.putExtra("showMyProfile", false)
        intent.putExtra("userKey", members[position].userKey)
        startActivity(intent)
    }

    override fun onCodeListClick(codeList: ArrayList<String>)
    {
        try
        {
            val clientPurchaseIDsDialogFragment = ClientPurchaseIDsDialogFragment.newInstance(codeList)
            clientPurchaseIDsDialogFragment.show(supportFragmentManager, ClientPurchaseIDsDialogFragment::class.java.simpleName)
        }
        catch (e: Exception) { e.printStackTrace() }
    }


}