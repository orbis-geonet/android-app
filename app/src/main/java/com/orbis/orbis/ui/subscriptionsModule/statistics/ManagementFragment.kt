package com.orbis.orbis.ui.subscriptionsModule.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseFragment
import com.orbis.orbis.databinding.FragmentSubscriptionManagementBinding
import com.orbis.orbis.extensions.roundOffDecimal
import com.orbis.orbis.extensions.roundOffDecimalString
import com.orbis.orbis.models.BigDataSharConstants
import com.orbis.orbis.models.subscriptions.Subscription
import com.orbis.orbis.models.subscriptions.SubscriptionStatistic
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class ManagementFragment : BaseFragment() {

    companion object {
        fun newInstance(groupKey: String): ManagementFragment {
            val args = Bundle()
            args.putString("groupKey", groupKey)
            val fragment = ManagementFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var binding: FragmentSubscriptionManagementBinding
    private lateinit var viewModel: ManagementViewModel
    private val subscriptionsProgressAdapter: ProgressAdapter = ProgressAdapter()
    private val revenueProgressAdapter: ProgressAdapter = ProgressAdapter()
    private val typesAdapter: FilterTypesAdapter = FilterTypesAdapter(::onFilterTypeItemClick)

    private var isOneTime = false
    private var groupKey = ""
    private var subscriptionKey = ""
    private var subscriptionName = ""
    private var subscriptionPosition = 0
    private var type = ""
    private var currentPage = 0
    private var filterTypeList: List<FilterType> = listOf()
    private val subscriptionsList: ArrayList<Subscription> = arrayListOf()

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
            R.layout.fragment_subscription_management,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        setupViewModel()
        setupTypesList()
        setupObservers()
        viewModel.getSubscriptions(groupKey, currentPage)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[ManagementViewModel::class.java]
    }

    private fun setupObservers() {
        viewModel.subscriptionsLiveData.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                currentPage++
                subscriptionsList.addAll(it)
                viewModel.getSubscriptions(groupKey, currentPage)
                if (subscriptionPosition == 0) {
                    onHeaderItemClick(subscriptionPosition)
                }
            }
        }
        viewModel.statisticLiveData.observe(viewLifecycleOwner) {
            setData(it)
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

    private fun setData(item: SubscriptionStatistic) {
        binding.tvTitle.text = subscriptionName
        binding.tvTotalSubscriptions.text = item.totalNumber.toString()
        binding.tvTotalRevenue.text = "R$${item.totalAmount.roundOffDecimalString}"
        binding.ivArrowLeft.setOnClickListener {
            onHeaderItemBackClick(subscriptionPosition)
        }
        binding.ivArrowRight.setOnClickListener {
            onHeaderItemNextClick(subscriptionPosition)
        }
        binding.tvSubscriptions.text = item.totalNumber.toString()
        binding.tvRevenue.text = "R$${item.totalAmount.roundOffDecimalString}"
        setProgressData(item)
    }

    private fun setupTypesList() {
        filterTypeList = listOf(
            FilterType(
                getString(R.string.month),
                FilterType.Type.MONTH,
                true
            ),
            FilterType(
                getString(R.string.trimester),
                FilterType.Type.TRIMESTER,
            ),
            FilterType(
                getString(R.string.semester),
                FilterType.Type.SEMESTER,
            ),
            FilterType(
                getString(R.string.year),
                FilterType.Type.YEAR
            ),
        )
        typesAdapter.submitList(filterTypeList)
        binding.rvSubscriptionTypes.adapter = typesAdapter
        binding.rvRevenueTypes.adapter = typesAdapter
        filterTypeList.find { it.isSelected }?.let {
            type = it.value.value
        }
    }

    private fun onHeaderItemBackClick(currentPosition: Int) {
        if (currentPosition > 0) {
            val newPosition = currentPosition - 1
            subscriptionPosition = newPosition
            onHeaderItemClick(subscriptionPosition)
        }
    }

    private fun onHeaderItemNextClick(currentPosition: Int) {
        if (currentPosition < subscriptionsList.size - 1) {
            val newPosition = currentPosition + 1
            subscriptionPosition = newPosition
            onHeaderItemClick(subscriptionPosition)
        }
    }

    private fun onHeaderItemClick(position: Int) {
        val subscription = subscriptionsList[position]

        isOneTime = subscription.type == "ONE_TIME"

        subscriptionName = subscription.name
        subscriptionKey = subscription.subscriptionKey

        BigDataSharConstants.isOneTimePurchase = isOneTime
        BigDataSharConstants.subscriptionKey = subscriptionKey

        if(isOneTime)
        {
            viewModel.getPurchaseKeyStatistic(groupKey, subscriptionKey, type)
        }
        else
        {
            viewModel.getSubscriptionStatistic(groupKey, subscriptionKey, type)
        }
    }

    private fun setProgressData(statistic: SubscriptionStatistic) {
        val numberProgress = arrayListOf<Progress>()
        val totalNumber = statistic.totalNumber.takeIf { it > 0 } ?: 1
        statistic.resultList.forEach {
            numberProgress.add(Progress(it.number, totalNumber, it.columnName))
        }
        subscriptionsProgressAdapter.submitList(numberProgress)
        binding.rvSubscriptionProgress.adapter = subscriptionsProgressAdapter

        val amountProgress = arrayListOf<Progress>()
        val totalAmount = statistic.totalAmount.takeIf { it > 0 } ?: 1f
        statistic.resultList.forEach {
            amountProgress.add(Progress(it.amount.toInt(), totalAmount.toInt(), it.columnName))
        }
        revenueProgressAdapter.submitList(amountProgress)
        binding.rvRevenueProgress.adapter = revenueProgressAdapter
    }

    private fun onFilterTypeItemClick(filterType: FilterType) {
        type = filterType.value.value

        if(isOneTime)
        {
            viewModel.getPurchaseKeyStatistic(groupKey, subscriptionKey, type)
        }
        else
        {
            viewModel.getSubscriptionStatistic(groupKey, subscriptionKey, type)
        }
    }
}