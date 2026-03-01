package com.example.androidsensorsinvestigation.viewmodel

import android.app.Application
import android.os.Looper
import androidx.lifecycle.ViewModel
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
    application: Application
) : ViewModel() {
    // To use these variables, modify the one starting with _ in this class for storing data, but access it in the UI composables with the other one
    private val _visitsCC = MutableStateFlow(0)
    val visitsCC: StateFlow<Int> = _visitsCC

    private val _visitsUnity = MutableStateFlow(0)
    val visitsUnity: StateFlow<Int> = _visitsUnity

    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps

    private val _locationEnabled = MutableStateFlow(false)
    val locationEnabled: StateFlow<Boolean> = _locationEnabled

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(application)

    private val _location = MutableStateFlow<LatLng?>(null)
    val location: StateFlow<LatLng?> = _location.asStateFlow()

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        5000L
    ).setMinUpdateIntervalMillis(5000L)
        .build()

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
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
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