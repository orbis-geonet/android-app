package com.orbis.orbis.ui.groupsModule.views

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseActivity
import com.orbis.orbis.databinding.ActivityEditGroupBinding
import com.orbis.orbis.models.Coordinates
import com.orbis.orbis.models.group.CreateGroupBody
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.ui.groupsModule.adapter.GroupColorsAdapter
import com.orbis.orbis.ui.groupsModule.viewModel.GroupViewModel
import com.orbis.orbis.utils.*
import com.orbis.orbis.utils.picker.Picker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditGroupActivity : BaseActivity(), GroupColorsAdapter.ColorCardInteraction {
    private var mAdapter: GroupColorsAdapter? = null
    private lateinit var list: IntArray
    private var resultUri: Uri? = null
    private lateinit var binding: ActivityEditGroupBinding
    lateinit var viewModel: GroupViewModel
    lateinit var groupDetails: GroupDetails
    private val picker = Picker()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        // Inflate the layout for this fragment
        picker.populate(activity = this)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_group)
        groupDetails = intent.getParcelableExtra("group")!!

        binding.data = CreateGroupBody(
            groupDetails.name,
            groupDetails.location,
            groupDetails.description,
            groupDetails.imageName,
            groupDetails.colorIndex.toInt(),
            groupDetails.strokeColorHex
        )
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        binding.urlGroupTv.text =
            "https//orbis.social/g/" + groupDetails.name.replace(" ", "").lowercase()
        downloadGroupPhoto()
        viewModel = ViewModelProvider(this)[GroupViewModel::class.java]
        val permissionUtil = PermissionUtil()
        if (isLocationPermissionAllowed()) {
            fetchLocation()
        } else {
            checkLocation()
            checkLocationPermission.launch(permissionUtil.locationPermissions)
        }

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
        mAdapter = GroupColorsAdapter(this, this, groupDetails.colorIndex.toInt())
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
        checkLocation()
        binding.updateGroup.setOnClickListener {
            validateData()
        }
        hideKeyboard(this)

        setupObservers()
    }

    private fun downloadGroupPhoto() {
        ViewUtils.loadGroupPhoto(this, binding.selectImageIv, groupDetails.imageName)
        ViewUtils.colorDrawable(
            binding.selectImageIv, Color.parseColor(groupDetails.strokeColorHex)
        )


    }

    private fun isLocationPermissionAllowed(): Boolean {
        return !(ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED)
    }

    private lateinit var locationManager: LocationManager
    private lateinit var checkLocationPermission: ActivityResultLauncher<Array<String>>
    private fun checkLocation() {
        checkLocationPermission = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
                fetchLocation()
            } else {

                // Permission was denied. Display an error message.
            }
        }
    }

    var location: Location? = null
    private fun fetchLocation() {
        val locationListener = object : LocationListener {
            override fun onLocationChanged(it: Location) {
                binding.data?.location = Coordinates(it.longitude, it.latitude)
            }

            override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {
                Log.d("onStatusChanged", s)
            }

            override fun onProviderEnabled(s: String) {
                Log.d("onProviderEnabled", s)
            }

            override fun onProviderDisabled(s: String) {
                Log.d("onProviderDisabled", s)
            }


        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val permissionUtil = PermissionUtil()
            checkLocationPermission.launch(permissionUtil.locationPermissions)
        }
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, 0L, 0F, locationListener
        )
        location = locationManager.getLastKnownLocation(
            LocationManager.GPS_PROVIDER
        )
        if (location != null) {
            binding.data?.location = Coordinates(location!!.longitude, location!!.latitude)
        } else {
            location = locationManager.getLastKnownLocation(
                LocationManager.NETWORK_PROVIDER
            )
            binding.data?.location = Coordinates(location!!.longitude, location!!.latitude)
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this, {
            binding.loading = it
        })
        viewModel.groupDetails.observe(this) {
            Toast.makeText(this, getString(R.string.group_updated), Toast.LENGTH_SHORT)
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
            if (selectedImage != null) {
                viewModel.uploadGroupImage(
                    this@EditGroupActivity,
                    resultUri!!,
                    selectedImage!!,
                    binding.data!!,
                    true,
                    groupDetails.groupKey
                )
            } else {
                viewModel.updateGroup(binding.data!!, groupDetails.groupKey)
            }

        }
    }

    override fun onItemClicked(position: Int) {
        val color = ContextCompat.getColor(this, list[position])
        val hexColor = String.format("#%06X", 0xFFFFFF and color)

        binding.data?.colorIndex = position
        binding.data?.strokeColorHex = hexColor

        ViewUtils.colorDrawable(binding.selectImageIv, color)
    }

    var selectedImage: Bitmap? = null
    //region onClicked
    private fun onClickedPickPhoto()
    {
        picker.pickImage { imageUri, bitmap, tag ->
            resultUri = imageUri
            binding.selectImageIv.setImageBitmap(bitmap)
            selectedImage = bitmap
            binding.addImage.visibility = View.GONE
        }
    }
    //endregion

}