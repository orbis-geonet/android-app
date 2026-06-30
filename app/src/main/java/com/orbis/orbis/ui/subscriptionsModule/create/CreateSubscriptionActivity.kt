package com.orbis.orbis.ui.subscriptionsModule.create

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Layout
import android.text.SpannableString
import android.text.style.AlignmentSpan
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseActivity
import com.orbis.orbis.databinding.ActivityCreateSubscriptionBinding
import com.orbis.orbis.extensions.show
import com.orbis.orbis.models.subscriptions.CreateStripeBody
import com.orbis.orbis.models.subscriptions.CreateSubscriptionBody
import com.orbis.orbis.models.subscriptions.Subscription
import com.orbis.orbis.ui.groupsModule.adapter.ImageSliderAdapterLocal
import com.orbis.orbis.ui.subscriptionsModule.create.benefits.BenefitsAdapter
import com.orbis.orbis.utils.picker.Picker
import dagger.hilt.android.AndroidEntryPoint
import java.util.ArrayList

@AndroidEntryPoint
class CreateSubscriptionActivity : BaseActivity() {

    companion object {
        const val ACTIVITY_RESULT_CODE = 102

        fun open(
            activity: Activity,
            groupKey: String,
            subscription: Subscription?
        ) {
            val intent = Intent(activity, CreateSubscriptionActivity::class.java)
            intent.putExtra("groupKey", groupKey)
            intent.putExtra("subscription", subscription)
            activity.startActivityForResult(intent, ACTIVITY_RESULT_CODE)
        }
    }

    private lateinit var binding: ActivityCreateSubscriptionBinding
    private lateinit var viewModel: CreateSubscriptionViewModel
    private lateinit var benefitsAdapter: BenefitsAdapter

    private var groupKey = ""
    private var subscription: Subscription? = null
    private var price = 0f
    private var total = 0f
    private var orbisCommission = 0f
    private var stripeCommission = 0f
    private var stripeAdditionFee = 0f
    private var currency = ""

    private var paymentType = "ONE_TIME"
    private var paymentInterval = "MONTH"

    private val picker = Picker()
    private lateinit var imageSliderAdapter: ImageSliderAdapterLocal
    private val pickedImages: ArrayList<String> = ArrayList()
    private val pickedUris: ArrayList<Uri> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupKey = intent.getStringExtra("groupKey") ?: ""
        subscription = if (Build.VERSION.SDK_INT >= 33)
        {
            intent.getParcelableExtra("subscription", Subscription::class.java)
        } else
        {
            intent.getParcelableExtra("subscription") as Subscription?
        }
        setupBinding()
        initView()

    }

    private fun initView() {
        setupViewModel()
        setupToolbar()
        setupBenefitsList()
        setupObservers()
        setOnClicked()

        picker.populate(activity = this)

        binding.btnCreate.setOnClickListener {
            viewModel.getStripe()

//            if (subscription != null) {
//                editSubscription()
//            } else {
//                createSubscription()
//            }
        }
        binding.etSubscriptionPrice.doAfterTextChanged {
            calculateTax(it.toString())
        }
        setDataIfExist()
        viewModel.getSubscriptionInfo()
    }

    private fun setupBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_subscription)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[CreateSubscriptionViewModel::class.java]
    }

    private fun setupToolbar() {
        binding.toolbar.titleTv.text =
            resources.getString(R.string.create_subscription)
        binding.toolbar.backArrowIv.setOnClickListener {
            finish()
        }
    }

    private fun calculateTax(input: String) {
        price = try {
            input.toFloat()
        } catch (e: Exception) {
            0f
        }
        total =
            (price + orbisCommission * price) + (price + orbisCommission * price) * stripeCommission + stripeAdditionFee
        val fees = total - price
        binding.tvTaxHint.text = getString(R.string.fees_total, fees, total)
    }

    private fun setupBenefitsList() {
        benefitsAdapter = BenefitsAdapter()
        binding.rvBenefits.adapter = benefitsAdapter
    }

    private fun setupObservers() {
        viewModel.stripeCreatedLiveData.observe(this) {
            openStripe(this, it.setupAccountUrl)
        }
        viewModel.stripeStatusLiveData.observe(this) {
            checkStripe(it)
        }
        viewModel.subscriptionInfoLiveData.observe(this) {
            orbisCommission = it.orbisCommission
            stripeCommission = it.stripeCommission
            stripeAdditionFee = it.stripeAdditionFee
            currency = it.currencies.firstOrNull() ?: ""
            calculateTax(price.toString())
        }
        viewModel.subscriptionCreatedLiveData.observe(this) {
            setResult(RESULT_OK)
            finish()
        }
        viewModel.imageUploadedLiveData.observe(this) {
            if(it)
            {
                if (subscription != null) {
                    viewModel.editSubscription(groupKey,  viewModel.requestBody!!)
                } else {
                    viewModel.createSubscription(groupKey, viewModel.requestBody!!)
                }
            }
        }
        viewModel.subscriptionCreatedLiveData.observe(this) {
            setResult(RESULT_OK)
            finish()
        }
        viewModel.error.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
        viewModel.isLoading.observe(this) {
            binding.loading = it
        }
    }

    private fun createStripe() {
        viewModel.createStripe(CreateStripeBody("BR", "INDIVIDUAL"))
    }

    private fun isDataValid(): Boolean {
        if (binding.etSubscriptionName.text.toString().isEmpty()) {
            binding.tvNameError.text = getString(R.string.enter_subscription_name)
            binding.tvNameError.show()
            return false
        }

        if (price == 0f) {
            binding.tvPriceError.text = getString(R.string.enter_subscription_price)
            binding.tvPriceError.show()
            return false
        }

        if (binding.etSubscriptionDescription.text.toString().isEmpty()) {
            binding.tvDescriptionError.text = getString(R.string.enter_subscription_description)
            binding.tvDescriptionError.show()
            return false
        }
        return true
    }

    private fun createSubscription() {
        if (isDataValid().not()) {
            return
        }

        val quantity = if(binding.installmentEditText.text.isEmpty()) { 1 } else { binding.installmentEditText.text.toString().toInt()}
        val body = CreateSubscriptionBody(
            name = binding.etSubscriptionName.text.toString(),
            price = if(quantity == 1) { total } else { (total / quantity.toFloat()) },
            originalPrice = price,
            currency = currency,
            description = binding.etSubscriptionDescription.text.toString(),
            benefit = benefitsAdapter.getData(),
            interval = paymentInterval,
            period = quantity,
            type = paymentType
        )

        viewModel.requestBody = body
        viewModel.uploadSubscriptionImage(this, pickedUris, 0)
    }

    private fun editSubscription() {
        if (isDataValid().not()) {
            return
        }

        val quantity = subscription!!.period

        val body = CreateSubscriptionBody(
            subscriptionKey = subscription?.subscriptionKey,
            name = binding.etSubscriptionName.text.toString(),
            price = if(quantity == 1) { total } else { (total / quantity.toFloat()) },
            originalPrice = price,
            currency = currency,
            description = binding.etSubscriptionDescription.text.toString(),
            benefit = benefitsAdapter.getData(),
            interval = paymentInterval,
            period = quantity,
            type = paymentType,
        )

        viewModel.requestBody = body
        viewModel.uploadSubscriptionImage(this, pickedUris, 0)
    }

    private fun openStripe(context: Context, url: String) {
        val browserIntentBuilder = CustomTabsIntent.Builder()
        val customTabColorSchemeParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(ContextCompat.getColor(context, R.color.white))
            .build()
        val browserIntent = browserIntentBuilder
            .setDefaultColorSchemeParams(customTabColorSchemeParams)
            .build()
        browserIntent.launchUrl(this, Uri.parse(url))
    }

    private fun setDataIfExist() {
        subscription?.let {
            binding.etSubscriptionName.setText(it.name)
            binding.etSubscriptionDescription.setText(it.description)
            binding.etSubscriptionPrice.setText(it.originalPrice.toString())
            currency = it.currency
            benefitsAdapter.setData(it.benefits)
            binding.btnCreate.text = getString(R.string.edit_subscription)
            binding.toolbar.titleTv.text =
                resources.getString(R.string.edit_subscription)

            if(it.type == "UNLIMITED" && it.interval == "YEAR")
            {
                binding.paymentFrequencyTextView.text = getString(R.string.yearly)
            }
            else if(it.type == "UNLIMITED" && it.interval == "MONTH")
            {
                binding.paymentFrequencyTextView.text = getString(R.string.monthly)
            }
            else if(it.type == "INTERVAL" && it.interval == "MONTH")
            {
                binding.installmentEditText.isVisible = true
                binding.paymentFrequencyTextView.text = getString(R.string.payment_in_installments)
                binding.installmentEditText.setText(it.period.toString())
            }
            else if(it.type == "ONE_TIME" && it.interval == "MONTH")
            {
                binding.paymentFrequencyTextView.text = getString(R.string.one_time)
            }

            paymentInterval = it.interval
            paymentType = it.type
        }
    }

    private fun checkStripe(status: String) {
        when (status) {
            "CREATED" -> {
                viewModel.updateStripe()
            }
            "VALIDATION_FAILED" -> {
                viewModel.updateStripe()
            }
            "READY_TO_USE" -> {
                if (subscription != null) {
                    editSubscription()
                } else {
                    createSubscription()
                }
            }
            else -> {
                createStripe()
            }
        }
    }

    //region PaymentFrequency & Image Picker
    private fun setOnClicked()
    {
        binding.paymentFrequencyLayout.setOnClickListener { onPaymentFrequencyClicked() }
        binding.photosLayout.setOnClickListener { onClickedPhotoPick() }
        binding.closeIv.setOnClickListener { hidePhotoSlider() }
    }

    private fun onPaymentFrequencyClicked()
    {
        val wrapper = ContextThemeWrapper(this, R.style.PopupMenu)
        val popup = PopupMenu(wrapper, binding.paymentFrequencyLayout, Gravity.CENTER)

        val menu: Menu = popup.menu
        menu.add(0, 1, 0, getString(R.string.yearly))
        menu.add(0, 2, 0, getString(R.string.monthly))
        menu.add(0, 3, 0, getString(R.string.payment_in_installments))
        menu.add(0, 4, 0, getString(R.string.one_time))

        for (i in 0 until menu.size())
        {
            val menuItem = menu.getItem(i)
            val menuTitle = menuItem.title.toString()
            val spannableString = SpannableString(menuTitle)
            spannableString.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),0, spannableString.length, 0 )
            menuItem.title = spannableString
        }
        popup.setOnMenuItemClickListener { item -> onPaymentFrequencySelected(item); true }
        popup.show()
    }

    private fun onPaymentFrequencySelected(item: MenuItem)
    {
        when(item.itemId)
        {
            1 -> {
                paymentType = "UNLIMITED"
                paymentInterval = "YEAR"
            }
            2 -> {
                paymentType = "UNLIMITED"
                paymentInterval = "MONTH"
            }
            3 -> {
                paymentType = "INTERVAL"
                paymentInterval = "MONTH"
            }
            4 -> {
                paymentType = "ONE_TIME"
                paymentInterval = ""
            }
        }
        binding.paymentFrequencyTextView.text = item.title.toString()
        binding.installmentEditText.isVisible = item.itemId == 3
        binding.installmentEditText.text.clear()
    }

    private fun onClickedPhotoPick()
    {
        picker.pickMultipleImage { imageUris ->
            for(imageUri in imageUris)
            {
                imageUri.path?.let {
                    pickedImages.add(it)
                    pickedUris.add(imageUri)
                }
            }

            imageSliderAdapter = ImageSliderAdapterLocal(this, pickedImages)
            binding.checkInViewpager.adapter = imageSliderAdapter
            showPhotoSlider()
        }
    }

    private fun showPhotoSlider() {
        binding.closeIv.visibility = View.VISIBLE
        binding.photoSliderLayout.visibility = View.VISIBLE
    }

    private fun hidePhotoSlider() {
        pickedImages.clear()
        pickedUris.clear()
        binding.closeIv.visibility = View.GONE
        binding.photoSliderLayout.visibility = View.GONE
    }


    //endregion
}