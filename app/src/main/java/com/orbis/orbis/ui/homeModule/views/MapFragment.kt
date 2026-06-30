package com.orbis.orbis.ui.homeModule.views

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.CancelableCallback
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.GroundOverlay
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.orbis.orbis.R
import com.orbis.orbis.databinding.FragmentMapBinding
import com.orbis.orbis.helpers.CoordinatesUtil
import com.orbis.orbis.helpers.CoordinatesUtil.getZoomLevel
import com.orbis.orbis.helpers.CoordinatesUtil.pixelsToDegrees
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.listeners.OnPlaceCreate
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.group.FeedPost
import com.orbis.orbis.models.message.ConversationModel
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.models.place.PolygonPlaceDetails
import com.orbis.orbis.ui.ProfileModule.views.ProfileActivity
import com.orbis.orbis.ui.ProfileModule.views.ProfileViewModel
import com.orbis.orbis.ui.authModule.views.AuthActivity
import com.orbis.orbis.ui.groupsModule.viewModel.GroupViewModel
import com.orbis.orbis.ui.groupsModule.views.GroupListActivity
import com.orbis.orbis.ui.homeModule.manager.MapOverlayManager
import com.orbis.orbis.ui.homeModule.manager.MapPlacesPagerController
import com.orbis.orbis.ui.homeModule.manager.MapZoomController
import com.orbis.orbis.ui.homeModule.manager.PolygonRenderer
import com.orbis.orbis.ui.messageModule.views.MessageActivity
import com.orbis.orbis.ui.newsFeedModule.views.NewsFeedActivity
import com.orbis.orbis.ui.placesModule.PlaceViewModel
import com.orbis.orbis.ui.placesModule.views.PlaceActivity
import com.orbis.orbis.utils.PermissionUtil
import com.orbis.orbis.utils.Utils
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.math.max


@AndroidEntryPoint
class MapFragment : Fragment(),
    GoogleMap.OnGroundOverlayClickListener, GoogleMap.OnPolygonClickListener,
    OnPlaceCreate, OnCameraMoveStartedListener,
    GoogleMap.OnCameraMoveListener, GoogleMap.OnCameraIdleListener,
    GoogleMap.OnMarkerClickListener {


    var loadedOverlay = false
    var lastZoomLevel = 0f

    private val DEFAULT_ZOOM = 11.5f
    private val NewYork = LatLng(40.7128, -74.0060)
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var locationManager: LocationManager
    private var mMap: GoogleMap? = null
    private var mLastLocation: Location? = null
    private var lastLocation: LatLng? = null
    private var myLocationMarker: Marker? = null
    private var placeSheetCheckIn = false
    private var allClicksDisabled: Boolean = false
    private var showingCommunitiesPopup: Boolean = false
    private var lastOverlayUpdatedTime: Long = 0
    var overlayClickedLoading = false
    var overlayClicked = false
    var circle: Circle? = null
    private var restrictedBounds: LatLngBounds? = null
    private var minAllowedZoom: Float? = null
    private var isCameraRestricted = false
    private lateinit var checkLocationPermission: ActivityResultLauncher<Array<String>>
    private lateinit var placeViewModel: PlaceViewModel
    lateinit var profileViewModel: ProfileViewModel
    private lateinit var groupViewModel: GroupViewModel

    lateinit var mapFragment: SupportMapFragment
    lateinit var binding: FragmentMapBinding
    private lateinit var groupSheet: GroupListBottomSheet
    var checkInCompleted = false
    var polygonFocusingFirstTime = true
    var mapPage = 0
    var lastCitySelected = ""
    private lateinit var backCallback: OnBackPressedCallback
    private lateinit var overlayManager: MapOverlayManager
    private lateinit var citySelector: CitySelector
    private lateinit var polygonRenderer: PolygonRenderer
    private lateinit var zoomController: MapZoomController
    private lateinit var pagerController: MapPlacesPagerController

    @SuppressLint("MissingPermission", "ClickableViewAccessibility")
    private val callback = OnMapReadyCallback { googleMap ->
        mMap = googleMap

        overlayManager = MapOverlayManager(googleMap)
        polygonRenderer = PolygonRenderer(
            context = requireContext(),
            map = googleMap,
            overlayManager = overlayManager,
            isInFocusMode = { overlayClicked },
            areClicksDisabled = { allClicksDisabled },
            onLoadingDone = { binding.loading = false },
            onGroundOverlayClick = ::onGroundOverlayClick,
            onMarkerClick = ::onMarkerClick,
            onPopupDataNeeded = { groupKey ->
                groupViewModel.getGroupAndMembersByKey(groupKey)
            }
        )
        citySelector = CitySelector(
            context = requireContext(),
            binding = binding,
            sharedPreferences = sharedPreferences,
            mMap = mMap,
            defaultZoom = DEFAULT_ZOOM,
            hasLocationPermission = ::hasLocationPermission,
            hideKeyboard = ::hideKeyboard,
            listener = object : CitySelector.CitySelectionListener {

                override fun onCitySelected(cityName: String, location: LatLng) {
                    lastCitySelected = cityName
                    placeViewModel.locationSharedOrSelected = true
                    loadedOverlay = false

                    overlayManager.removeAll()
                    placeViewModel.setLastCitySelected(location)

                    enableMapDragging()
                    centerMapOnLocation(placeViewModel.lastLocationSelected, "", false)

                    initOrUpdateGroupSheet(placeViewModel.lastLocationSelected!!, cityName)
                    citySelector.saveLastCitySelectedOnSharedPreferences(location, cityName)
                    groupSheet.show()
                }

                override fun onSelectionStart() {
                    overlayManager.hideAllPopupMarkers()
                    binding.myLocationFab.isVisible = false
                }

                override fun onSelectionCancelled() {
                    binding.myLocationFab.isVisible = true
                }
            }
        )
        citySelector.setupCityBoxClickListeners()

        mMap?.setOnCameraIdleListener(this)
        mMap?.setOnCameraMoveListener(this)
        mMap?.setOnCameraMoveStartedListener(this)
        mMap?.uiSettings?.isTiltGesturesEnabled = false
        // customize map style
        googleMap.setMapStyle(
            MapStyleOptions.loadRawResourceStyle(
                requireContext(),
                R.raw.map_style
            )
        )

        googleMap.setOnMarkerClickListener(this)
        googleMap.setOnGroundOverlayClickListener(this)
        googleMap.setOnPolygonClickListener(this)
        //disable navigation option
        googleMap.uiSettings.isMapToolbarEnabled = false
        googleMap.uiSettings.isCompassEnabled = false
        googleMap.uiSettings.isRotateGesturesEnabled = false

        val savedCity = getSavedCity()
        if (!hasLocationPermission())
            handleNoPermission(savedCity)
        else
            handleWithPermission(savedCity)
    }

    data class SavedCity(
        val lat: Double,
        val lon: Double,
        val name: String
    )

    private fun getSavedCity(): SavedCity? {
        val lat = sharedPreferences.getFloat("selected_city_latitude", -1f)
        val lon = sharedPreferences.getFloat("selected_city_longitude", -1f)
        val name = sharedPreferences.getString("selected_city_name", "") ?: ""

        return if (lat != -1f && lon != -1f) {
            SavedCity(lat.toDouble(), lon.toDouble(), name)
        } else null
    }

    private fun handleNoPermission(savedCity: SavedCity?) {
        if (savedCity != null) {
            mMap?.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(savedCity.lat, savedCity.lon),
                    DEFAULT_ZOOM
                )
            )
            binding.loading = true
            placeViewModel.findPolygonPlacesForMap(savedCity.lat, savedCity.lon)
            notifyCitySelected(savedCity)
            initOrUpdateGroupSheet(Location("SavedCity").apply {
                latitude = savedCity.lat; longitude = savedCity.lon
            }, savedCity.name)
        } else {
            moveToDefaultCity()
            if (!placeViewModel.locationSharedOrSelected) {
                blockMapDragging()
                citySelector.hide()
                citySelector.showNoLocation()
            }
        }

        requestFineLocation()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun handleWithPermission(savedCity: SavedCity?) {
        placeViewModel.locationSharedOrSelected = true

        if (savedCity != null) {
            val location = Location("SavedCity").apply {
                latitude = savedCity.lat
                longitude = savedCity.lon
            }

            centerMapOnLocation(location, "saved city", false)
            initOrUpdateGroupSheet(location, savedCity.name)
        } else {
            handleRequestLocation()
        }
    }

    private fun initOrUpdateGroupSheet(location: Location, city: String?) {
        val newCity = city ?: binding.cityTextLocView.text.toString()

        if (!::groupSheet.isInitialized) {
            groupSheet = GroupListBottomSheet.newInstance(
                location = location,
                userKey = null,
                city = newCity,
                onExpanded = { onBottomSheetExpand() },
                onCollapsed = { onBottomSheetCollapsed() },
                onProcess = { onBottomSheetProcess() },
                onHidden = { onBottomSheetHide() }
            )
            groupSheet.fabToAnimate = binding.myLocationFab
            childFragmentManager.beginTransaction()
                .replace(R.id.bottom_sheet_container, groupSheet)
                .commitNow()
            binding.bottomSheetContainer.visibility = View.VISIBLE
        } else {
            groupSheet.updateLocation(location, newCity)
        }
    }

    private fun moveToDefaultCity() {
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(NewYork, DEFAULT_ZOOM))

        allClicksDisabled = true

        if (!loadedOverlay) {
            binding.loading = true
            placeViewModel.findPolygonPlacesForMap(NewYork.latitude, NewYork.longitude)
        }

        val cityName = "New York"
        binding.locationTextView.text = cityName
        binding.locationBox.isVisible = true

        initOrUpdateGroupSheet(Location("DefaultCity").apply {
            latitude = NewYork.latitude; longitude = NewYork.longitude
        }, cityName)
    }

    private fun notifyCitySelected(city: SavedCity) {
        placeViewModel.locationSharedOrSelected = true
        placeViewModel.setLastCitySelected(LatLng(city.lat, city.lon))
    }

    private fun saveLastCitySelectedOnSharedPreferences() {
        val loc = placeViewModel.lastLocationSelected ?: return

        citySelector.saveLastCitySelectedOnSharedPreferences(
            LatLng(loc.latitude, loc.longitude),
            lastCitySelected
        )
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            mLastLocation = location
            Constants.location = location

            if (!hascenteredOnLocation && getSavedCity() == null) {
                hascenteredOnLocation = true
                centerMapOnLocation(location, "first fix", false)
            }

            val userLocation = LatLng(location.latitude, location.longitude)
            updateUserMarker(userLocation)

            if (lastCitySelected.isEmpty())
                saveLocationPref(location)
        }
    }

    private fun setBottomUiVisibility(
        showLogo: Boolean,
        showGroupsFab: Boolean,
        showNewsFeed: Boolean,
        showGradient: Boolean,
        showLocationFab: Boolean = false,
        showCommunities: Boolean = true
    ) {
        binding.logoCardview.isVisible = showLogo
        binding.groupsFab.isVisible = showGroupsFab
        binding.newsFeedFab.isVisible = showNewsFeed
        binding.linearLayout7.isVisible = showGradient
        binding.myLocationFab.isVisible = showLocationFab
        binding.communitiesNearbyButton.isVisible = showCommunities
        binding.communitiesUpButton.isVisible = showCommunities
    }

    private fun onBottomSheetExpand() {
        setBottomUiVisibility(
            showLogo = false,
            showGroupsFab = false,
            showNewsFeed = false,
            showGradient = false,
            showCommunities = false
        )
    }

    private fun onBottomSheetCollapsed() {
        setBottomUiVisibility(
            showLogo = true,
            showGroupsFab = true,
            showNewsFeed = true,
            showGradient = true,
            showCommunities = false,
            showLocationFab = !overlayClicked //&& canShowLocationFab(),
        )
    }

    private fun onBottomSheetHide() {
        setBottomUiVisibility(
            showLogo = true,
            showGroupsFab = true,
            showNewsFeed = true,
            showGradient = true,
            showLocationFab = !overlayClicked,//&& canShowLocationFab(),
            showCommunities = !overlayClicked //&& showingCommunitiesPopup
        )
    }

    private fun onBottomSheetProcess() {}

    private fun updateUserMarker(userLocation: LatLng) {
        myLocationMarker?.remove()

        val markerOptions = MarkerOptions()
            .position(userLocation)
            .title(getString(R.string.your_location))
            .icon(
                bitmapDescriptorFromVector(
                    requireContext(),
                    R.drawable.ic_current_location
                )
            )

        myLocationMarker = mMap?.addMarker(markerOptions)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_map, container, false)
        sharedPreferences = requireContext().getSharedPreferences("MAP", Context.MODE_PRIVATE)
        placeViewModel = ViewModelProvider(this)[PlaceViewModel::class.java]
        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        groupViewModel = ViewModelProvider(this)[GroupViewModel::class.java]
        groupViewModel.groupNMembers.observe(viewLifecycleOwner) {
            polygonRenderer.resolvePopupWithGroupDetails(it.first, it.second)
        }
        profileViewModel.updateFcmToken()
        placeViewModel.buildCities(resources.openRawResource(R.raw.cities))
        val id: String? =
            requireActivity().intent.getStringExtra("id")
        if (!id.isNullOrEmpty()) {
            placeViewModel.getPost(id)
        }

        profileViewModel.unreadCount.observe(viewLifecycleOwner) {
            val counter = binding.notificationIcon.cartBadge
            counter.text =
                it.notifications.toString()
            if (it.notifications == 0) {
                counter.visibility = View.GONE
            } else if (it.notifications > 99) {
                counter.text = "99"
                counter.visibility = View.VISIBLE
            } else {
                counter.visibility = View.VISIBLE
            }

        }
        LocationServices.getFusedLocationProviderClient(requireActivity())
        mapFragment = childFragmentManager.findFragmentById(R.id.map2) as SupportMapFragment
        locationManager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        mapFragment.getMapAsync(callback)
        checkLocationPermission = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (!granted) return@registerForActivityResult

            enableMapDragging()
            placeViewModel.locationSharedOrSelected = true

            startLocationUpdates()
            initLastKnownLocation()

            if (getSavedCity() == null) {
                mLastLocation?.let { location ->
                    centerMapOnLocation(location, "shared location", false)
                }
            }
        }

        binding.loginBtn.setOnClickListener {
            val intent = Intent(requireContext(), AuthActivity::class.java)
            intent.putExtra("goToLogin", true)
            startActivity(intent)
        }

        profileViewModel.tokenError.observe(viewLifecycleOwner) {
            if (it) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.something_wromg),
                    Toast.LENGTH_LONG
                ).show()
                if (PrefManager(requireContext()).isSocialLogin()) {
                    FirebaseAuth.getInstance().signOut()
                    profileViewModel.deleteTokenToServer()

                } else {
                    profileViewModel.deleteTokenToServer()
                }
            }
        }
        profileViewModel.logout.observe(viewLifecycleOwner) {
            if (it) {
                val intent = Intent(requireContext(), AuthActivity::class.java)
                startActivity(intent)
            }

        }

        profileViewModel.myProfile.observe(viewLifecycleOwner) {
            PrefManager(requireContext()).saveUserKey(it.userKey!!)
            PrefManager(requireContext()).saveUserName(it.displayName)
            Constants.IS_PRIVATE = it.accountPrivate

            if (!it.imageName.isNullOrEmpty()) {
                Utils.downloadProfilePicture(it.imageName!!, binding.userIv)
            } else if (it.providerImageUrl.isNotEmpty()) {
                val uri = it.providerImageUrl.toUri()
                if (it.providerImageUrl.contains("fbsbx")) {
                    val asid = uri.getQueryParameter("asid")
                    val load = "https://graph.facebook.com/$asid/picture?width=300&height=300"
                    Picasso.get().load(load).into(binding.userIv)
                    Constants.userImage = load
                } else {
                    Picasso.get().load(it.providerImageUrl).into(binding.userIv)
                    Constants.userImage = it.providerImageUrl
                }
            } else {
                binding.userIv.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_user
                    )
                )
            }
        }
        placeViewModel.mapPolygonPlaces.observe(viewLifecycleOwner) {
            loadedOverlay = true
            if (pendingRegionChange != null)
                lastLocation =
                    LatLng(pendingRegionChange!!.latitude, pendingRegionChange!!.longitude)

            if (it.isEmpty() || (it.isNotEmpty() && it.size != 20))
                binding.loading = false

            placeViewModel.isLoading.postValue(true)

            if (!it.isNullOrEmpty() && pendingRegionChange != null)
                pendingRegionChange = null

            if (it.size == 20) {
                mapPage++

                val location = lastLocation?.let {
                    LatLng(it.latitude, it.longitude)
                } ?: NewYork

                placeViewModel.findPolygonPlacesForMap(
                    location.latitude,
                    location.longitude,
                    mapPage
                )
            }
            for (place in it) {
                if (!overlayManager.containsPlace(place)) {
                    overlayManager.addPlaceToMemory(place)
                    overlayManager.markAsWillBeOverlay(place)
                    lifecycleScope.launch {
                        polygonRenderer.createGroundOverlayAndPolygon(place)
                    }
                }
            }
            displayPolygonsAndOverlays()
        }
        placeViewModel.polygonFocusedPlaces.observe(viewLifecycleOwner) {
            if (overlayClicked && it.isNotEmpty()) {
                pagerController.loadPlaces(it, placeViewModel.changedFocus)

                // Set the markers for current focus
                if (placeViewModel.newFocusedChanges.value == null) {
                    polygonRenderer.focusPolygonMarkers.keys.forEach { it.remove() }
                    for (place in it)
                        drawFocusedPlaceMarker(place, it.size == 1)
                }

                // Move the pager to start focus
                if (polygonFocusingFirstTime)
                    pagerController.adjustViewPagerHeightAt(0)

                binding.mapPlacesPager.mapPlacesPager2.visibility = View.VISIBLE
                polygonFocusingFirstTime = false
            }
        }
        placeViewModel.newFocusedChanges.observe(viewLifecycleOwner) { focusedChanges ->
            if (focusedChanges != null) {
                val oldPlace = focusedChanges.first
                val newFocus = focusedChanges.second
                polygonRenderer.focusPolygonMarkers.entries.forEach { entry ->
                    if (entry.value.placeKey == oldPlace.placeKey || entry.value.placeKey == newFocus.placeKey)
                        entry.key.remove()
                }
                //Is not unique because at least we have 2
                drawFocusedPlaceMarker(oldPlace, false)
                drawFocusedPlaceMarker(newFocus, false)

                pagerController.moveToCurrentFocus()
            }
        }
        placeViewModel.newAddedPlace.observe(viewLifecycleOwner) {
            Log.d("placeCreated", "data retrieved for place: " + it.name)

            //restart map view
            mapPage = 0
            placeViewModel.completeCheckInProgressBar()
            placeViewModel.findPolygonPlacesForMap(
                lastLocation!!.latitude,
                lastLocation!!.longitude
            )
            enableMapDragging()
            Handler(Looper.getMainLooper()).postDelayed({
                checkInCompleted = true
                hideCheckInLoading()
            }, 100)

            //find exactly modified/added polygon
            placeViewModel.findPolygonPlace(it.placeKey)
        }

        placeViewModel.locationInfo.observe(viewLifecycleOwner) { locationInfo ->
            if (overlayClicked || !placeViewModel.locationSharedOrSelected) return@observe

            val zoom = mMap?.cameraPosition?.zoom ?: DEFAULT_ZOOM

            citySelector.show()
            binding.locationBox.isVisible = false

            when {
                zoom > 11 -> {
                    val neighborhood = locationInfo?.suburb ?: locationInfo?.municipality
                    val city = locationInfo?.city
                        ?: locationInfo?.stateDistrict
                        ?: locationInfo?.country
                        ?: mMap?.cameraPosition?.target?.let { getString(R.string.choose_city_title) }
                        ?: getString(R.string.choose_city_title)

                    binding.cityTextLocView.text = city

                    if (!neighborhood.isNullOrEmpty()) {
                        binding.locationBox.isVisible = true
                        binding.locationTextView.text = neighborhood
                    } else if (!city.isNullOrEmpty()) {
                        // Fallback: show city in locationBox too so it's never empty
                        binding.locationBox.isVisible = true
                        binding.locationTextView.text = city
                    }
                }

                zoom in 7.0..<11.0 -> {
                    binding.cityTextLocView.text = locationInfo?.country
                        ?: locationInfo?.city
                                ?: getString(R.string.choose_city_title)
                    binding.myLocationFab.extend()
                }

                else -> {
                    binding.cityTextLocView.text = getString(R.string.choose_city_title)
                    binding.myLocationFab.extend()
                }
            }
        }

        placeViewModel.allCities.observe(viewLifecycleOwner) {
            citySelector.loadCities(it)
        }

        return binding.root
    }

    private val renderScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val semaphore = Semaphore(3)
    private fun displayPolygonsAndOverlays() {
        if (overlayClicked) return
        val cameraTarget = mMap?.cameraPosition?.target ?: return

        lifecycleScope.launch {
            val centralPolygon = overlayManager.updateInBoundsAsync(
                lastUpdatedTime = lastOverlayUpdatedTime,
                cameraTarget = cameraTarget,
                onShouldCreate = { place ->
                    overlayManager.markAsWillBeOverlay(place)
                    renderScope.launch {
                        semaphore.withPermit {
                            polygonRenderer.createGroundOverlayAndPolygon(place)
                        }
                    }
                },
                onShouldRemove = overlayManager::removeOverlay
            )

            if (centralPolygon != null) {
                polygonRenderer.requestPopupIfNeeded(centralPolygon)
                overlayManager.showOnlyPopupFor(centralPolygon)
            } else {
                binding.root.postDelayed({ overlayManager.hideAllPopupMarkers() }, 600)
            }
        }
    }

    private fun initViews() {
        if (hasLocationPermission()) {
            val permissionUtil = PermissionUtil()
            checkLocationPermission.launch(permissionUtil.locationPermissions)
        } else {
            mapFragment.getMapAsync(callback)
        }

        zoomController = MapZoomController(
            mapProvider = { mMap },
            leftSeekBar = binding.leftSeekbar,
            rightSeekBar = binding.rightSeekbar
        )
        zoomController.init()


        binding.notificationIcon.notificationIcon.setOnClickListener { showNotificationSheet() }
        binding.userIv.setOnClickListener {
            if (!PrefManager(requireContext()).getIdToken().isNullOrEmpty()) {
                val profileIntent = Intent(requireContext(), ProfileActivity::class.java)
                profileIntent.putExtra("userKey", PrefManager(requireContext()).getUserKey())
                startActivity(profileIntent)
            }
        }
        binding.messageIcon.messageIcon.setOnClickListener {
            val messageIntent = Intent(requireContext(), MessageActivity::class.java)
            messageIntent.putExtra("fromMap", true)
            startActivity(messageIntent)
        }
        binding.groupsFab.setOnClickListener {
            //binding.myLocationFab.visibility = View.GONE
            binding.communitiesNearbyButton.visibility = View.GONE
            binding.communitiesUpButton.visibility = View.GONE
            groupSheet.toggle()

        }
        binding.communitiesNearbyButton.setOnClickListener {
            groupSheet.show()
            binding.communitiesNearbyButton.visibility = View.GONE
            binding.communitiesUpButton.visibility = View.GONE
        }
        binding.communitiesUpButton.setOnClickListener {
            groupSheet.show()
            groupSheet.expand()
            binding.communitiesNearbyButton.visibility = View.GONE
            binding.communitiesUpButton.visibility = View.GONE
            binding.myLocationFab.visibility = View.GONE
        }
        backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

                when {
                    isBottomSheetVisible() -> {
                        groupSheet.hide()
                    }

                    isPolygonFocused -> {
                        onMapPlacesBack()
                    }

                    else -> {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            backCallback
        )
        binding.newsFeedFab.setOnClickListener {
            val newsIntent = Intent(requireContext(), NewsFeedActivity::class.java)
            val watchingLocation = Location("OrbisWatchingLocation")
            watchingLocation.longitude = mMap?.cameraPosition!!.target.longitude
            watchingLocation.latitude = mMap?.cameraPosition!!.target.latitude
            newsIntent.putExtra("location", watchingLocation)
            newsIntent.putExtra("city", binding.cityTextLocView.text.toString())
            startActivity(newsIntent)
        }
        binding.root.postDelayed({
            if (!overlayClicked) {
                showingCommunitiesPopup = true
                binding.communitiesNearbyButton.visibility = View.VISIBLE
                binding.communitiesUpButton.visibility = View.VISIBLE
                if (::groupSheet.isInitialized){
                    groupSheet.hide()
                    groupSheet.show()
                }
            }
        }, 350)
        binding.myLocationFab.background.alpha = 255 / 4
        binding.myLocationFab.icon?.alpha = 255
        binding.myLocationFab.shrink()
        binding.myLocationFab.isVisible = true
        binding.myLocationFab.setOnClickListener {
            binding.myLocationFab.shrink()

            if (!hasLocationPermission())
                requestFineLocation()

            val target = mLastLocation ?: run {
                val savedCity = getSavedCity()
                Location("fallback").apply {
                    latitude = savedCity?.lat ?: NewYork.latitude
                    longitude = savedCity?.lon ?: NewYork.longitude
                }
            }

            centerMapOnLocation(target, "", false)
        }

        pagerController = MapPlacesPagerController(
            context = requireContext(),
            binding = binding,
            placeViewModel = placeViewModel,
            mLastLocation = { mLastLocation },
            onBackPressed = ::onMapPlacesBack,
        )
        pagerController.setup()

        // Zoom into users location j
        locationManager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private fun isBottomSheetVisible(): Boolean {
        return groupSheet.getState() == BottomSheetBehavior.STATE_EXPANDED ||
                groupSheet.getState() == BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun showCheckInPlaceSheet() {
        lateinit var location: Location
        val watchingLocation = Location("OrbisWatchingLocation")
        watchingLocation.longitude = mMap?.cameraPosition!!.target.longitude
        watchingLocation.latitude = mMap?.cameraPosition!!.target.latitude

        onMapPlacesBack()

        location = watchingLocation

        Constants.location = location
        val checkInPlaceDialogFragment = CheckInPlaceDialogFragment.newInstance(location)
        checkInPlaceDialogFragment.listener = this
        checkInPlaceDialogFragment.show(
            childFragmentManager,
            CheckInPlaceDialogFragment::class.java.simpleName
        )
    }

    private fun blockMapDragging() {
        zoomController.disable()
        mMap?.uiSettings?.isScrollGesturesEnabled = false
        mMap?.uiSettings?.isZoomGesturesEnabled = false
    }

    private fun enableMapDragging() {
        zoomController.enable()
        mMap?.uiSettings?.isScrollGesturesEnabled = true
        mMap?.uiSettings?.isZoomGesturesEnabled = true
    }

    private fun showNotificationSheet() {
        val notificationDialogFragment = NotificationDialogFragment()
        notificationDialogFragment.show(
            childFragmentManager,
            NotificationDialogFragment::class.java.simpleName
        )
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    fun centerMapOnLocation(location: Location?, title: String?, displayMarker: Boolean) {
        if (location == null) return

        updateLocationState(location)
        updateMarker(location, title, displayMarker)
        animateCamera(location)

        allClicksDisabled = false

        if (!loadedOverlay) {
            binding.loading = true
            placeViewModel.findPolygonPlacesForMap(location.latitude, location.longitude)
            displayPolygonsAndOverlays()
        }
    }

    private fun updateLocationState(location: Location) {
        mLastLocation = location
        Constants.location = location
        saveLocationPref(location)

        if (lastLocation == null) {
            lastLocation = LatLng(location.latitude, location.longitude)
        }
    }

    private fun updateMarker(location: Location, title: String?, displayMarker: Boolean) {
        myLocationMarker?.remove()

        if (!displayMarker) return

        val latLng = LatLng(location.latitude, location.longitude)

        myLocationMarker = mMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(title)
                .icon(bitmapDescriptorFromVector(requireContext(), R.drawable.ic_current_location))
        )

        myLocationMarker?.hideInfoWindow()
    }

    private fun animateCamera(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        mMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM),
            object : CancelableCallback {
                override fun onFinish() {
                    initOrUpdateGroupSheet(location, null)
                }

                override fun onCancel() {}
            })
    }

    fun saveLocationPref(location: Location) {
        placeViewModel.locationSharedOrSelected = true
        placeViewModel.setLastCitySelected(location)
        saveLastCitySelectedOnSharedPreferences()
    }

    private fun distance(latLng: LatLng): Float =
        lastLocation?.let { CoordinatesUtil.computeDistance(it, latLng).toFloat() } ?: 0f

    private fun drawFocusedPlaceMarker(place: PolygonPlaceDetails, unique: Boolean) {
        polygonRenderer.drawFocusedPlaceMarker(
            place = place,
            unique = unique,
            zoomLevel = lastZoomLevel,
            focusedPlaces = placeViewModel.polygonFocusedPlaces.value ?: emptyList()
        )
    }

    private fun requestFineLocation() {
        val permissionUtil = PermissionUtil()
        checkLocationPermission.launch(permissionUtil.locationPermissions)
    }

    override fun onPolygonClick(clickedPolygon: Polygon) {
        overlayManager.getOverlayForPolygon(clickedPolygon)?.let {
            onGroundOverlayClick(it)
        }
    }

    override fun onGroundOverlayClick(p0: GroundOverlay) {
        overlayClickedLoading = true
        overlayClicked = true
        placeSheetCheckIn = false
        circle?.remove()
        citySelector.hide()
        val place = overlayManager.getPlaceForOverlay(p0) ?: return
        overlayManager.hideAllPopupMarkers()
        polygonPlaceGroundOverlayClicked(place, p0)
    }

    private var isPolygonFocused = false

    fun polygonPlaceGroundOverlayClicked(placeDetails: PolygonPlaceDetails, p0: GroundOverlay) {
        isPolygonFocused = true

        enterFocusState(placeDetails, p0)
        animateCameraToFocus(placeDetails)
        showFocusUI(placeDetails)
    }

    private fun enterFocusState(placeDetails: PolygonPlaceDetails, overlay: GroundOverlay) {
        overlayManager.getPolygonForOverlay(overlay)?.isClickable = false
        overlay.isClickable = false

        placeViewModel.setFocusedListPolygon(placeDetails.places)
        overlayManager.enterFocusMode(placeDetails)
    }

    private fun animateCameraToFocus(placeDetails: PolygonPlaceDetails) {
        circle = polygonRenderer.drawEnclosingCircle(placeDetails)
        circle?.isVisible = true

        val center = circle!!.center
        val radius = circle!!.radius

        lastZoomLevel = getZoomLevel(radius, center, requireContext().resources.displayMetrics)

        restrictedBounds = getBoundsFromCircle(center, radius)
        minAllowedZoom = lastZoomLevel
        isCameraRestricted = true

        val cameraTarget = LatLng(
            center.latitude - pixelsToDegrees(50, lastZoomLevel, center),
            center.longitude
        )

        mMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(cameraTarget, lastZoomLevel),
            1000,
            null
        )

        enableMapDragging()

        zoomController.moveSeekbarsUp()
    }

    private fun showFocusUI(placeDetails: PolygonPlaceDetails) {
        pagerController.showPolygonGroupCards(placeDetails)
        binding.mapPolygonGroupCards.root.visibility = View.VISIBLE
        binding.mapPlacesPager.root.visibility = View.VISIBLE
        citySelector.hide()

        binding.communitiesUpButton.visibility = View.GONE
        binding.communitiesNearbyButton.visibility = View.GONE
        binding.myLocationFab.visibility = View.GONE
        groupSheet.hide()
    }

    val messages: HashMap<String, Boolean> = HashMap()
    private var conversationListener: ListenerRegistration? = null
    override fun onResume() {
        super.onResume()
        handleNotificationIntent()
        profileViewModel.getMyProfile()
        profileViewModel.getUnreadCount()

        //catch cheked-ins from other screens
        try {
            if (Constants.lastCheckinPlace != null && Constants.lastcheckInPolygonCoordinateKey != null && !placeSheetCheckIn) {
                val newPlace = Constants.lastCheckinPlace
                val chekInCoordkey = Constants.lastcheckInPolygonCoordinateKey
                Constants.lastCheckinPlace = null
                Constants.lastcheckInPolygonCoordinateKey = null

                reRenderPolygons()
                removeFocusedPlaceView()
                onCameraIdle()

                loadCheckIn(newPlace!!, chekInCoordkey!!)
            }
        } catch (e: Exception) {
            Log.e("checkInError", "Error handling check-in intent: ${e.message}")
        }

        val db = Firebase.firestore

        val query = db.collection("conversation")
            .whereArrayContains("participants", PrefManager(requireContext()).getUserKey()!!)
        Log.d("refreshingList", "conversationList")
        conversationListener = query.addSnapshotListener { value, _ ->
            messages.clear()
            for (document in value?.documents!!) {
                val data = document.toObject(ConversationModel::class.java)

                if (data?.lastMessage != null && !data.lastMessage?.isRead!! && data.lastMessage?.senderId!! != PrefManager(
                        requireContext()
                    ).getUserKey()!!
                ) {
                    Log.e("timestampCheck", data.timestamp.toString())
                    messages[data.lastMessage?.conversationId!!] = true
                }


            }
            val counter = binding.messageIcon.cartBadge
            counter.text =
                messages.size.toString()
            if (messages.size == 0) {
                counter.visibility = View.GONE
            } else if (messages.size > 99) {
                counter.text = "99"
                counter.visibility = View.VISIBLE
            } else {
                counter.visibility = View.VISIBLE
            }
        }

        if (placeViewModel.locationSharedOrSelected) {
            citySelector.hideNoLocation()
        }
        val isGuest = PrefManager(requireContext()).getIdToken().isNullOrEmpty()
        binding.photoItemIv.isVisible = isGuest
        binding.loginBtn.isVisible = isGuest
        binding.userIv.isVisible = !isGuest
        binding.messageIcon.root.isVisible = !isGuest
        binding.notificationIcon.root.isVisible = !isGuest
        if (PrefManager(requireContext()).getIdToken().isNullOrEmpty()) {
            binding.logoFab.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_logo
                )
            )
            binding.logoCardview.setOnClickListener {
                val intent = Intent(requireContext(), AuthActivity::class.java)
                intent.putExtra("goToLogin", true)
                startActivity(intent)
            }

        } else {
            binding.logoFab.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_checkin_button
                )
            )

            binding.logoCardview.setOnClickListener {
                placeSheetCheckIn = true
                showCheckInPlaceSheet()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        conversationListener?.remove()
        conversationListener = null
    }

    private var checkInJob: Job? = null
    fun startCheckInLoading() {
        checkInJob?.cancel()
        binding.checkInLoadingContainer.visibility = View.VISIBLE
        binding.checkInLoadingExtra.alpha = 0f
        binding.checkInProgressBar.progress = 0
        placeViewModel.restartCheckInProgressBar()

        blockMapDragging()
        placeViewModel.clearMapPolygonPlaces()
        binding.loading = false
        overlayManager.removeAll()

        checkInJob = lifecycleScope.launch {
            placeViewModel.checkInProgress.collect { progress ->
                if (!checkInCompleted) {
                    overlayManager.removeAll()
                    ObjectAnimator.ofInt(
                        binding.checkInProgressBar,
                        "progress",
                        binding.checkInProgressBar.progress,
                        progress
                    ).apply {
                        duration = 300L
                        start()
                    }
                    if (progress in 85..99) {
                        binding.checkInLoadingExtra.animate()
                            .alpha(1f)
                            .setDuration(250)
                            .start()
                    }
                }
            }
        }

        lifecycleScope.launch {
            placeViewModel.startCheckInLoading()
        }
    }

    private fun hideCheckInLoading() {
        binding.checkInLoadingExtra.alpha = 0f
        binding.checkInLoadingContainer.visibility = View.GONE
        binding.checkInProgressBar.progress = 0
        placeViewModel.restartCheckInProgressBar()
    }

    override fun onPlaceCreate(placeDetails: PlaceDetails) {
        Log.d(
            "placeCreated",
            placeDetails.name + " group: " + placeDetails.dominantGroup?.name!! + " placeKey: " + placeDetails.placeKey + " by menu"
        )
        loadCheckIn(placeDetails, placeDetails.checkInPolygonCoordinateKey)
    }

    override fun onPostCreate(feedPost: FeedPost) {
        feedPost.place?.let { p ->
            loadCheckIn(p, feedPost.checkInPolygonCoordinateKey)
        }
    }

    fun loadCheckIn(newPlace: PlaceDetails, checkInPolygonCoordinateKey: String) {
        Log.d("placeCreated", "checkInPolygonId: $checkInPolygonCoordinateKey")
        polygonRenderer.lastPlaceCreatedKey = newPlace.placeKey
        lastLocation = newPlace.coordinates!!.toLatLng()
        placeViewModel.askPolygonMapUpdate(checkInPolygonCoordinateKey, newPlace)
        checkInCompleted = false
        startCheckInLoading()
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(requireContext(), PlaceActivity::class.java)
            fun watchingLocation(): Location {
                val watchingLocation = Location("OrbisWatchingLocation")
                watchingLocation.longitude = mMap?.cameraPosition!!.target.longitude
                watchingLocation.latitude = mMap?.cameraPosition!!.target.latitude
                return watchingLocation
            }
            Constants.location = watchingLocation()
            intent.putExtra("data", newPlace)
            startActivity(intent)
            requireActivity().overridePendingTransition(
                R.anim.slide_in_right,
                R.anim.slide_out_right
            )
        }, 250)
    }

    private fun reRenderPolygons() {
        polygonRenderer.clearPolygonMarkers()
        placeViewModel.clearFocusedPolygonPLaces()

        if (!polygonFocusingFirstTime) {
            zoomController.moveSeekbarsDown()
            overlayManager.restoreAllVisibility()
        }
        polygonFocusingFirstTime = true
    }

    override fun onCameraMoveStarted(p0: Int) {
        if (overlayClicked) return

        if (!overlayClickedLoading) {
            reRenderPolygons()
        }
    }

    override fun onCameraMove() {
        binding.locationBox.isVisible = false
        //citySelector.hide()
        citySelector.restoreLayout()
        hideKeyboard()
    }

    fun onMapPlacesBack() {
        isPolygonFocused = false

        if (!overlayClickedLoading) {
            removeFocusedPlaceView()
            enableMapDragging()
            reRenderPolygons()
        }
    }

    private fun removeFocusedPlaceView() {
        circle?.remove()

        isCameraRestricted = false
        restrictedBounds = null
        minAllowedZoom = null
        minAllowedZoom = null

        binding.myLocationFab.isVisible = true
        binding.mapPolygonGroupCards.root.visibility = View.GONE
        binding.mapPlacesPager.root.visibility = View.GONE
        citySelector.show()
        binding.locationBox.isVisible = true
        if (showingCommunitiesPopup) {
            binding.communitiesNearbyButton.visibility = View.VISIBLE
            binding.communitiesUpButton.visibility = View.VISIBLE
        }

        overlayClicked = false
    }

    var pendingRegionChange: LatLng? = null
    override fun onCameraIdle() {
        if (!overlayClicked) {
            displayPolygonsAndOverlays()
        }

        val map = mMap ?: return

        if (isCameraRestricted) {
            val bounds = restrictedBounds
            val minZoom = minAllowedZoom
            if (bounds != null && minZoom != null) {
                val target = map.cameraPosition.target
                val zoom = map.cameraPosition.zoom

                val isOutOfBounds = !bounds.contains(target)
                val isTooZoomedOut = zoom < minZoom

                if (isOutOfBounds || isTooZoomedOut) {
                    val center = bounds.center
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(center, minZoom))
                    return
                }
            }
        }

        if (overlayClicked) {
            binding.myLocationFab.isVisible = false
            overlayClickedLoading = false
            return
        }

        lastOverlayUpdatedTime = System.currentTimeMillis() / 1000
        placeViewModel.getLocationInfo(
            map.cameraPosition.target.latitude,
            map.cameraPosition.target.longitude
        )
        overlayClickedLoading = false

        val dist = distance(map.cameraPosition.target) / 1000
        if (dist > 24.5) {
            pendingRegionChange = LatLng(
                map.cameraPosition.target.latitude,
                map.cameraPosition.target.longitude
            )
            mapPage = 0
            placeViewModel.findPolygonPlacesForMap(
                pendingRegionChange!!.latitude,
                pendingRegionChange!!.longitude
            )
        }

        if (!placeViewModel.locationSharedOrSelected)
            binding.locationBox.isVisible = true
    }

    override fun onMarkerClick(p0: Marker): Boolean {
        val focusedPlace = polygonRenderer.focusPolygonMarkers[p0]
        if (focusedPlace != null) {
            placeViewModel.changeFocusedPlace(focusedPlace)
            return true
        }

        val popupPlace = p0.tag as? PolygonPlaceDetails
        if (popupPlace != null) {
            val overlay = overlayManager.findOverlay(popupPlace) ?: return true
            onGroundOverlayClick(overlay)
            return true
        }

        return true
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        if (!hasLocationPermission()) return

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1L, 1F, locationListener)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1L, 1F, locationListener)
        } catch (e: IllegalArgumentException) {
            Log.w("MapFragment", "Provider unavailable: ", e)
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun initLastKnownLocation() {
        mLastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    }

    fun hideKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        vectorDrawable!!.setBounds(
            0,
            0,
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight
        )
        val bitmap = createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private var hascenteredOnLocation = false

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun handleRequestLocation() {
        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1L, 1F, locationListener)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1L, 1F, locationListener)
        } catch (e: Exception) {
            Log.w("MapFragment", "Provider unavailable: ", e)
        }

        val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (lastKnown != null) {
            hascenteredOnLocation = true
            centerMapOnLocation(lastKnown, "last known location", true)
        }
    }

    private fun getBoundsFromCircle(center: LatLng, radius: Double): LatLngBounds {
        val distance = radius * Math.sqrt(2.0)

        val southwest = SphericalUtil.computeOffset(center, distance, 225.0)
        val northeast = SphericalUtil.computeOffset(center, distance, 45.0)

        return LatLngBounds(southwest, northeast)
    }

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("NOTIF_DEBUG", "Broadcast received, refreshing unread count")
            profileViewModel.getUnreadCount()
        }
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            requireContext(),
            notificationReceiver,
            IntentFilter("com.orbis.NEW_NOTIFICATION"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(notificationReceiver)
    }

    private fun handleNotificationIntent() {
        val intent = requireActivity().intent ?: return
        val type = intent.getStringExtra("type") ?: return
        if (type.isEmpty()) return

        Log.d("NOTIF_DEBUG", "Handling notification intent: type=$type")

        val fromUserKey = intent.getStringExtra("fromUserKey") ?: ""
        val contentKey  = intent.getStringExtra("contentKey")  ?: ""

        intent.removeExtra("type")
        intent.removeExtra("fromUserKey")
        intent.removeExtra("contentKey")

        when (type) {
            "MESSAGE" -> {
                val messageIntent = Intent(requireContext(), MessageActivity::class.java)
                messageIntent.putExtra("conversationId", contentKey)
                messageIntent.putExtra("fromUserKey", fromUserKey)
                startActivity(messageIntent)
            }
            "FOLLOWER", "FOLLOW_REQUEST" -> {
                val profileIntent = Intent(requireContext(), ProfileActivity::class.java)
                profileIntent.putExtra("userKey", fromUserKey)
                startActivity(profileIntent)
            }
            "POST", "CHECK_IN", "COMMENT" -> {
                showNotificationSheet()
            }
            else -> {
                Log.d("NOTIF_DEBUG", "Unknown notification type: $type")
            }
        }
    }

}
