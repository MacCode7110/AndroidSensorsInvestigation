package com.example.androidsensorsinvestigation.viewmodel

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.app.PendingIntent
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.example.androidsensorsinvestigation.GeofenceBroadcastReceiver
import com.example.androidsensorsinvestigation.GeofenceVisitLocation
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingRequest
import androidx.lifecycle.viewModelScope
import com.example.androidsensorsinvestigation.ui.main.ActivityStateHolder
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
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: Application,
    private val activityRepo : ActivityRecognitionRepository,
    geofenceVisitStore: GeofenceVisitLocation
) : ViewModel(), SensorEventListener {

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private var initialStepCount = -1f
    private var isTrackingSteps = false

    // Tracking activity duration
    private var lastActivityType: ActivityType? = null
    private var lastActivityStartTime: Long = System.currentTimeMillis()

    val visitsCC: StateFlow<Int> = geofenceVisitStore.visitsCampusCenter
    val visitsUnity: StateFlow<Int> = geofenceVisitStore.visitsUnityHall

    val isDebug = activityRepo is FakeActivityRepo

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
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
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

    init {
        // Collect activity changes to show duration toasts
        viewModelScope.launch {
            activityType.collect { newActivity ->
                val currentTime = System.currentTimeMillis()
                lastActivityType?.let { prevActivity ->
                    if (prevActivity != newActivity) {
                        val durationSeconds = (currentTime - lastActivityStartTime) / 1000
                        // Only toast if the activity lasted at least 3 seconds to filter out jitter
                        if (durationSeconds >= 3) {
                            val message = "Last activity $prevActivity lasted $durationSeconds seconds"
                            Toast.makeText(application, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                if (lastActivityType != newActivity) {
                    lastActivityType = newActivity
                    lastActivityStartTime = currentTime
                }
            }
        }
    }

    fun startStepTracking() {
        if (isTrackingSteps) return

        var registered = false
        stepDetectorSensor?.let {
            val success = sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
            if (success) registered = true
        }

        stepCounterSensor?.let {
            val success = sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
            if (success) registered = true
        }

        isTrackingSteps = registered
    }

    fun stopStepTracking() {
        if (!isTrackingSteps) return
        sensorManager.unregisterListener(this)
        isTrackingSteps = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                _steps.value += 1
                
                // Immediately transition to walking if we detect a physical step
                if (ActivityStateHolder.activityFlow.value != DetectedActivity.WALKING && 
                    ActivityStateHolder.activityFlow.value != DetectedActivity.RUNNING) {
                    ActivityStateHolder.activityFlow.value = DetectedActivity.WALKING
                }
            }
            Sensor.TYPE_STEP_COUNTER -> {
                val totalStepsSinceReboot = event.values[0]
                if (initialStepCount == -1f) {
                    initialStepCount = totalStepsSinceReboot - _steps.value
                } else {
                    val currentStepsFromCounter = (totalStepsSinceReboot - initialStepCount).toInt()
                    if (currentStepsFromCounter > _steps.value) {
                        _steps.value = currentStepsFromCounter
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

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
        if (isGeofenceRegistered || !hasLocationPermission() || !hasBackgroundLocationPermission()) return

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
                .addOnSuccessListener { isGeofenceRegistered = true }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while registering geofences", e)
        }
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun hasBackgroundLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCleared() {
        super.onCleared()
        stopStepTracking()
        stopLocationUpdates()
        activityRepo.stopTracking()
    }

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
