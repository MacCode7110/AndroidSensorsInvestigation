package com.example.androidsensorsinvestigation.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidsensorsinvestigation.ui.main.ActivityTransitionReceiver
import com.example.androidsensorsinvestigation.ui.main.activityRepos.ActivityRecognitionRepository
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.example.androidsensorsinvestigation.ui.main.activityRepos.FakeActivityRepo

@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: Application,
    private val activityRepo : ActivityRecognitionRepository
) : ViewModel() {
    
    val isDebug = activityRepo is FakeActivityRepo

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
        activityRepo.stopTracking()
    }

    val activityType = activityRepo.activityFlow
        .map { detected ->
            when (detected) {
                DetectedActivity.RUNNING -> ActivityType.RUNNING
                DetectedActivity.WALKING -> ActivityType.WALKING
                DetectedActivity.IN_VEHICLE -> ActivityType.IN_VEHICLE
                else -> ActivityType.STILL
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            ActivityType.STILL
        )

    fun startActivityTracking(){
        activityRepo.startTracking()
    }

    fun debugSetActivity(activity: Int) {
        if (isDebug) {
            val intent = Intent(application, ActivityTransitionReceiver::class.java).apply {
                action = ActivityTransitionReceiver.ACTION_DEBUG_ACTIVITY
                putExtra(ActivityTransitionReceiver.EXTRA_ACTIVITY_TYPE, activity)
            }
            application.sendBroadcast(intent)
        }
    }
}

enum class ActivityType {
    IN_VEHICLE,
    RUNNING,
    STILL,
    WALKING
}
