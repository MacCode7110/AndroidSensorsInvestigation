package com.example.androidsensorsinvestigation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var geofenceVisitStore: GeofenceVisitLocation

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Geofence error: $errorMessage")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition != Geofence.GEOFENCE_TRANSITION_DWELL) {
            return
        }

        geofencingEvent.triggeringGeofences?.forEach { geofence ->
            when (geofence.requestId) {
                "campus_center" -> {
                    geofenceVisitStore.incrementCampusCenter()
                    Toast.makeText(
                        context,
                        "You have been inside the Campus Center geofence for 5 seconds, incrementing counter",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.i(TAG, "User dwelled in Campus Center geofence")
                }
                "unity_hall" -> {
                    geofenceVisitStore.incrementUnityHall()
                    Toast.makeText(
                        context,
                        "You have been inside the Unity Hall geofence for 5 seconds, incrementing counter",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.i(TAG, "User dwelled in Unity Hall geofence")
                }
                else -> Log.w(TAG, "Unknown geofence: ${geofence.requestId}")
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}

