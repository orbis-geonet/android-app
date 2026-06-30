package com.orbis.orbis.ui.placesModule.views

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.GroundOverlayOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.maps.android.ui.IconGenerator
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseFragment
import com.orbis.orbis.databinding.FragmentPlacePostBinding
import com.orbis.orbis.helpers.PlaceIcon
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.ui.app.GlideApp
import com.orbis.orbis.ui.groupsModule.views.GroupDetailsActivity
import com.orbis.orbis.ui.placesModule.adapter.GroupIconAdapter
import com.orbis.orbis.ui.placesModule.adapter.PlacePostsAdapter
import com.orbis.orbis.utils.Utils
import com.orbis.orbis.utils.ViewUtils
import de.hdodenhof.circleimageview.CircleImageView
import java.util.*
import kotlin.math.cos


class PlacePostFragment(private val placeDetails: PlaceDetails?) : BaseFragment(),
    GroupIconAdapter.GroupIconCardInteraction {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    lateinit var binding: FragmentPlacePostBinding
    private var mGroupIconAdapter: GroupIconAdapter? = null
    private var mAdapter: PlacePostsAdapter? = null
    private lateinit var mMap: GoogleMap
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_place_post, container, false)
        binding.data = placeDetails
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(0, 0, 0, navBarInsets.bottom)
            insets
        }
        return binding.root
    }


    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->
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
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(), R.raw.map_style
                )
            )
            if (!success) {
                Log.e("Style", "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("Style", "Can't find style. Error: ", e)
        }

        val icon2 = BitmapFactory.decodeResource(
            context?.getResources(),
            ViewUtils.getPlaceIcon(placeDetails?.type!!)!!
        )
        val storage =
            Firebase.storage.getReference(
                Constants.GROUP_PHOTO_STORAGE + Utils.getImageUrl200(
                    placeDetails.dominantGroup!!.imageName
                )
            )
        GlideApp.with(requireContext()).load(storage).listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                val inflatedView =
                    View.inflate(requireContext(), R.layout.custom_group_marker, null)
                val iconGenerator = IconGenerator(requireContext())
                val imageView = inflatedView.findViewById<ImageView>(R.id.markerImage)
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        PlaceIcon.getIconByType(placeDetails.type)
                    )
                )
                iconGenerator.setBackground(null)
                iconGenerator.setContentView(inflatedView)
                val PLACE_LOCATION =
                    LatLng(placeDetails.coordinates?.latitude!!, placeDetails.coordinates.longitude)
                val groundOverlayOptions = GroundOverlayOptions()
                groundOverlayOptions.image(BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon()))
                    .anchor(0.5F, 0.5F)
                // Specifying the position of the  GroundOverlay
                groundOverlayOptions.position(PLACE_LOCATION, 400F)
                groundOverlayOptions.bearing(0f)
                groundOverlayOptions.clickable(true)
                // Adding the GroundOverlay to the GoogleMap
                val groundOverlay = mMap.addGroundOverlay(groundOverlayOptions)
                val baseZoomLevel = 15f
                val adjustedZoomLevel = adjustZoomLevelForLatitude(PLACE_LOCATION, baseZoomLevel)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(PLACE_LOCATION, adjustedZoomLevel))
                //disable navigation option
                googleMap.uiSettings.isMapToolbarEnabled = false

                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                requireActivity().runOnUiThread {
                    val inflatedView =
                        View.inflate(requireContext(), R.layout.custom_map_places, null)
                    val iconImage = inflatedView.findViewById<CircleImageView>(R.id.placeImage)
                    iconImage.setImageDrawable(resource)
                    if (placeDetails.dominantGroup.strokeColorHex.isNotEmpty())
                        iconImage.borderColor =
                            Color.parseColor(placeDetails.dominantGroup.strokeColorHex)
                    val iconGenerator = IconGenerator(requireContext())
                    iconGenerator.setBackground(null)
                    iconGenerator.setContentView(inflatedView)
                    val PLACE_LOCATION = LatLng(
                        placeDetails.coordinates?.latitude!!,
                        placeDetails.coordinates.longitude
                    )
                    val groundOverlayOptions = GroundOverlayOptions()
                    groundOverlayOptions.image(BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon()))
                        .anchor(0.5F, 0.5F)
                    // Specifying the position of the  GroundOverlay
                    groundOverlayOptions.position(PLACE_LOCATION, 400F)
                    groundOverlayOptions.bearing(0f)
                    groundOverlayOptions.clickable(true)
                    // Adding the GroundOverlay to the GoogleMap
                    val groundOverlay = mMap.addGroundOverlay(groundOverlayOptions)
                    val baseZoomLevel = 15f
                    val adjustedZoomLevel = adjustZoomLevelForLatitude(PLACE_LOCATION, baseZoomLevel)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(PLACE_LOCATION, adjustedZoomLevel))
                    //disable navigation option
                    googleMap.uiSettings.isMapToolbarEnabled = false
                }

                return true
            }

        }).submit()


    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

companion object {

}


    override fun onIconClicked(groupDetails: GroupDetails) {
        val intent = Intent(requireContext(), GroupDetailsActivity::class.java)
        intent.putExtra("data", groupDetails)
        intent.putExtra("location", Constants.location)
        startActivity(intent)
    }

    fun adjustZoomLevelForLatitude(latLng: LatLng, baseZoomLevel: Float): Float {
        // Calculate the adjustment factor based on the latitude
        val adjustmentFactor = cos(Math.toRadians(latLng.latitude)).toFloat()
        return baseZoomLevel - (1 - adjustmentFactor) * baseZoomLevel / 2
    }
}