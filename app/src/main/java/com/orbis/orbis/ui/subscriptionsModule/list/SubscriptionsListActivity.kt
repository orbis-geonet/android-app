package com.orbis.orbis.ui.subscriptionsModule.list

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseActivity
import com.orbis.orbis.databinding.ActivitySubscriptionsListBinding
import com.orbis.orbis.extensions.inVisible
import com.orbis.orbis.models.subscriptions.Subscription
import com.orbis.orbis.ui.subscriptionsModule.EndlessRecyclerViewScrollListener
import com.orbis.orbis.ui.subscriptionsModule.create.CreateSubscriptionActivity
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SubscriptionsListActivity : BaseActivity() {

    companion object {
        const val ACTIVITY_RESULT_CODE = 101

        fun open(
            activity: Activity,
            groupKey: String,
            isAdmin: Boolean,
        ) {
            val intent = Intent(activity, SubscriptionsListActivity::class.java)
            intent.putExtra("groupKey", groupKey)
            intent.putExtra("isAdmin", isAdmin)
            activity.startActivityForResult(intent, ACTIVITY_RESULT_CODE)
        }
    }

    private lateinit var binding: ActivitySubscriptionsListBinding
    private lateinit var viewModel: SubscriptionsListViewModel
    private var subscriptionsListAdapter: SubscriptionsListAdapter? = null
    private var groupKey = ""
    private var isAdmin = false

    private var paymentSheet: PaymentSheet? = null
    private var itemPos: Int? = null

    private var scrollListener: EndlessRecyclerViewScrollListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupKey = intent?.getStringExtra("groupKey") ?: ""
        isAdmin = intent?.getBooleanExtra("isAdmin", false) ?: false
        setupBinding()
        initView()
        initStripe()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == CreateSubscriptionActivity.ACTIVITY_RESULT_CODE) {
                reset()
            }
        }
    }

    private fun initView() {
        setupToolbar()
        setupViewModel()
        setupSubscriptionsList()
        observeSubscriptions()
        reset()
    }

    private fun setupBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_subscriptions_list)
    }

    private fun setupToolbar() {
        binding.toolbar.titleTv.text =
            resources.getString(R.string.subscriptions)
        binding.toolbar.backArrowIv.setOnClickListener {
            finish()
        }
        val ivPlus = binding.toolbar.plusIv
        ivPlus.inVisible(isAdmin.not())
        ivPlus.setOnClickListener {
            CreateSubscriptionActivity.open(this, groupKey, null)
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[SubscriptionsListViewModel::class.java]
    }

    private fun setupSubscriptionsList() {
        subscriptionsListAdapter = SubscriptionsListAdapter(
            isAdmin,
            ::onEditClick,
            ::onDeleteClick,
            ::onSubscribeClick,
            ::onUnsubscribeClick,
            ::onPhotosClick,
            ::onQuantityClick
        )
        binding.rvSubscriptions.adapter = subscriptionsListAdapter

        val pagerSnapHelper = object : PagerSnapHelper() {
            override fun findTargetSnapPosition(
                layoutManager: RecyclerView.LayoutManager?,
                velocityX: Int,
                velocityY: Int
            ): Int {
                val snapPosition = super.findTargetSnapPosition(layoutManager, velocityX, velocityY)
                binding.tabLayout.getTabAt(snapPosition)?.select()
                return snapPosition
            }
        }
        pagerSnapHelper.attachToRecyclerView(binding.rvSubscriptions)
        scrollListener =
            object : EndlessRecyclerViewScrollListener(binding.rvSubscriptions.layoutManager!!) {
                override fun onLoadMore(page: Int, totalItemsCount: Int) {
                    getData(page)
                }

                override val isLoading: Boolean
                    get() = viewModel.isDataLoading
            }
        binding.rvSubscriptions.addOnScrollListener(scrollListener!!)
    }

    private fun observeSubscriptions() {
        viewModel.subscriptionsLiveData.observe(this) {
            val currentList = subscriptionsListAdapter?.currentList ?: listOf()
            subscriptionsListAdapter?.submitList(currentList + it)
            createPagerIndicator(it.size)
        }
        viewModel.subscribeLiveData.observe(this) {
            itemPos = it.position
            presentPaymentSheet(it.clientSecret, it.publicToken)
        }
        viewModel.unsubscribeLiveData.observe(this) {
            subscriptionsListAdapter?.update(it, false)
        }
        viewModel.deleteSubscriptionLiveData.observe(this) {
            subscriptionsListAdapter?.remove(it)
        }
        viewModel.error.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
        viewModel.isLoading.observe(this) {
            binding.loading = it
        }
    }

    private fun getData(page: Int) {
        viewModel.getSubscriptions(groupKey, page)
    }

    private fun onEditClick(position: Int, item: Subscription) {
        CreateSubscriptionActivity.open(this, groupKey, item)
    }

    private fun onDeleteClick(position: Int, item: Subscription) {
        val deleteSubscriptionDialog =
            DeleteSubscriptionDialogFragment.newInstance(getString(R.string.delete_subscription_confirmation))
        deleteSubscriptionDialog.onDeleteClick = {
            viewModel.deleteSubscription(position, groupKey, item.subscriptionKey)
        }
        deleteSubscriptionDialog.show(supportFragmentManager, "DeleteSubscriptionDialogFragment")
    }

    private fun onSubscribeClick(position: Int, item: Subscription, quantity: Int = 0) {
        if(quantity == 0)
        {
            viewModel.subscribeSubscription(position, item.subscriptionKey)
        }
        else
        {
            viewModel.purchaseSubscription(position, item.subscriptionKey, quantity)
        }
    }

    private fun onUnsubscribeClick(position: Int, item: Subscription) {
        val deleteSubscriptionDialog =
            DeleteSubscriptionDialogFragment.newInstance(getString(R.string.unsubscribe_subscription_confirmation))
        deleteSubscriptionDialog.onDeleteClick = {
            viewModel.unsubscribeSubscription(position, item.subscriptionKey)
        }
        deleteSubscriptionDialog.show(supportFragmentManager, "DeleteSubscriptionDialogFragment")
    }

    private fun onPhotosClick(imagesName: ArrayList<String>)
    {
        try
        {
            val subscriptionImageDialogFragment = SubscriptionImageDialogFragment.newInstance(imagesName)
            subscriptionImageDialogFragment.show(supportFragmentManager, SubscriptionImageDialogFragment::class.java.simpleName)
        }
        catch (e: Exception) { e.printStackTrace() }
    }

    private fun onQuantityClick(position: Int)
    {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.enter_quanitity))

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        builder.setView(input)

        builder.setPositiveButton(resources.getString(R.string.ok)) { _: DialogInterface, _: Int ->
            var enteredNumber = input.text.toString().toInt()
            enteredNumber = if(enteredNumber == 0) { 1 } else { enteredNumber }

            subscriptionsListAdapter?.updateQuantity(position, enteredNumber)
        }

        builder.setNegativeButton(resources.getString(R.string.cancel)) { dialog: DialogInterface, _: Int ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun initStripe() {
        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)
    }

    private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        when (paymentSheetResult) {
            is PaymentSheetResult.Canceled -> {
            }
            is PaymentSheetResult.Failed -> {
                Toast.makeText(
                    this,
                    paymentSheetResult.error.message ?: "",
                    Toast.LENGTH_SHORT
                ).show()
            }
            is PaymentSheetResult.Completed -> {
                itemPos?.let {
                    subscriptionsListAdapter?.update(it, true)
                }
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    private fun presentPaymentSheet(clientSecret: String, publicToken: String) {
        PaymentConfiguration.init(this, publicToken)
        paymentSheet?.presentWithPaymentIntent(
            clientSecret,
            PaymentSheet.Configuration(getString(R.string.app_name))
        )
    }

    private fun createPagerIndicator(count: Int) {
        for (i in 1..count) {
            binding.tabLayout.addTab(binding.tabLayout.newTab())
        }
    }

    private fun reset() {
        binding.tabLayout.removeAllTabs()
        scrollListener?.resetState()
        itemPos = null
        subscriptionsListAdapter?.submitList(null)
        getData(0)
    }
}
