package com.example.androidsensorsinvestigation.ui.main

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.androidsensorsinvestigation.R
import com.example.androidsensorsinvestigation.viewmodel.ActivityType
import com.example.androidsensorsinvestigation.viewmodel.MainViewModel
import com.google.android.gms.location.DetectedActivity

@Preview(showBackground = true)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val activity by viewModel.activityType.collectAsState()
    val locationEnabled by viewModel.locationEnabled.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(all = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DataText(viewModel)

        Spacer(modifier = Modifier.height(30.dp))

        if(!locationEnabled) {
            RequestLocationPermission {
                viewModel.setLocationEnabled(true)
                viewModel.startActivityTracking()
            }
        }

        // MapView or placeholder taking up flexible space
        Column(modifier = Modifier.weight(1f)) {
            if(locationEnabled) {
                MapView(viewModel = viewModel)
            } else {
                Spacer(modifier = Modifier.fillMaxSize())
            }

            ActivityView(activity = activity)
        }
        
        if (viewModel.isDebug) {
            DebugActivityControls(viewModel)
        }
    }
}

@Composable
fun DataText(
    viewModel: MainViewModel
) {
    val visitsCC by viewModel.visitsCC.collectAsState()
    val visitsUnity by viewModel.visitsUnity.collectAsState()
    val steps by viewModel.steps.collectAsState()

    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        Text(text = "Visits to Campus Center geoFence: $visitsCC", fontSize = 16.sp)
        Text(text = "Visits to Unity Hall geoFence: $visitsUnity", fontSize = 16.sp)
        Text(text = "Steps taken since app started: $steps", fontSize = 16.sp)
    }
}

@Composable
fun ActivityView(
    activity: ActivityType
) {
    val activityImage = when(activity) {
        ActivityType.IN_VEHICLE -> painterResource(R.drawable.in_vehicle)
        ActivityType.RUNNING -> painterResource(R.drawable.running)
        ActivityType.STILL -> painterResource(R.drawable.still)
        ActivityType.WALKING -> painterResource(R.drawable.walking)
    }
    val activityText = when (activity) {
        ActivityType.IN_VEHICLE -> "You are Driving"
        ActivityType.RUNNING -> "You are Running"
        ActivityType.STILL -> "You are Still"
        ActivityType.WALKING -> "You are Walking"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(vertical = 8.dp)
    ) {
        Image(
            painter = activityImage,
            contentDescription = "Activity Image",
            modifier = Modifier.size(300.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = activityText,
            fontSize = 20.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun RequestLocationPermission(
    onPermissionGranted: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val activityRecognitionGranted = permissions[Manifest.permission.ACTIVITY_RECOGNITION] ?: false

        if ((fineLocationGranted || coarseLocationGranted) && activityRecognitionGranted) {
            onPermissionGranted()
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        )
    }
}

@Composable
fun DebugActivityControls(
    viewModel: MainViewModel
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Text("Debug Simulation", fontSize = 14.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { viewModel.debugSetActivity(DetectedActivity.WALKING) }, modifier = Modifier.weight(1f).padding(2.dp)) {
                Text("Walk", fontSize = 10.sp)
            }
            Button(onClick = { viewModel.debugSetActivity(DetectedActivity.RUNNING) }, modifier = Modifier.weight(1f).padding(2.dp)) {
                Text("Run", fontSize = 10.sp)
            }
            Button(onClick = { viewModel.debugSetActivity(DetectedActivity.IN_VEHICLE) }, modifier = Modifier.weight(1f).padding(2.dp)) {
                Text("Drive", fontSize = 10.sp)
            }
            Button(onClick = { viewModel.debugSetActivity(DetectedActivity.STILL) }, modifier = Modifier.weight(1f).padding(2.dp)) {
                Text("Still", fontSize = 10.sp)
            }
        }
    }
}
