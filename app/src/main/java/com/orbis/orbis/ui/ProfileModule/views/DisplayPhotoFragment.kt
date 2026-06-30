package com.orbis.orbis.ui.ProfileModule.views

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseFragment
import com.orbis.orbis.utils.hideKeyboard
import com.orbis.orbis.databinding.FragmentDisplayPhotoBinding


class DisplayPhotoFragment : BaseFragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    lateinit var binding: FragmentDisplayPhotoBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDisplayPhotoBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        var drawable=getArguments()?.getInt("photoInt", 0)
        binding.photoIv.setImageDrawable(resources.getDrawable(drawable!!,null))

        hideKeyboard(requireActivity())
    }

    companion object {
        
    }


}