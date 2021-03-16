package com.mobicomp_notificationapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.mobicomp_notificationapp.db.AppDB
import com.mobicomp_notificationapp.db.LocationTable

const val LOCATION_REQUEST_CODE = 137
const val GEOFENCE_LOCATION_REQUEST_CODE = 1337
const val CAMERA_ZOOM_LEVEL = 13f
const val GEOFENCE_RADIUS = 500
const val GEOFENCE_ID = "REMINDER_GEOFENCE_ID"
const val GEOFENCE_EXPIRATION = 10 * 24 * 60 * 60 * 1000
const val GEOFENCE_DWELL_DELAY = 10 * 1000

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var locations: List<LocationTable>
    private var fenceCircle: Circle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geofencingClient = LocationServices.getGeofencingClient(this)

        applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putString("locationSelected", "none").apply()
        val uid = applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).getInt("UserID", -1)
        AsyncTask.execute {
            val db = Room.databaseBuilder(
                    applicationContext,
                    AppDB::class.java,
                    getString(R.string.dbFileName)
            ).fallbackToDestructiveMigration().build()
            locations = db.locationDAO().getLocationsByUser(uid)
            db.close()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true

        if (!checkLocationPermission()) {
            getPermissions()
        }
        else {
            // Was this double-check completely useless?
            if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION)
                                    != PackageManager.PERMISSION_GRANTED) {
                getPermissions()
            }

            var latlng: LatLng
            for (location in locations) {
                Log.d("MobiComp_MARKERS", "Current location: ${location.id}, ${location.name}, ${location.latitude}, ${location.longitude}")
                latlng = LatLng(location.latitude, location.longitude)
                //this@MapsActivity.runOnUiThread(java.lang.Runnable {
                val marker = googleMap.addMarker(
                        MarkerOptions()
                                .position(latlng)
                                .title(location.name)
                                .snippet("${location.latitude}, ${location.longitude}")
                )
                marker.tag = location.id.toString()
                //})
            }

            map.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener {
                if (it != null) {
                    with(map) {
                        val latlng = LatLng(it.latitude, it.longitude)
                        moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, CAMERA_ZOOM_LEVEL))
                    }
                }
                else {
                    with(map) {
                        moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(65.05775, 25.46909), CAMERA_ZOOM_LEVEL)
                        )
                    }
                }
            }
        }
        onLongPress(map)
        infoWindowListener(map)
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun getPermissions() {
        val permissions = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                LOCATION_REQUEST_CODE
        )
    }

    private fun onLongPress(map: GoogleMap) {
        map.setOnMapLongClickListener { latlng ->
            val marker = map.addMarker(
                    MarkerOptions()
                            .position(latlng)
                            .title("New location")
                            .snippet("<press to save or select>")
            )
            marker.tag = "new_marker"
            marker.showInfoWindow()
            fenceCircle?.remove()
            fenceCircle = map.addCircle(CircleOptions().center(latlng)
                    .strokeColor(Color.argb(70, 175, 175, 175))
                    .fillColor(Color.argb(40, 175, 175, 175))
                    .radius(GEOFENCE_RADIUS.toDouble())
            )
            // map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, CAMERA_ZOOM_LEVEL))

            //
        }
    }

    private fun infoWindowListener(map: GoogleMap) {
        map.setOnInfoWindowClickListener {

            if (it.tag == "new_marker") {
                val markerLat = it.position.latitude
                val markerLng = it.position.longitude
                val uid = applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).getInt("UserID", -1)
                var location_id = -1L

                val builder = AlertDialog.Builder(this)
                builder.setTitle("Save location?")
                builder.setMessage("You can save current location for later use or select an unsaved location for single use")
                val txtName = EditText(this)
                txtName.setInputType(InputType.TYPE_CLASS_TEXT)
                txtName.hint = "Location name"
                builder.setView(txtName)
                builder.setPositiveButton("Save") { _, _ ->
                    AsyncTask.execute {
                        val db = Room.databaseBuilder(
                                applicationContext,
                                AppDB::class.java,
                                getString(R.string.dbFileName)
                        ).fallbackToDestructiveMigration().build()
                        val locationNames = db.locationDAO().getNames(uid)
                        val locationName = txtName.getText().toString()
                        if (locationName != "") {
                            if (locationName !in locationNames) {
                                val locationItem = LocationTable(
                                        id = null,
                                        profile_id = uid,
                                        name = locationName,
                                        latitude = markerLat,
                                        longitude = markerLng
                                )
                                location_id = db.locationDAO().insert(locationItem)
                                this@MapsActivity.runOnUiThread(java.lang.Runnable {
                                    it.title = txtName.text.toString()
                                    it.snippet = "${it.position.latitude}, ${it.position.longitude}"
                                    it.tag = location_id.toString()
                                    it.showInfoWindow()
                                })
                            } else {
                                // Name already in use
                                this@MapsActivity.runOnUiThread(java.lang.Runnable {
                                    val toast = Toast.makeText(applicationContext, "$locationName is already in use, please select another name!", Toast.LENGTH_SHORT)
                                    toast.show()
                                })
                            }
                        } else {
                            // Name cannot be empty
                            this@MapsActivity.runOnUiThread(java.lang.Runnable {
                                val toast = Toast.makeText(applicationContext, "Location name required, please try again!", Toast.LENGTH_SHORT)
                                toast.show()
                            })
                        }

                        db.close()
                    }
                }
                builder.setNeutralButton("Select") {dialog, _ ->
                    applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putString("locationSelected", "raw").apply()
                    applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putString("locationName", it.title).apply()
                    applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putFloat("locationLatitude", it.position.latitude.toFloat()).apply()
                    applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putFloat("locationLongitude", it.position.latitude.toFloat()).apply()
                    applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putInt("locationId", 0).apply()
                    dialog.dismiss()
                    finish()
                }
                builder.setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
                builder.show()
            }
            else {
                val location_id = it.tag.toString().toInt()

                val builder = AlertDialog.Builder(this)
                builder.setTitle(it.title)
                builder.setMessage("Select location?")
                builder.setPositiveButton("Select") { _, _ ->
                    applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putString("locationSelected", "db").apply()
                    applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putString("locationName", it.title).apply()
                    applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putFloat("locationLatitude", it.position.latitude.toFloat()).apply()
                    applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putFloat("locationLongitude", it.position.latitude.toFloat()).apply()
                    applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putInt("locationId", it.tag.toString().toInt()).apply()
                    finish()
                }
                builder.setNeutralButton("Delete") {dialog, _ ->
                    AsyncTask.execute {
                        val db = Room.databaseBuilder(
                                applicationContext,
                                AppDB::class.java,
                                getString(R.string.dbFileName)
                        ).fallbackToDestructiveMigration().build()
                        db.locationDAO().delete(location_id)
                        db.close()
                    }
                    // update marker
                    it.remove()

                    dialog.dismiss()
                }
                builder.setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
                builder.show()
            }
        }

        map.setOnMarkerClickListener {
            //it.showInfoWindow()
            fenceCircle?.remove()
            fenceCircle = map.addCircle(CircleOptions().center(it.position)
                    .strokeColor(Color.argb(70, 175, 175, 175))
                    .fillColor(Color.argb(40, 175, 175, 175))
                    .radius(GEOFENCE_RADIUS.toDouble())
            )
            false
        }

    }
}