package com.getir.patika.foodcouriers

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.AdvancedMarkerOptions
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL


class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var myMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var autocompleteFragment: AutocompleteSupportFragment
    private lateinit var textView: TextView
    private lateinit var userLocation: LatLng
    private lateinit var markerOuterCircle: Circle
    private lateinit var markerInnerCircle: Circle

    // Companion object to access the code that represents location permission throughout the class
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Initialize Places API
        Places.initialize(applicationContext, getString(R.string.google_map_api_key))
        autocompleteFragment =
            supportFragmentManager.findFragmentById(R.id.autoComplete_fragment) as AutocompleteSupportFragment

        // Set the hint Text of autocomplete fragment
        autocompleteFragment.setHint("Where is your location")

        textView = findViewById(R.id.textView)

        // List of values returned after searching
        autocompleteFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
            )
        )

        // Set a listener to handle place selection for autocomplete fragment
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onError(p0: Status) {
                Toast.makeText(this@MapActivity, "Error in Search", Toast.LENGTH_SHORT).show()
            }

            override fun onPlaceSelected(place: Place) {
                // Get the latitude and longitude of the selected place
                val latLng = place.latLng!!

                addMarkerToMap(latLng)

                // Display the address of the selected place at the bottom of the screen
                textView.text = place.address
            }
        })

        // Find the Mapfragment ID
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        // Get a GoogleMap object asynchronously
        mapFragment.getMapAsync(this)

        // Used to access the last known location.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    // Called when the GoogleMap object is ready to be used
    override fun onMapReady(googleMap: GoogleMap) {
        myMap = googleMap

        // Check for location permission
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            // Return if permission is denied
            return
        }

        // Enable location if permission is granted
        myMap.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                // Move the camera to user's location and add a marker
                userLocation = LatLng(it.latitude, it.longitude)

                addMarkerToMap(userLocation)

                getAddressFromLocation(userLocation.latitude, userLocation.longitude)

            }
        }
    }

    // Retrieve the address from given latLong value
    private fun getAddressFromLocation(latitude: Double, longitude: Double) {
        val apiKey = getString(R.string.google_map_api_key)
        val url =
            "https://maps.googleapis.com/maps/api/geocode/json?latlng=$latitude,$longitude&key=$apiKey"

        // Used coroutine for network operations asynchronously
        GlobalScope.launch(Dispatchers.IO) {
            // HTTP Get request to Google Maps API
            val response = URL(url).readText()
            // Parsing address from the response
            val address = parseAddressFromResponse(response)
            // Switching to main thread to update UI components
            withContext(Dispatchers.Main) {
                // Updates the text value of address
                textView.text = address
            }
        }
    }

    // Function to parse address from the response obtained from Google Maps API
    private fun parseAddressFromResponse(response: String): String {
        val jsonObject = JSONObject(response)
        val results = jsonObject.getJSONArray("results")
        if (results.length() > 0) {
            val address = results.getJSONObject(0).getString("formatted_address")
            return address
        }
        return "Address not found"
    }

    // Function to handle the result of a permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Check if the permission request is for location
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // Checks if permission was granted
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                onMapReady(myMap)
        }
    }

    // Function to add marker to the map at the given location with two red backgrounds as circle
    private fun addMarkerToMap(userLocation:LatLng){
        markerOuterCircle = myMap.addCircle(
            CircleOptions()
                .center(userLocation)
                .radius(60.0).strokeWidth(0f)
                .fillColor(Color.parseColor("#F6DEE1"))
        )
        markerInnerCircle = myMap.addCircle(
            CircleOptions()
                .center(userLocation)
                .radius(20.0)
                .strokeWidth(0f)
                .fillColor(Color.parseColor("#F5B1B7"))
        )
        // Add marker to the map at the user's location
        myMap.addMarker(AdvancedMarkerOptions()
            .position(userLocation))

        // Move the camera to the user's location with a zoom level
        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 18f))
    }
}