package com.orbis.orbis.ui.groupsModule.views

import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseActivity
import com.orbis.orbis.databinding.ActivityCreateGroupBinding
import com.orbis.orbis.models.Coordinates
import com.orbis.orbis.models.group.CreateGroupBody
import com.orbis.orbis.ui.groupsModule.adapter.GroupColorsAdapter
import com.orbis.orbis.ui.groupsModule.viewModel.GroupViewModel
import com.orbis.orbis.utils.ViewUtils
import com.orbis.orbis.utils.alert
import com.orbis.orbis.utils.hideKeyboard
import com.orbis.orbis.utils.picker.Picker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateGroupActivity : BaseActivity(), GroupColorsAdapter.ColorCardInteraction {
    private var mAdapter: GroupColorsAdapter? = null
    private lateinit var list: IntArray
    private var resultUri: Uri? = null
    private lateinit var binding: ActivityCreateGroupBinding
    lateinit var viewModel: GroupViewModel
    private val picker = Picker()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout for this fragment
        picker.populate(activity = this)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_group)
        binding.data = CreateGroupBody()

        viewModel = ViewModelProvider(this)[GroupViewModel::class.java]
        location = intent.getParcelableExtra("location")

        binding.data?.location = Coordinates(location!!.longitude, location!!.latitude)

        binding.colorsRv.setLayoutManager(
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        )
        list = intArrayOf(
            R.color.g_color1,
            R.color.g_color2,
            R.color.g_color3,
            R.color.g_color4,
            R.color.g_color5,
            R.color.g_color6,
            R.color.g_color7
        )
        mAdapter = GroupColorsAdapter(this, this)
        mAdapter!!.setList(list)
        binding.colorsRv.setAdapter(mAdapter)
        binding.toolbar.titleTv.setText("")
        binding.selectImageIv.setOnClickListener {
            onClickedPickPhoto()
        }
        binding.addImage.setOnClickListener {
            onClickedPickPhoto()
        }
        binding.toolbar.backArrowIv.setOnClickListener { this.onBackPressed() }

        binding.groupNameEt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {
                if (s.length != 0) binding.urlGroupTv.text =
                    "https//orbis.social/g/" + s.toString().replace(" ", "").lowercase()
                else binding.urlGroupTv.text = "https//orbis.social/g/"
            }
        })
        binding.createGropBtn.setOnClickListener {
            validateData()
        }
        binding.aboutGroupEt.setOnEditorActionListener { _, actionId, event ->
            val done =
                actionId == EditorInfo.IME_ACTION_DONE ||
                        event?.keyCode == KeyEvent.KEYCODE_ENTER
            if (done) {
                hideKeyboard(this)
                binding.aboutGroupEt.clearFocus()
                true
            } else false
        }
        hideKeyboard(this)

        setupObservers()
    }

    var location: Location? = null
    private fun setupObservers() {
        viewModel.isLoading.observe(this) {
            binding.loading = it
        }
        viewModel.groupDetails.observe(this) {
            Toast.makeText(this, getString(R.string.group_created), Toast.LENGTH_SHORT)
                .show()
            it.imageName = binding.data?.imageName!!
            val intent = Intent(this, GroupDetailsActivity::class.java)
            intent.putExtra("data", it)
            intent.putExtra("location", location)
            startActivity(intent)
            finish()
        }
        viewModel.error.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()

        }
    }

    private fun validateData() {
        if (binding.data?.name!!.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.enter_group_name),
                Toast.LENGTH_SHORT
            ).show()
        } else if (binding.data?.description!!.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.enter_group_description),
                Toast.LENGTH_SHORT
            ).show()
        } else if (binding.data?.description!!.length < 15) {
            Toast.makeText(
                this,
                getString(R.string.group_description_small),
                Toast.LENGTH_SHORT
            ).show()
        } else if (selectedImage == null) {
            Toast.makeText(
                this,
                getString(R.string.select_a_group_image),
                Toast.LENGTH_SHORT
            ).show()
        } else if (binding.data?.colorIndex == -1) {
            Toast.makeText(this, getString(R.string.choose_a_color), Toast.LENGTH_SHORT)
                .show()
        } else if (binding.data?.location == null) {
            Toast.makeText(
                this,
                getString(R.string.unable_location),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            this.alert(resources.getString(R.string.it_is_forbidden_to), null) {
                positiveButton(resources.getString(R.string.i_understood)) {
                    viewModel.uploadGroupImage(
                        this@CreateGroupActivity,
                        resultUri!!,
                        selectedImage!!,
                        binding.data!!
                    )
                }
            }.show()
        }
    }

    override fun onItemClicked(position: Int) {
        //   Toast.makeText(this, "color selected " + position, Toast.LENGTH_SHORT).show()
        val color = ContextCompat.getColor(
            this,
            list[position]
        )
        val hexColor = String.format("#%06X", 0xFFFFFF and color)
        binding.data?.colorIndex = position
        binding.data?.strokeColorHex = hexColor
        if (resultUri != null) {
            ViewUtils.colorDrawable(
                binding.selectImageIv, color
            )
        } else
            Toast.makeText(this, getString(R.string.please_select_image), Toast.LENGTH_SHORT).show()
    }

    var selectedImage: Bitmap? = null
    //region onClicked
    private fun onClickedPickPhoto()
    {
        picker.pickImage { imageUri, bitmap, _ ->
            resultUri = imageUri
            binding.selectImageIv.setImageBitmap(bitmap)
            selectedImage = bitmap
            binding.cameraIconIv.visibility = View.GONE
        }
    }
    //endregion

}