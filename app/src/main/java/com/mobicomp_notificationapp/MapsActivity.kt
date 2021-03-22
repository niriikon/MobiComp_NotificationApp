package com.mobicomp_notificationapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.google.android.gms.location.*
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
import kotlin.random.Random

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
        } else {

            if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED) {
                getPermissions()
                return
            }
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
                    //val toast = Toast.makeText(applicationContext, "My location is ${it.latitude}, ${it.longitude}", Toast.LENGTH_LONG)
                    //toast.show()
                }
                else {

                    with(map) {
                        moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(65.05775, 25.46909), CAMERA_ZOOM_LEVEL)
                        )
                    }
                    //val toast = Toast.makeText(applicationContext, "Cannot get lastLocation", Toast.LENGTH_LONG)
                    //toast.show()
                }
            }

        onLongPress(map)
        infoWindowListener(map)

        /*
        val origin = intent.getStringExtra("origin")
        if (origin == "profile") {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Set location")
            builder.setMessage("Use a mock location or reset to using actual location")
            builder.setPositiveButton("Mock") { _, _ ->
                // Close dialog and allow to select location
            }
            builder.setNeutralButton("Reset") { dialog, _ ->
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    getPermissions()
                }
                fusedLocationClient.setMockMode(false)
                dialog.dismiss()
                finish()
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
                finish()
            }
            builder.show()
        }

         */
    }

    private fun checkLocationPermission(): Boolean {
        /*
        return ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

         */
        return ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
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

            //val origin = intent.getStringExtra("origin")

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
                    /*
                    if (origin == "profile") {
                        if (!checkLocationPermission()) {
                            getPermissions()
                        }
                        else {
                            if (ContextCompat.checkSelfPermission(
                                            this,
                                            Manifest.permission.ACCESS_FINE_LOCATION)
                                    != PackageManager.PERMISSION_GRANTED &&
                                    ContextCompat.checkSelfPermission(
                                            this,
                                            Manifest.permission.ACCESS_COARSE_LOCATION)
                                    != PackageManager.PERMISSION_GRANTED) {
                                getPermissions()
                            }
                            else {
                                fusedLocationClient.setMockMode(true)
                                //val locMan = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                                val tmpLocation = Location("mock")
                                tmpLocation.latitude = it.position.latitude
                                tmpLocation.longitude = it.position.longitude
                                tmpLocation.time = System.currentTimeMillis()
                                tmpLocation.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                                val task = fusedLocationClient.setMockLocation(tmpLocation)
                                if (task.isSuccessful) {
                                    val toast = Toast.makeText(applicationContext, "Mock location set successfully!", Toast.LENGTH_LONG)
                                    toast.show()
                                } else {
                                    val toast = Toast.makeText(applicationContext, "Mock location setting failed!", Toast.LENGTH_LONG)
                                    toast.show()
                                }
                            }
                        }
                    }

                     */
                    //else {
                        applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putString("locationSelected", "raw").apply()
                        applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putString("locationName", it.title).apply()
                        applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putFloat("locationLatitude", it.position.latitude.toFloat()).apply()
                        applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putFloat("locationLongitude", it.position.latitude.toFloat()).apply()
                        applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putInt("locationId", 0).apply()
                        createGeoFence(it.position, -1, "Check a reminder at your current location!", geofencingClient)
                    //}
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
                    /*
                    if (origin == "profile") {
                        if (!checkLocationPermission()) {
                            getPermissions()
                        }
                        else {
                            if (ContextCompat.checkSelfPermission(
                                            this,
                                            Manifest.permission.ACCESS_FINE_LOCATION)
                                    != PackageManager.PERMISSION_GRANTED &&
                                    ContextCompat.checkSelfPermission(
                                            this,
                                            Manifest.permission.ACCESS_COARSE_LOCATION)
                                    != PackageManager.PERMISSION_GRANTED) {
                                getPermissions()
                            }
                            else {
                                fusedLocationClient.setMockMode(true)
                                //val locMan = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                                val tmpLocation = Location("mock")
                                tmpLocation.setLatitude(it.position.latitude)
                                tmpLocation.setLongitude(it.position.longitude)
                                tmpLocation.setTime(System.currentTimeMillis())
                                tmpLocation.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                                val task = fusedLocationClient.setMockLocation(tmpLocation)
                                if (task.isSuccessful) {
                                    val toast = Toast.makeText(applicationContext, "Mock location set successfully!", Toast.LENGTH_LONG)
                                    toast.show()
                                } else {
                                    val toast = Toast.makeText(applicationContext, "Mock location setting failed!", Toast.LENGTH_LONG)
                                    toast.show()
                                }
                            }
                        }
                    }

                     */
                    //else {
                        applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putString("locationSelected", "db").apply()
                        applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putString("locationName", it.title).apply()
                        applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putFloat("locationLatitude", it.position.latitude.toFloat()).apply()
                        applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putFloat("locationLongitude", it.position.latitude.toFloat()).apply()
                        applicationContext.getSharedPreferences(getString(R.string.sharedPreference), Context.MODE_PRIVATE).edit().putInt("locationId", it.tag.toString().toInt()).apply()
                        createGeoFence(it.position, it.tag.toString().toInt(), "Check a reminder at your current location!", geofencingClient)
                    //}
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

    private fun createGeoFence(latlng: LatLng, key: Int, message: String, geoClient: GeofencingClient) {
        val geofence = Geofence.Builder()
                .setRequestId(GEOFENCE_ID)
                .setCircularRegion(latlng.latitude, latlng.longitude, GEOFENCE_RADIUS.toFloat())
                .setExpirationDuration(GEOFENCE_EXPIRATION.toLong())
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL)
                .setLoiteringDelay(GEOFENCE_DWELL_DELAY)
                .build()

        val geofenceRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

        val intent = Intent(this, GeofenceReceiver::class.java)
        intent.putExtra("key", key)
        intent.putExtra("msg", message)
        val pendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                            applicationContext,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                        GEOFENCE_LOCATION_REQUEST_CODE
                    )
                }
            else {
                geoClient.addGeofences(geofenceRequest, pendingIntent)
            }
        }
        else {
            geoClient.addGeofences(geofenceRequest, pendingIntent)
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == GEOFENCE_LOCATION_REQUEST_CODE) {
            if (permissions.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        "This app needs background location to be enabled",
                        Toast.LENGTH_LONG).show()
                getPermissions()
            }
        }
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED || grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                if (ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    return
                }
                map.isMyLocationEnabled = true
                onMapReady(map)
            }
            else {
                Toast.makeText(this,
                        "This app needs background location to be enabled",
                        Toast.LENGTH_LONG).show()
                getPermissions()
            }
        }
    }

    private fun scheduleJob() {
        val componentName = ComponentName(this, ReminderJobService::class.java)
        val info = JobInfo.Builder(321, componentName)
                .setRequiresCharging(false)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setPeriodic(15 * 60 * 1000)
                .build()

        val scheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
        val resultCode = scheduler.schedule(info)
        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            Log.d("MobiComp_JOBS", "Job scheduled")
        } else {
            Log.d("MobiComp_JOBS", "Job scheduling failed")
            scheduleJob()
        }
    }

    private fun cancelJob() {
        val scheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
        scheduler.cancel(321)
        Log.d("MobiComp_JOBS", "Job cancelled")
    }

    companion object {
        fun removeGeofences(context: Context, triggeringGeofenceList: MutableList<Geofence>) {
            val geofenceIdList = mutableListOf<String>()
            for (entry in triggeringGeofenceList) {
                geofenceIdList.add(entry.requestId)
            }
            LocationServices.getGeofencingClient(context).removeGeofences(geofenceIdList)
        }

        fun showNotification(context: Context, message: String) {
            val CHANNEL_ID = "REMINDER_NOTIFICATION_CHANNEL"
            var notificationId = 1234
            notificationId += Random(notificationId).nextInt(1, 30)

            val notificationBuilder = NotificationCompat.Builder(context.applicationContext)
                    .setSmallIcon(R.drawable.calendar_notification)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.app_name),
                        NotificationManager.IMPORTANCE_DEFAULT
                ).apply{description = context.getString(R.string.app_name)}

                notificationManager.createNotificationChannel(channel)
            }
            notificationManager.notify(notificationId, notificationBuilder.build())
        }
    }
}