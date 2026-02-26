package com.example.androidsensorsinvestigation.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {
    private val _visitsCC = MutableStateFlow(0)
    val visitsCC: StateFlow<Int> = _visitsCC

    private val _visitsUnity = MutableStateFlow(0)
    val visitsUnity: StateFlow<Int> = _visitsUnity

    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps

    // Functions go here
}

enum class ActivityType {
    IN_VEHICLE,
    RUNNING,
    STILL,
    WALKING
}