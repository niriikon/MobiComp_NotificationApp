package com.mobicomp_notificationapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceReceiver: BroadcastReceiver() {
    var key: Int = -1
    lateinit var message: String
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)
            val geofencingTransition = geofencingEvent.geofenceTransition

            if (geofencingTransition == Geofence.GEOFENCE_TRANSITION_ENTER
                    || geofencingTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
                if (intent != null) {
                    key = intent.getIntExtra("key", -1)
                    message = intent.getStringExtra("msg")!!
                }
                MapsActivity.showNotification(
                    context.applicationContext, message)

            }

            val triggeringGeofences = geofencingEvent.triggeringGeofences
            MapsActivity.removeGeofences(context, triggeringGeofences)
        }
    }
}