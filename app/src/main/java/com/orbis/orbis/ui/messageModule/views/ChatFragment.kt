package com.orbis.orbis.ui.messageModule.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseFragment
import com.orbis.orbis.ui.messageModule.adapter.ChatAdapter
import com.orbis.orbis.ui.messageModule.adapter.MessageSearchListAdapter
import com.orbis.orbis.ui.messageModule.viewModel.Chat
import com.orbis.orbis.utils.hideKeyboard
import com.orbis.orbis.databinding.FragmentChatBinding

class ChatFragment : BaseFragment(), ChatAdapter.messageBubbleInteraction,
    MessageSearchListAdapter.MessageSearchInteraction {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var binding: FragmentChatBinding
    private var mAdapter: ChatAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.messagesRv.setLayoutManager(LinearLayoutManager(requireContext()))
        val arrayList = ArrayList<Chat>()
        arrayList.add(
            Chat(
                1,
                "Michael Bruno",
                R.drawable.image_test3,
                "",
                "Yesterday 11:25",
                false, 3
            )
        )
        arrayList.add(
            Chat(
                1,
                "Michael Bruno",
                R.drawable.image_test3,
                "Hi",
                "12:33",
                false, 1
            )
        )
        arrayList.add(
            Chat(
                2,
                "Michael Bruno",
                R.drawable.image_test3,
                "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, ",
                "12:34",
                true, 2
            )
        )
        arrayList.add(
            Chat(
                1,
                "Michael Bruno",
                R.drawable.image_test3,
                "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod",
                "12:34",
                true, 2
            )
        )
        arrayList.add(
            Chat(
                1,
                "Michael Bruno",
                R.drawable.image_test3,
                "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod",
                "12:34",
                true, 1
            )
        )

        arrayList.add(
            Chat(
                1,
                "Michael Bruno",
                R.drawable.image_test3,
                "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod",
                "12:34",
                true, 2
            )
        )


        arrayList.add(
            Chat(
                1,
                "Michael Bruno",
                R.drawable.image_test3,
                "ok",
                "12:35",
                true, 2
            )
        )

        arrayList.add(
            Chat(
                1,
                "Michael Bruno",
                R.drawable.image_test3,
                "ok",
                "12:35",
                true, 2
            )
        )


        arrayList.add(
            Chat(
                1,
                "Michael Bruno",
                R.drawable.image_test3,
                "ok",
                "12:35",
                true, 2
            )
        )

        binding.sendIv.setOnClickListener {
            arrayList.add(
                Chat(
                    1,
                    "Michael Bruno",
                    R.drawable.image_test3,
                    binding.userNameEt.text.toString(),
                    "12:35",
                    true, 1
                )
            )
            binding.userNameEt.setText("")
            mAdapter?.notifyDataSetChanged()
        }

        mAdapter = ChatAdapter(this, requireContext())
        mAdapter!!.setList(arrayList)
        binding.messagesRv.setAdapter(mAdapter)
        binding.toolbar.backArrowIv.setOnClickListener { requireActivity().onBackPressed() }
        hideKeyboard(requireActivity())
    }


    companion object {
        fun getInstance(): ChatFragment {
            return ChatFragment()
        }
    }

    override fun onItemClicked() {

    }

}