package com.example.androidsensorsinvestigation.ui.main

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.androidsensorsinvestigation.viewmodel.MainViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

@Composable
fun MapView(
    viewModel: MainViewModel
) {

    val location by viewModel.location.collectAsState()

    val cameraPositionState = rememberCameraPositionState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.startLocationUpdates()
    }

    LaunchedEffect(location) {
        location?.let {
            scope.launch {
                cameraPositionState.move(
                    CameraUpdateFactory.newLatLngZoom(it, 15f)
                )
            }
        }
    }

    GoogleMap(
        modifier = Modifier
            .height(300.dp)
            .width(300.dp),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = true
        )
    )
}