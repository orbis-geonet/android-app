package com.orbis.orbis.ui.commentModule.views

import android.content.DialogInterface
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseBottomSheetFragment
import com.orbis.orbis.databinding.FragmentCommentDialogueBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.posts.CommentModel
import com.orbis.orbis.models.posts.CreateCommentBody
import com.orbis.orbis.ui.commentModule.adapter.CommentsAdapter
import com.orbis.orbis.ui.newsFeedModule.viewModel.FeedViewModel
import com.orbis.orbis.ui.storiesModule.views.DialogueListener

import com.orbis.orbis.utils.hideKeyboard
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent


@AndroidEntryPoint
class CommentDialogFragment(
    private val postKey: String,
    private val listener: CommentDialogue? = null
) : BottomSheetDialogFragment(),
    CommentsAdapter.CommentInteraction {
    lateinit var mAdapter: CommentsAdapter
    lateinit var binding: FragmentCommentDialogueBinding
    lateinit var viewModel: FeedViewModel
    val comments: ArrayList<CommentModel> = ArrayList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogStyle)

    }

    override fun onDestroy() {
        listener?.onDismissDialogue()
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_comment_dialogue, container, false)
        initView()
        return binding.root
    }


    private fun initView() {
        KeyboardVisibilityEvent.setEventListener(
            activity
        ) { isOpen ->
            if (isOpen) {

                //scroll to last view
//                binding.nested.scrollToDescendant(binding.constraintLayout2)

            }
        }
        viewModel = ViewModelProvider(this)[FeedViewModel::class.java]
        viewModel.getCommentsByPost(postKey, 0)
        binding.commentsRv.layoutManager = LinearLayoutManager(requireContext())
        mAdapter = CommentsAdapter(requireContext(), comments, this)
        binding.commentsRv.adapter = mAdapter
        setupObservers()
        if (Constants.userImage.isNotEmpty()) {
            Picasso.get().load(Constants.userImage).into(binding.userIconIv)
        }
        binding.sendIv.setOnClickListener {
            createComment()
        }

        val isLoggedIn = !PrefManager(requireContext()).getIdToken().isNullOrEmpty()
        if (!isLoggedIn) {
            binding.userNameEt.isEnabled = false
            binding.userNameEt.hint = requireContext().getString(R.string.login_to_comment)
            binding.sendIv.alpha = 0.4f
            binding.sendIv.isEnabled = false
        } else {
            binding.sendIv.setOnClickListener {
                createComment()
            }
        }
    }

    private fun setupObservers() {
        viewModel.comments.observe(viewLifecycleOwner) {

            comments.addAll(it)

            mAdapter.notifyDataSetChanged()
            binding.commentsRv.scrollToPosition(comments.size - 1)
            listener?.updateCommentCount(postKey, comments.size)
        }
        viewModel.commentPosted.observe(viewLifecycleOwner) {

            binding.userNameEt.setText("")
            for (i in 0 until comments.size) {
                comments[i].selectedForReply = false
            }
            if (it) {
                comments.clear()
                viewModel.getCommentsByPost(postKey, 0)
            }
        }
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.loading = it
        }
    }

    override fun onDismiss(dialog: DialogInterface) {

        super.onDismiss(dialog)
    }

    private fun createComment() {
        val text = binding.userNameEt.text.toString()
        if (text.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.enter_comment), Toast.LENGTH_SHORT)
                .show()
        } else {
            binding.loading = true
            val body = CreateCommentBody(text)
            for (comment in comments) {
                if (comment.selectedForReply) {
                    body.replyToKey = comment.commentKey
                }
            }
            hideKeyboard(requireActivity())
            viewModel.postComment(postKey, body)

        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (dialog as? BottomSheetDialog)?.behavior?.state = STATE_EXPANDED
        //title_tv.setText(resources.getString(R.string.comments))
        //back_arrow_iv.setOnClickListener { dismiss() }
        hideKeyboard(requireActivity())
    }

    //    override val layoutId: Int
//        get() = R.layout.fragment_comment
//    override val pageTitle: String?
//        get() = ""
    override fun onPause() {
        listener?.onDismissDialogue()
        super.onPause()

    }

    override fun onLike(position: Int, commentKey: String) {
        viewModel.likeComment(postKey, commentKey)
    }

    override fun onUnLike(position: Int, commentKey: String) {
        viewModel.unlikeComment(postKey, commentKey)
    }

    override fun onReply(position: Int) {
        val prev = comments[position].selectedForReply
        for (i in 0 until comments.size) {
            comments[i].selectedForReply = false
        }
        comments[position].selectedForReply = !prev
        mAdapter.notifyDataSetChanged()
    }

    override fun onDelete(commentKey: String) {
        viewModel.deleteComment(postKey, commentKey)
    }


}

interface CommentDialogue {
    fun onDismissDialogue()
    fun updateCommentCount(postKey: String, count: Int)
}