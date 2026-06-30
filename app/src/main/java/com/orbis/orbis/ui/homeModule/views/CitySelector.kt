package com.orbis.orbis.ui.homeModule.views

import android.animation.ValueAnimator
import android.content.Context
import android.content.SharedPreferences
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.orbis.orbis.databinding.FragmentMapBinding

/**
 * Handles all city-selection UI and logic for MapFragment.
 *
 * Responsibilities:
 *  - Populating the city-picker dropdowns (with/without location permission).
 *  - Expanding/collapsing the city-box widgets.
 *  - Persisting the selected city to SharedPreferences.
 *  - Notifying the caller via [CitySelectionListener] when the user picks a city.
 */
class CitySelector(
    private val context: Context,
    private val binding: FragmentMapBinding,
    private val sharedPreferences: SharedPreferences,
    private val mMap: GoogleMap?,
    private val defaultZoom: Float,
    private val hasLocationPermission: () -> Boolean,
    private val hideKeyboard: () -> Unit,
    private val listener: CitySelectionListener
) {

    interface CitySelectionListener {
        fun onCitySelected(cityName: String, location: LatLng)
        fun onSelectionStart()
        fun onSelectionCancelled()
    }

    private var lastCitySelected = ""

    fun loadCities(allCities: HashMap<String, LatLng>) {
        val cities = allCities.keys.toList()
        val adapter = createCityAdapter(cities)

        setupCommonUI(adapter)
        setupLocationPicker(adapter, cities, allCities)
        setupNoLocationPickerIfNeeded(adapter, cities, allCities)
    }

    fun restoreLayout() {
        if (binding.cityPickerLocView.isVisible) {
            val layoutParams = binding.cityBoxWLocation.layoutParams
            layoutParams.width -= 570
            layoutParams.width = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            binding.cityBoxWLocation.layoutParams = layoutParams

            binding.cityTextLocView.isVisible = true
            binding.cityPickerLocView.isVisible = false
            binding.cityPickerLocView.setText("")

            listener.onSelectionCancelled()
        }
    }

    fun saveLastCitySelectedOnSharedPreferences(loc: LatLng, cityName: String) {
        sharedPreferences.edit().apply {
            putFloat("selected_city_latitude", loc.latitude.toFloat())
            putFloat("selected_city_longitude", loc.longitude.toFloat())
            putString("selected_city_name", cityName)
            apply()
        }
    }

    fun setupCityBoxClickListeners() {
        binding.cityBoxNoLocation.setOnClickListener {
            if (binding.cityTextNoLocView.isVisible) {
                expandBox(
                    box = binding.cityBoxNoLocation,
                    extraWidth = 520,
                    picker = binding.cityPickerNoLocView,
                    textView = binding.cityTextNoLocView
                )
            }
        }

        binding.cityBoxWLocation.setOnClickListener {
            if (binding.cityTextLocView.isVisible) {
                expandBox(
                    box = binding.cityBoxWLocation,
                    extraWidth = 570,
                    picker = binding.cityPickerLocView,
                    textView = binding.cityTextLocView
                )
            }
        }
    }

    fun hide(){
        binding.cityBoxWLocation.isVisible = false
    }
    fun show(){
        binding.cityBoxWLocation.isVisible = true
    }
    fun hideNoLocation(){
        binding.cityBoxNoLocation.isVisible = false
    }
    fun showNoLocation(){
        binding.cityBoxNoLocation.isVisible = true
    }

    private fun createCityAdapter(cities: List<String>): ArrayAdapter<String> =
        ArrayAdapter(context, android.R.layout.simple_list_item_1, cities)

    private fun setupCommonUI(adapter: ArrayAdapter<String>) {
        binding.cityBoxWLocation.isClickable = true
        binding.cityBoxNoLocation.isClickable = true

        binding.cityPickerLocView.apply {
            dropDownVerticalOffset = 22
            setAdapter(adapter)
        }
    }

    private fun setupLocationPicker(
        adapter: ArrayAdapter<String>,
        cities: List<String>,
        allCities: HashMap<String, LatLng>
    ) {
        binding.cityPickerLocView.apply {
            onItemClickListener = createItemClickListener(allCities)
            setOnEditorActionListener(
                createEditorListener(adapter, cities, allCities, { text.toString() })
            )
        }
    }

    private fun setupNoLocationPickerIfNeeded(
        adapter: ArrayAdapter<String>,
        cities: List<String>,
        allCities: HashMap<String, LatLng>
    ) {
        if (hasLocationPermission()) return

        binding.cityPickerNoLocView.apply {
            setAdapter(adapter)

            onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
                val city = parent.getItemAtPosition(position) as String
                handleCitySelection(city, allCities)
                switchToLocationLayout()
            }

            setOnEditorActionListener(
                createEditorListener(adapter, cities, allCities, { text.toString() }) {
                    switchToLocationLayout()
                }
            )
        }
    }

    private fun createItemClickListener(
        allCities: HashMap<String, LatLng>
    ): AdapterView.OnItemClickListener =
        AdapterView.OnItemClickListener { parent, _, position, _ ->
            val city = parent.getItemAtPosition(position) as String
            handleCitySelection(city, allCities)
        }

    private fun createEditorListener(
        adapter: ArrayAdapter<String>,
        cities: List<String>,
        allCities: HashMap<String, LatLng>,
        getText: () -> String,
        extraAction: (() -> Unit)? = null
    ): TextView.OnEditorActionListener =
        TextView.OnEditorActionListener { _, actionId, event ->
            val text = getText()
            val isSubmit =
                actionId == EditorInfo.IME_ACTION_DONE ||
                        (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)

            if (text.isNotEmpty() && isSubmit) {
                val selectedCity = adapter.takeIf { it.count > 0 }?.getItem(0) ?: text
                if (cities.contains(selectedCity)) {
                    handleCitySelection(selectedCity, allCities)
                    extraAction?.invoke()
                    hideKeyboard()
                    return@OnEditorActionListener true
                }
            }
            false
        }

    private fun handleCitySelection(cityName: String, allCities: HashMap<String, LatLng>) {
        val loc = allCities[cityName] ?: return

        if (cityName == lastCitySelected) {
            restoreLayout()
            hideKeyboard()
            return
        }

        lastCitySelected = cityName
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, defaultZoom))

        listener.onCitySelected(cityName, loc)
        restoreLayout()
        hideKeyboard()
    }

    private fun switchToLocationLayout() {
        binding.cityBoxWLocation.isVisible = true
        binding.cityBoxNoLocation.isVisible = false
    }

    private fun expandBox(
        box: View,
        extraWidth: Int,
        picker: View,
        textView: View
    ) {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val maxWidth = screenWidth - box.left
        val expandTo = (box.width + extraWidth).coerceAtMost(maxWidth)

        ValueAnimator.ofInt(box.width, expandTo).apply {
            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                val lp = box.layoutParams
                lp.width = value
                box.layoutParams = lp
            }
            duration = 500
            start()
        }

        textView.isVisible = false
        picker.isVisible = true

        picker.requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(picker, 0)

        listener.onSelectionStart()
    }
}