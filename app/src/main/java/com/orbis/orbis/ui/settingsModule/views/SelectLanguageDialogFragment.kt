package com.orbis.orbis.ui.settingsModule.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseBottomSheetFragment
import com.orbis.orbis.databinding.FragmentLanguageListBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.ui.settingsModule.adapter.LanguagesListAdapter
import com.orbis.orbis.ui.settingsModule.viewModel.AppLanguages
import com.orbis.orbis.ui.settingsModule.viewModel.Language
import com.orbis.orbis.utils.hideKeyboard


class SelectLanguageDialogFragment(private val languageListener: LanguageListener) :
    BaseBottomSheetFragment(),
    LanguagesListAdapter.LanguageCardInteraction {
    private var param1: String? = null
    private var param2: String? = null
    private var mAdapter: LanguagesListAdapter? = null
    private lateinit var binding: FragmentLanguageListBinding
    val arrayList = ArrayList<Language>()
    var selectedLang = -1
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentLanguageListBinding.inflate(inflater, container, false)
        return binding.root
    }
    fun setSelectedLanguage(selectedLang: Int) {
        mAdapter?.setSelectedLanguage(selectedLang)
    }
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.languagesRv.setLayoutManager(LinearLayoutManager(requireContext()))
        arrayList.add(Language(getString(R.string.device_language), AppLanguages.DEVICE))
        arrayList.addAll(AppLanguages.supported)
        mAdapter = LanguagesListAdapter(this, requireContext())
        mAdapter!!.setList(arrayList)
        binding.languagesRv.setAdapter(mAdapter)
        val currentLang = PrefManager(requireContext()).getLanguage()
        val currentIndex = arrayList.indexOfFirst { it.key.equals(currentLang, ignoreCase = true) }
        if (currentIndex >= 0) {
            selectedLang = currentIndex
            mAdapter!!.setSelectedLanguage(currentIndex)
        }
        binding.toolbar.titleTv.setText(resources.getString(R.string.languages))
        binding.toolbar.backArrowIv.setOnClickListener { dismiss() }
        hideKeyboard(requireActivity())
    }

    override val layoutId: Int
        get() = R.layout.fragment_language_list
    override val pageTitle: String?
        get() = ""



    override fun onItemClicked(position: Int) {
        languageListener.onSelect(arrayList.get(position).key)

        selectedLang = position
        dismiss()
    }



}