package com.orbis.orbis.ui.messageModule.views

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.databinding.ActivityChatBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.helpers.TimeAgo
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.auth.UserInfo
import com.orbis.orbis.models.message.ConversationModel
import com.orbis.orbis.models.message.MessageModel
import com.orbis.orbis.ui.ProfileModule.views.ProfileActivity
import com.orbis.orbis.ui.ProfileModule.views.ProfileViewModel
import com.orbis.orbis.ui.messageModule.adapter.ChatMessageAdapter
import com.orbis.orbis.utils.PermissionUtil
import com.orbis.orbis.utils.ProgressDialog
import com.orbis.orbis.utils.Utils
import com.orbis.orbis.utils.ViewUtils
import com.orbis.orbis.utils.picker.Picker
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import org.ocpsoft.prettytime.PrettyTime
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


@AndroidEntryPoint
class ChatActivity : AppCompatActivity(), PickerBottomSheet.PickerInteraction {

    private lateinit var progressDialog: ProgressDialog

    private val picker = Picker()
    lateinit var binding: ActivityChatBinding
    var conversationModel: ConversationModel? = null
    var user: UserInfo? = null
    val chats: ArrayList<MessageModel> = ArrayList()
    lateinit var chatMessageAdapter: ChatMessageAdapter
    lateinit var profileViewModel: ProfileViewModel
    lateinit var pickerBottomSheet: PickerBottomSheet
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat)
        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        conversationModel = intent.getParcelableExtra("data")
        if (conversationModel == null) {
            user = intent.getParcelableExtra("user")
            if (user != null) {
                loadNewConversation()
            } else {
                val userKey = intent.getStringExtra("userKey")
                profileViewModel.getUserProfile(userKey!!)
            }
        }
        profileViewModel.myProfile.observe(this) {
            user = it
            loadNewConversation()
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.navigationBarColor = Color.TRANSPARENT

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())

            val bottomInset = maxOf(navInsets.bottom, imeInsets.bottom)
            view.setPadding(0, 0, 0, bottomInset)

            val topOffset = statusBarInsets.top + resources.getDimensionPixelSize(R.dimen.margin_top_8dp)

            (binding.backArrowIv.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)?.let { params ->
                params.topMargin = topOffset
                binding.backArrowIv.layoutParams = params
            }

            (binding.userIconIv.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)?.let { params ->
                params.topMargin = topOffset
                binding.userIconIv.layoutParams = params
            }

            insets
        }
        initView()
    }

    private fun loadNewConversation() {
        binding.user = user
        ViewUtils.loadUserProfilePic(
            this,
            binding.userIconIv,
            user?.imageName,
            user?.providerImageUrl
        )
        val db = Firebase.firestore


        db.collection("conversation")
            .whereArrayContains("participants", user?.userKey!!)
            .get()
            .addOnSuccessListener {
                Log.d("docCheckOutOfLoop", it.documents.size.toString())
                for (document in it.documents) {
                    Log.d("docCheck", "foundData")
                    val data = document.toObject(ConversationModel::class.java)
                    if (data?.participants!![0] == PrefManager(this@ChatActivity).getUserKey()!! || data.participants[1] == PrefManager(
                            this@ChatActivity
                        ).getUserKey()!!
                    ) {
                        conversationModel = data
                        conversationModel?.sender = user!!
                        if (conversationModel?.lastMessage != null)
                            initConversation()
                    }
                }
            }.addOnFailureListener {
                Log.e("messageGetFailed", it.message!!)
            }
    }

    private fun contain(messageModel: MessageModel): Boolean {
        for (chat in chats) {
            if (chat.timestamp == messageModel.timestamp) {
                return true
            }
        }
        return false
    }

    private fun initView()
    {
        progressDialog = ProgressDialog(this)
        picker.populate(this)

        pickerBottomSheet = PickerBottomSheet(this)
        initConversation()
        binding.backArrowIv.setOnClickListener {
            finish()
        }
        profileViewModel.conversationModelData.observe(this) {
            conversationModel = it
            initConversation()
        }
        binding.userIconIv.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("userKey", conversationModel?.sender?.userKey)
            intent.putExtra("displayName", conversationModel?.sender?.displayName)
            startActivity(intent)
        }
        binding.titleTv.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("userKey", conversationModel?.sender?.userKey)
            intent.putExtra("displayName", conversationModel?.sender?.displayName)
            startActivity(intent)
        }
        binding.onlineTv.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("userKey", conversationModel?.sender?.userKey)
            intent.putExtra("displayName", conversationModel?.sender?.displayName)
            startActivity(intent)
        }

        binding.addIv.setOnClickListener {
            pickerBottomSheet.show(supportFragmentManager, "")
            //checkPermission()
        }
        profileViewModel.isLoading.observe(this) {
            binding.loading = it
            if (!it) {
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.uploadLoader.visibility = View.GONE
                }, 200)
            }
        }
        binding.sendIv.setOnClickListener {
            if (binding.messageText.text.isNotEmpty()) {
                sendTextMessage(binding.messageText.text.toString())
            }
        }

    }

    private fun initConversation() {
        if (conversationModel != null && conversationModel?.lastMessage != null) {
            binding.user = conversationModel?.sender!!

            binding.onlineTv.text = conversationModel?.lastMessage?.timestamp?.let {
                TimeAgo.DateDifference(it, this)
            } ?: getString(R.string.tap_to_learn_more)

            if (conversationModel?.lastMessage?.senderId != PrefManager(this).getUserKey()!!) {
                binding.onlineTv.setText(
                    TimeAgo.DateDifference(
                        conversationModel?.lastMessage?.timestamp!!,
                        this
                    )
                )
            }


            chatMessageAdapter = ChatMessageAdapter(this, chats, PrefManager(this).getUserKey()!!)
            binding.chatRv.layoutManager = LinearLayoutManager(this)
            binding.chatRv.adapter = chatMessageAdapter
            val db = Firebase.firestore
            val query = db.collection("conversation")
                .document(conversationModel!!.lastMessage?.conversationId!!)
            conversationModel!!.lastMessage?.isRead = true
            query.update("lastMessage", conversationModel!!.lastMessage).addOnFailureListener {
                Log.e("seenUpdateError", it.message!!)
            }
            var userKey = conversationModel!!.participants[0]
            if (conversationModel!!.participants[0] == PrefManager(this).getUserKey()) {
                userKey = conversationModel!!.participants[1]
            }

            db.collection("userActivity").document(userKey)
                .addSnapshotListener { value, error ->
                    val lastSeen = value?.getLong("lastActive")
                    if (lastSeen != null) {
                        binding.onlineTv.setText(TimeAgo.DateDifference(lastSeen, this))
                        if (conversationModel != null) {
                            if (TimeAgo.difference(lastSeen) > TimeAgo.difference(conversationModel?.lastMessage?.timestamp!!) && conversationModel?.lastMessage?.senderId != PrefManager(
                                    this@ChatActivity
                                ).getUserKey()!!
                            ) {
                                Log.d("lastSeenPast", "got")
                                binding.onlineTv.setText(
                                    TimeAgo.DateDifference(
                                        conversationModel?.lastMessage?.timestamp!!,
                                        this
                                    )
                                )
                            }
                        }
                    }


                }
            val ref = db.collection("chatMessages")
                .whereEqualTo("conversationId", conversationModel!!.lastMessage?.conversationId!!)
                .orderBy("timestamp", Query.Direction.ASCENDING)
            ref.get().addOnSuccessListener {
                for (document in it.documents) {
                    val data = document.toObject(MessageModel::class.java)


                    if (!contain(data!!)) {
                        chats.add(data!!)
                    }

                    chatMessageAdapter.notifyDataSetChanged()
                    binding.chatRv.scrollToPosition(chats.size - 1)
                }
                for (i in 0 until chats.size) {
                    if (chats[i].type == "VIDEO") {
                        downloadChatVideo(i)
                    }
                }
            }
            ref.addSnapshotListener { value, error ->
                for (document in value?.documents!!) {
                    val data = document.toObject(MessageModel::class.java)
                    if (!contain(data!!)) {
                        chats.add(data)
                        if (data.type == "VIDEO") {
                            downloadChatVideo(chats.size - 1)
                        }
                    }
                    chatMessageAdapter.notifyDataSetChanged()
                    binding.chatRv.scrollToPosition(chats.size - 1)

                }
            }
            ViewUtils.loadUserProfilePic(
                this,
                binding.userIconIv,
                conversationModel?.sender?.imageName,
                conversationModel?.sender?.providerImageUrl
            )

        }
    }

    private fun sendTextMessage(toString: String) {
        binding.loading = true
        val db = Firebase.firestore


        if (conversationModel != null && conversationModel?.lastMessage != null) {
            val messageModel = MessageModel(
                conversationModel?.lastMessage?.conversationId!!,
                false,
                toString,
                "",
                PrefManager(this@ChatActivity).getUserKey()!!,
                Calendar.getInstance().timeInMillis,
                "TEXT"
            )
            val participant: ArrayList<String> = ArrayList()
            participant.add(PrefManager(this@ChatActivity).getUserKey()!!)
            participant.add(binding.user?.userKey!!)
            val conversationModel1 =
                ConversationModel(messageModel, participant, System.currentTimeMillis(), null)
            db.collection("conversation").document(conversationModel?.lastMessage?.conversationId!!)
                .set(conversationModel1)
                .addOnSuccessListener {
                    db.collection("chatMessages").add(
                        MessageModel(
                            conversationModel!!.lastMessage?.conversationId!!,
                            false,
                            toString,
                            "",
                            PrefManager(this).getUserKey()!!,
                            System.currentTimeMillis(),
                            "TEXT"
                        )
                    ).addOnSuccessListener {
                        binding.messageText.setText("")
                        binding.loading = false
                    }.addOnFailureListener {
                        binding.loading = false
                    }
                }.addOnFailureListener {
                    binding.loading = false
                }

        } else {
            val id = db.collection("conversation").document().id
            val messageModel = MessageModel(
                id,
                false,
                toString,
                "",
                PrefManager(this@ChatActivity).getUserKey()!!,
                System.currentTimeMillis(),
                "TEXT"
            )
            val participant: ArrayList<String> = ArrayList()
            participant.add(PrefManager(this@ChatActivity).getUserKey()!!)
            participant.add(binding.user?.userKey!!)
            val conversationModel1 =
                ConversationModel(messageModel, participant, System.currentTimeMillis(), null)
            db.collection("conversation").document(id).set(conversationModel1)
                .addOnSuccessListener {
                    db.collection("chatMessages").add(messageModel).addOnSuccessListener {
                        binding.loading = false
                        binding.messageText.setText("")
                        conversationModel = conversationModel1
                        conversationModel?.sender = binding.user!!
                        initConversation()

                    }
                }
            Log.d("checkNewChatId", id)
        }
    }

    private fun downloadChatVideo(i: Int) {
        val storage =
            Firebase.storage.getReference(Constants.CHAT_VIDEO + chats[i].mediaUrl)
        storage.downloadUrl.addOnSuccessListener { url ->
            chats[i].videoUrl = url.toString()
            chatMessageAdapter.notifyItemChanged(i)
            // Picasso.get().load(url).into(viewHolder.binding?.shareIconIv)
        }.addOnFailureListener {
            //add_iv.setImageDrawable(ContextCompat.getDrawable(context,ViewUtils.getPlaceIcon(check_in_list[position].place.type)!!))
        }

    }

    override fun chosenItem(position: Int) {
        pickerBottomSheet.dismiss()
        if (position == 1)
        {
            onClickedSelectPhoto()
        }
        else if (position == 2)
        {
            onClickedSelectVideo()
        }
    }

    //region onClicked
    private fun onClickedSelectPhoto()
    {
        picker.pickMultipleImage { imageUris ->

            for(imageUri in imageUris)
            {
                binding.uploadLoader.visibility = View.VISIBLE
                binding.chatRv.scrollToPosition(chats.size - 1)
                Picasso.get().load(imageUri).into(binding.thumb)
                if (conversationModel != null) {
                    profileViewModel.uploadChatImage(this, imageUri, conversationModel!!.lastMessage?.conversationId!!, binding.user!!)
                }
                else
                {
                    profileViewModel.uploadChatImageNew(this, imageUri, binding.user!!)
                }
            }
        }
    }

    private fun onClickedSelectVideo()
    {
        progressDialog.setMessage(getString(R.string.wait_for_compression))
        progressDialog.setMax(100)
        progressDialog.setProgress(0)
        progressDialog.setCancelable(false)
        progressDialog.setProgressStyle(ProgressDialog.ProgressStyle.HorizontalStyle)

        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                progressDialog.onBackPressed { finish() }
            }
        })

        picker.pickVideo({ videoUri ->
            Glide.with(this)
                .asBitmap()
                .load(videoUri)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(binding.thumb)

            binding.uploadLoader.visibility = View.VISIBLE
            binding.chatRv.scrollToPosition(chats.size - 1)
            if (conversationModel != null)
            {
                profileViewModel.uploadChatVideo(this, videoUri, conversationModel!!.lastMessage?.conversationId!!, binding.user!!)
            }
            else
            {
                profileViewModel.uploadChatVideoNew(this, videoUri, binding.user!!)
            }
        }, object : CompressionListener {
            override fun onCancelled(index: Int) { progressDialog.dismiss() }
            override fun onFailure(index: Int, failureMessage: String)  { progressDialog.dismiss() }

            override fun onProgress(index: Int, percent: Float)
            {
                runOnUiThread { progressDialog.setProgress(percent.toInt()) }
            }

            override fun onStart(index: Int)
            {
                runOnUiThread { progressDialog.show() }
            }

            override fun onSuccess(index: Int, size: Long, path: String?) { progressDialog.dismiss() }
        })
    }


    //endregion
}