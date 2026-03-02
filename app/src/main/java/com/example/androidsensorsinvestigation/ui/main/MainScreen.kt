package com.example.androidsensorsinvestigation.ui.main

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidsensorsinvestigation.viewmodel.ActivityType
import com.example.androidsensorsinvestigation.viewmodel.MainViewModel
import com.example.androidsensorsinvestigation.R

@Preview(showBackground = true)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val activity by viewModel.activityType.collectAsState()
    val locationEnabled by viewModel.locationEnabled.collectAsState()

    LaunchedEffect(Unit){
        viewModel.startActivityTracking()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(all = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DataText(viewModel)

        Spacer(modifier = Modifier.height(20.dp))

        if(!locationEnabled) {
            RequestLocationPermission {
                viewModel.setLocationEnabled(true)
            }
        }

        if(locationEnabled) {
            MapView(viewModel = viewModel)
        }

        ActivityView(activity = activity)
    }
}

@Composable
fun DataText(
    viewModel: MainViewModel
) {
    val visitsCC by viewModel.visitsCC.collectAsState()
    val visitsUnity by viewModel.visitsUnity.collectAsState()
    val steps by viewModel.steps.collectAsState()

    Text(
        text = "Visits to Campus Center geoFence: $visitsCC",
        fontSize = 18.sp
    )
    Text(
        text = "Visits to Unity Hall geoFence: $visitsUnity",
        fontSize = 18.sp
    )
    Text(
        text = "Steps taken since app started: $steps",
        fontSize = 18.sp
    )
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
    val activityText = when(activity) {
        ActivityType.IN_VEHICLE -> "You are Driving"
        ActivityType.RUNNING -> "You are Running"
        ActivityType.STILL -> "You are Still"
        ActivityType.WALKING -> "You are Walking"
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = activityImage,
            contentDescription = "Activity Image",
            modifier = Modifier
                .height(300.dp)
                .width(300.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = activityText,
            fontSize = 20.sp,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
fun RequestLocationPermission(
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->

        val fineLocationGranted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

        val coarseLocationGranted =
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        val activityRecognitionGranted =
            permissions[Manifest.permission.ACTIVITY_RECOGNITION] ?: false

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