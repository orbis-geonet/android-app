package com.orbis.orbis.ui.placesModule.views

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface.OnShowListener
import android.location.Location
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.maps.android.ui.IconGenerator
import com.orbis.orbis.R
import com.orbis.orbis.databinding.FragmentCreatePlaceMapBinding
import com.orbis.orbis.listeners.OnPlaceCreate
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.Coordinates
import com.orbis.orbis.models.place.CreatePlaceBody
import com.orbis.orbis.ui.homeModule.views.MapFragment
import com.orbis.orbis.ui.placesModule.PlaceViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class CreatePlaceMapDialogFragment : BottomSheetDialogFragment(), GoogleMap.OnMarkerClickListener, GoogleMap.OnCameraMoveListener {

    companion object {
        fun newInstance(
            createPlaceBody: CreatePlaceBody? = null,
            icon: Int,
            location: Location
        ): CreatePlaceMapDialogFragment{
            val args = Bundle()
            args.putParcelable("createPlaceBody", createPlaceBody)
            args.putParcelable("location", location)
            args.putInt("icon", icon)
            val fragment = CreatePlaceMapDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var createPlaceBody: CreatePlaceBody? = null
    lateinit var mLastLocation: Location
    private var icon: Int = 0
    var listener: OnPlaceCreate? = null

    private lateinit var mMap: GoogleMap
    lateinit var mapFragment: SupportMapFragment
    private var myLocationMarker: Marker? = null
    private var marker: Marker? = null
    lateinit var binding: FragmentCreatePlaceMapBinding
    lateinit var viewModel: PlaceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createPlaceBody = arguments?.getParcelable("createPlaceBody") as? CreatePlaceBody?
        mLastLocation = arguments?.getParcelable("location") as? Location ?: viewModel.lastLocationSelected ?: Constants.location ?: Location("")
        icon = arguments?.getInt("icon") ?: 0
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        generateMarker()
        mapFragment =
            childFragmentManager.findFragmentById(R.id.createPlaceMap) as SupportMapFragment
        mapFragment.getMapAsync(callback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("mapViewGotData", createPlaceBody?.name?:"")
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_create_place_map, container, false)
        binding.data = createPlaceBody
        viewModel = ViewModelProvider(this).get(PlaceViewModel::class.java)
        val view = binding.root
        val title = view.findViewById(R.id.title_tv) as TextView?
        val back_arrow = view.findViewById(R.id.back_arrow_iv) as ImageView?

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(0, 0, 0, navBarInsets.bottom)
            insets
        }
        title?.setText(resources.getString(R.string.create_place))
        back_arrow?.setOnClickListener { dismiss() }
        setupObservers()
        binding.createPlaceBtn.setOnClickListener {
            viewModel.createPlace(binding.data!!)
        }

        // get the views and attach the listener
        return binding.root
    }



    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.loading = it
        }
        viewModel.placeDetails.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), "Place Created Successfully", Toast.LENGTH_SHORT)
                .show()
            listener?.onPlaceCreate(it)
            dismiss()
        }
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog: Dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener(OnShowListener { dialogInterface ->
            // val bottomSheetDialog = dialogInterface as BottomSheetDialog
            setupFullHeight(dialog)
        })
        return dialog
    }


    private fun setupFullHeight(bottomSheetDialog: Dialog) {
        val bottomSheet = bottomSheetDialog.findViewById<View>(R.id.design_bottom_sheet)
        val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
        val layoutParams = bottomSheet!!.layoutParams
        val windowHeight = getWindowHeight()
        if (layoutParams != null) {
            layoutParams.height = windowHeight- 50
        }
        bottomSheetDialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        bottomSheet.layoutParams = layoutParams
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun getWindowHeight(): Int {
        // Calculate window height for fullscreen use
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }


    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->
        Log.d("mapReady", "mapReady")
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */

        mMap = googleMap
        mMap.setOnCameraMoveListener(this)
        mMap.setOnMarkerClickListener(this)

        mMap.setOnMarkerDragListener(object : OnMarkerDragListener {
            override fun onMarkerDragStart(arg0: Marker) {
                Log.d(
                    "System out",
                    "onMarkerDragStart..." + arg0.position.latitude + "..." + arg0.position.longitude
                )
            }

            override fun onMarkerDragEnd(arg0: Marker) {

                Log.d(
                    "System out",
                    "onMarkerDragEnd..." + arg0.position.latitude + "..." + arg0.position.longitude
                )
            }

            override fun onMarkerDrag(arg0: Marker) {
                Log.i("System out", "onMarkerDrag...")
            }
        })

        marker?.hideInfoWindow()
        googleMap.uiSettings.isMapToolbarEnabled = false

        binding.data?.userCoordinates =
            Coordinates(mLastLocation!!.longitude, mLastLocation!!.latitude)
        centerMapOnLocation(mLastLocation, "")

        googleMap.setOnMapClickListener { point ->
            marker?.hideInfoWindow()
            println(point.latitude.toString() + "---" + point.longitude)
        }
    }

    private fun generateMarker() {
        val inflatedView = View.inflate(requireContext(), R.layout.custom_group_marker, null)
        val iconImage = inflatedView.findViewById<ImageView>(R.id.markerImage)
        iconImage.setImageDrawable(ContextCompat.getDrawable(requireContext(), icon))
        val iconGenerator = IconGenerator(requireContext())
        iconGenerator.setBackground(null)
        iconGenerator.setContentView(inflatedView)
        // icon2 = BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon())
        binding.imageView3.setImageBitmap(iconGenerator.makeIcon())
        binding.imageView3.visibility = View.VISIBLE
    }

    override fun onCameraMove() {
        val position: CameraPosition = mMap.cameraPosition
        Log.d(
            "onCameraMove",
            String.format(
                "lat: %f, lon: %f, zoom: %f, tilt: %f",
                position.target.latitude,
                position.target.longitude, position.zoom,
                position.tilt
            )
        )
        binding.data?.coordinates = Coordinates(position.target.longitude, position.target.latitude)

    }

    fun animateMarker(destination: LatLng, marker: Marker?) {
        if (marker != null) {
            val startPosition = marker.position
            val endPosition = LatLng(destination.latitude, destination.longitude)
            val startRotation = marker.rotation
            val latLngInterpolator: LatLngInterpolator = LatLngInterpolator.LinearFixed()
            val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
            valueAnimator.duration = 1000 // duration 1 second
            valueAnimator.interpolator = LinearInterpolator()
            valueAnimator.addUpdateListener { animation ->
                try {
                    val v = animation.animatedFraction
                    val newPosition: LatLng? =
                        latLngInterpolator.interpolate(v, startPosition, endPosition)
                    marker.setPosition(newPosition!!)
                    marker.rotation = computeRotation(v, startRotation, 1f)
                } catch (ex: java.lang.Exception) {
                    // I don't care atm..
                }
            }
            valueAnimator.start()
        }
    }

    override fun onMarkerClick(p0: Marker): Boolean {
//        show=true
        //if title is empty the window not display

        return false
    }
    var locationCentered = false
    //
    @Override
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        binding.data?.userCoordinates =
            Coordinates(mLastLocation!!.longitude, mLastLocation!!.latitude)
        if (!locationCentered) {
            centerMapOnLocation(mLastLocation, "")
            locationCentered = true
        }
    }
    //Request Permission
    private fun requestFineLocation(act: Activity?, code: Int) {
        ActivityCompat.requestPermissions(
            act!!,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            code
        )
    }

    fun centerMapOnLocation(location: Location?, title: String?) {
        if (myLocationMarker != null) {
            myLocationMarker?.remove()
        }
        if(location !=null) {
            val userLocation = LatLng(location.latitude, location.longitude)
            myLocationMarker = mMap.addMarker(
                MarkerOptions().position(userLocation).title(title).icon(
                    BitmapDescriptorFactory.fromResource(
                        R.drawable.current_location
                    )
                )
            )

            myLocationMarker?.hideInfoWindow()
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 20f), 500, null)
        }

    }

    private fun computeRotation(fraction: Float, start: Float, end: Float): Float {
        val normalizeEnd = end - start // rotate start to 0
        val normalizedEndAbs = (normalizeEnd + 360) % 360
        val direction: Float =
            if (normalizedEndAbs > 180) (-1).toFloat() else 1.toFloat() // -1 = anticlockwise, 1 = clockwise
        val rotation: Float
        rotation = if (direction > 0) {
            normalizedEndAbs
        } else {
            normalizedEndAbs - 360
        }
        val result = fraction * rotation + start
        return (result + 360) % 360
    }

    private interface LatLngInterpolator {
        fun interpolate(fraction: Float, a: LatLng?, b: LatLng?): LatLng?
        class LinearFixed : LatLngInterpolator {


            override fun interpolate(fraction: Float, a: LatLng?, b: LatLng?): LatLng? {
                val lat = (b!!.latitude - a!!.latitude) * fraction + a.latitude
                var lngDelta = b.longitude - a.longitude
                // Take the shortest path across the 180th meridian.
                if (Math.abs(lngDelta) > 180) {
                    lngDelta -= Math.signum(lngDelta) * 360
                }
                val lng = lngDelta * fraction + a.longitude
                return LatLng(lat, lng)
            }
        }
    }

}