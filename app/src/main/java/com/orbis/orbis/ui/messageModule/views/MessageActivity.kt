package com.orbis.orbis.ui.messageModule.views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseActivity
import com.orbis.orbis.databinding.ActivityMessageBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.auth.UserInfo
import com.orbis.orbis.models.message.ConversationModel
import com.orbis.orbis.models.message.MessageModel
import com.orbis.orbis.ui.ProfileModule.adapter.FollowersFollowingAdapter
import com.orbis.orbis.ui.ProfileModule.views.ProfileActivity
import com.orbis.orbis.ui.ProfileModule.views.ProfileViewModel
import com.orbis.orbis.ui.homeModule.views.MapActivity
import com.orbis.orbis.ui.messageModule.adapter.ConversationListAdapter
import com.orbis.orbis.utils.Utils
import com.orbis.orbis.utils.ViewUtils
import com.orbis.orbis.utils.hideKeyboard
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@AndroidEntryPoint
class MessageActivity : BaseActivity() {
    lateinit var binding: ActivityMessageBinding
    val conversations: ArrayList<ConversationModel> = ArrayList()
    val conversationsTemp: ArrayList<ConversationModel> = ArrayList()
    lateinit var conversationAdapter: ConversationListAdapter
    lateinit var profileViewModel: ProfileViewModel
    lateinit var myProfile: UserInfo
    val users: HashMap<String, UserInfo> = HashMap()
    lateinit var query: Query
    var listenerAdded = false
    val searchUser: ArrayList<UserInfo> = ArrayList()
    lateinit var usersAdapter: FollowersFollowingAdapter
    var searchTerm = ""
    var currentPage = 0
    var nextPage = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_message)
        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        profileViewModel.getMyProfile()
        val db = Firebase.firestore
        query = db.collection("conversation")
            .whereArrayContains("participants", PrefManager(this).getUserKey()!!)
        binding.messagesRv.layoutManager = LinearLayoutManager(this)
        conversationAdapter = ConversationListAdapter(this, conversations)
        binding.messagesRv.adapter = conversationAdapter
        binding.toolbar.titleTv.text = getString(R.string.messages)
        binding.toolbar.backArrowIv.setOnClickListener {
            finish()
        }
        setupObservers()
        usersAdapter = FollowersFollowingAdapter(this, searchUser, null, false, showMessage = true)
        binding.usersRv.layoutManager = LinearLayoutManager(this)
        binding.usersRv.adapter = usersAdapter
        binding.myProfilePic.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("userKey", PrefManager(this).getUserKey())
            startActivity(intent)
        }
        binding.usersRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
                val total = layoutManager!!.itemCount
                val currentLastItem = layoutManager!!.findLastVisibleItemPosition()
                Log.d("paginationCalling", total.toString() + " " + currentLastItem)
                if (currentLastItem == total - 10 && searchTerm.isNotEmpty() && currentPage != nextPage) {
                    profileViewModel.searchUser(searchTerm, nextPage)
                    currentPage = nextPage
                }
            }
        })
        binding.messageSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    searchTerm = query
                    searchUser.clear()
                    binding.messagesRv.visibility = View.GONE
                    binding.usersRv.visibility = View.VISIBLE
                    binding.noItem.visibility = View.GONE
                    currentPage = 0
                    nextPage = 0
                    profileViewModel.searchUser(query, 0)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    binding.messagesRv.visibility = View.VISIBLE
                    binding.usersRv.visibility = View.GONE
                    if (conversationsTemp.size == 0) {
                        binding.noItem.visibility = View.VISIBLE
                    } else {
                        binding.noItem.visibility = View.GONE
                    }
                    hideKeyboard(this@MessageActivity)
                }
                return false
            }

        })
    }

    private fun contain(conversationModel: ConversationModel): Int {
        for (i in 0 until conversations.size) {
            if (conversations[i].lastMessage?.conversationId!! == conversationModel.lastMessage?.conversationId) {
                return i
            }
        }
        return -1
    }

    private fun sortList() {
        Collections.sort(conversations, object : Comparator<ConversationModel> {
            override fun compare(o1: ConversationModel?, o2: ConversationModel?): Int {
                val calander1 = Calendar.getInstance()
                val calander2 = Calendar.getInstance()
                calander1.timeInMillis = o1?.lastMessage?.timestamp!!
                calander2.timeInMillis = o2?.lastMessage?.timestamp!!
                if (calander1.before(calander2)) {
                    return 1
                } else {
                    return -1
                }
            }

        })
        conversationAdapter.notifyDataSetChanged()
    }

    private fun setupObservers() {
        profileViewModel.conversationModelData.observe(this) {
            if (contain(it) == -1) {
                conversations.add(it)
            }
            users[it.sender?.userKey!!] = it.sender!!
            if (conversations.size == conversationsTemp.size) {
                sortList()

                    if (!listenerAdded) {
                        listenerAdded = true
                        query.addSnapshotListener { value, error ->
                            for (document in value?.documents!!) {

                                val data = document.toObject(ConversationModel::class.java)

                                if (data?.lastMessage != null) {
                                    val index = contain(data!!)
                                    if (index == -1) {
                                        data.timestamp = System.currentTimeMillis()
                                        conversationsTemp.add(0, data)
                                        getUserProfileRecurse(0)
                                    } else {
                                        data.sender = conversations[index].sender
                                        data.timestamp = System.currentTimeMillis()
                                        conversations[index] = data
                                        sortList()
                                    }

                                }
                            }
                        }
                    }

                }  else {
                    getUserProfileRecurse(conversations.size)
                }

                Log.d("conversationGetCheckFinal", conversations.size.toString())
        }
        profileViewModel.isLoading.observe(this) {
            binding.loading = it
        }
        profileViewModel.myProfile.observe(this) {
            myProfile = it
            downloadMyImage()
        }
        profileViewModel.userList.observe(this) {
            searchUser.clear()
            searchUser.addAll(it)
            usersAdapter.notifyDataSetChanged()
            nextPage = currentPage + 1
        }
    }

    private fun getUserProfileRecurse(i: Int) {
        if (i < conversationsTemp.size) {
            var userKey = ""
            if (conversationsTemp[i].participants[0] == PrefManager(this).getUserKey()!!) {
                userKey = conversationsTemp[i].participants[1]
            } else {
                userKey = conversationsTemp[i].participants[0]
            }
            if (users[userKey] == null) {
                profileViewModel.getUserProfileMessage(conversationsTemp[i])
            } else {
                conversationsTemp[i].sender = users[userKey]
                profileViewModel.conversationModelData.postValue(conversationsTemp[i])
            }
        }
    }

    private fun findUserIndex(userInfo: UserInfo): Int {
        for (i in 0 until conversations.size) {
            if (conversations[i].sender?.userKey!! == userInfo.userKey) {
                return i
            }
        }
        return -1
    }

    private fun downloadMyImage() {
        ViewUtils.loadUserProfilePic(
            this,
            binding.myProfilePic,
            myProfile.imageName,
            myProfile.providerImageUrl
        )
    }


    override fun onResume() {
        super.onResume()
        val db = Firebase.firestore
        conversations.clear()
        conversationsTemp.clear()
        val query = db.collection("conversation")
            .whereArrayContains("participants", PrefManager(this).getUserKey()!!)

        Log.d("refreshingList", "conversationList")
        query.get().addOnSuccessListener {
            Log.d("conversationGetCheck", it.documents.size.toString())
            for (document in it.documents) {
                Log.d("docCheck", "foundData")
                val data = document.toObject(ConversationModel::class.java)
                if (data != null) {
                    if (data.lastMessage != null) {
                        data.lastMessage?.senderId
                        conversationsTemp.add(data!!)
                    }
                }
            }
            if (conversationsTemp.size == 0) {
                binding.noItem.visibility = View.VISIBLE
            } else {
                binding.noItem.visibility = View.GONE
            }

            Log.d("conversationGetCheckAdd", conversationsTemp.size.toString())
            getUserProfileRecurse(0)

        }

    }
}