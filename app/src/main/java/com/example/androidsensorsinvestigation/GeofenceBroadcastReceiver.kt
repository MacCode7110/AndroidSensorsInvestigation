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
        Log.i(TAG, "GeofenceBroadcastReceiver.onReceive() called")

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null - PendingIntent may be misconfigured")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Geofence error: $errorMessage (code=${geofencingEvent.errorCode})")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val transitionName = when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "ENTER"
            Geofence.GEOFENCE_TRANSITION_DWELL -> "DWELL"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "EXIT"
            else -> "UNKNOWN($geofenceTransition)"
        }

        val triggeringGeofences = geofencingEvent.triggeringGeofences
        val geofenceIds = triggeringGeofences?.joinToString { it.requestId } ?: "none"

        Log.i(TAG, "Transition: $transitionName for geofences: $geofenceIds")

        if (geofenceTransition != Geofence.GEOFENCE_TRANSITION_DWELL) {
            Log.i(TAG, "Ignoring $transitionName transition (only processing DWELL)")
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
                    Log.i(TAG, "User dwelled in Campus Center geofence - counter incremented")
                }
                "unity_hall" -> {
                    geofenceVisitStore.incrementUnityHall()
                    Toast.makeText(
                        context,
                        "You have been inside the Unity Hall geofence for 5 seconds, incrementing counter",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.i(TAG, "User dwelled in Unity Hall geofence - counter incremented")
                }
                else -> Log.w(TAG, "Unknown geofence: ${geofence.requestId}")
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}
