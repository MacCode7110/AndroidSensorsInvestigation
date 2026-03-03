package com.example.androidsensorsinvestigation

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class GeofenceVisitLocation @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("geofence_visits", Context.MODE_PRIVATE)

    private val _visitsCampusCenter = MutableStateFlow(prefs.getInt("campus_center_visits", 0))
    val visitsCampusCenter: StateFlow<Int> = _visitsCampusCenter.asStateFlow()

    private val _visitsUnityHall = MutableStateFlow(prefs.getInt("unity_hall_visits", 0))
    val visitsUnityHall: StateFlow<Int> = _visitsUnityHall.asStateFlow()

    fun incrementCampusCenter() {
        val updated = _visitsCampusCenter.value + 1
        _visitsCampusCenter.value = updated
        prefs.edit {
            putInt("campus_center_visits", updated)
        }
    }

    fun incrementUnityHall() {
        val updated = _visitsUnityHall.value + 1
        _visitsUnityHall.value = updated
        prefs.edit {
            putInt("unity_hall_visits", updated)
        }
    }
}


