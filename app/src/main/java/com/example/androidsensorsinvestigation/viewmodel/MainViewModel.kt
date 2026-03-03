package com.example.androidsensorsinvestigation.viewmodel

import android.Manifest
import android.app.Application
import android.app.PendingIntent
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.example.androidsensorsinvestigation.GeofenceBroadcastReceiver
import com.example.androidsensorsinvestigation.GeofenceVisitLocation
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: Application,
    geofenceVisitStore: GeofenceVisitLocation
) : ViewModel() {
    // To use these variables, modify the one starting with _ in this class for storing data, but access it in the UI composables with the other one
    val visitsCC: StateFlow<Int> = geofenceVisitStore.visitsCampusCenter

    val visitsUnity: StateFlow<Int> = geofenceVisitStore.visitsUnityHall

    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps

    private val _locationEnabled = MutableStateFlow(false)
    val locationEnabled: StateFlow<Boolean> = _locationEnabled

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(application)

    private val geofencingClient =
        LocationServices.getGeofencingClient(application)

    private var isGeofenceRegistered = false

    private val _location = MutableStateFlow<LatLng?>(null)
    val location: StateFlow<LatLng?> = _location.asStateFlow()

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(application, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            application,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        5000L
    ).setMinUpdateIntervalMillis(5000L).build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            _location.value = LatLng(location.latitude, location.longitude)
        }
    }

    fun setLocationEnabled(enabled: Boolean) {
        _locationEnabled.value = enabled
    }

    fun startLocationUpdates() {
        if (!hasLocationPermission()) return

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission denied", e)
        }
    }

    fun registerGeofences() {
        if (isGeofenceRegistered || !hasLocationPermission()) return

        val geofenceList = listOf(
            Geofence.Builder()
                .setRequestId("campus_center")
                .setCircularRegion(42.274641, -71.808457, 50f)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL)
                .setLoiteringDelay(5000)
                .build(),

            Geofence.Builder()
                .setRequestId("unity_hall")
                .setCircularRegion(42.273808, -71.806722, 50f)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL)
                .setLoiteringDelay(5000)
                .build()
        )

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL)
            .addGeofences(geofenceList)
            .build()

        try {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener {
                    isGeofenceRegistered = true
                    Log.i(TAG, "Geofences registered successfully")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to register geofences", e)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission error while registering geofences", e)
        }
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}

enum class ActivityType {
    IN_VEHICLE,
    RUNNING,
    STILL,
    WALKING
}