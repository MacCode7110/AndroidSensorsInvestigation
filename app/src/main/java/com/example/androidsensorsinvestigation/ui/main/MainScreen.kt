package com.example.androidsensorsinvestigation.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.androidsensorsinvestigation.viewmodel.ActivityType
import com.example.androidsensorsinvestigation.viewmodel.MainViewModel
import com.example.androidsensorsinvestigation.R

@Preview(showBackground = true)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val activity = ActivityType.RUNNING
    val locationEnabled by viewModel.locationEnabled.collectAsState()
    val context = LocalContext.current

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasBackgroundPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasActivityPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            viewModel.setLocationEnabled(true)
            viewModel.startLocationUpdates()
        }
    }

    // Register geofences when we have both location and background permissions
    LaunchedEffect(hasLocationPermission, hasBackgroundPermission) {
        if (hasLocationPermission && hasBackgroundPermission) {
            viewModel.registerGeofences()
        }
    }

    // Start/Stop step tracking based on activity permission
    DisposableEffect(hasActivityPermission) {
        if (hasActivityPermission) {
            viewModel.startStepTracking()
        }
        onDispose {
            viewModel.stopStepTracking()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(all = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DataText(viewModel)

        Spacer(modifier = Modifier.height(20.dp))

        if (!hasLocationPermission || !hasBackgroundPermission || !hasActivityPermission) {
            RequestPermissions { result ->
                if (result.locationGranted) {
                    hasLocationPermission = true
                    viewModel.setLocationEnabled(true)
                    viewModel.startLocationUpdates()
                }
                if (result.backgroundGranted) {
                    hasBackgroundPermission = true
                    viewModel.registerGeofences()
                }
                if (result.activityGranted) {
                    hasActivityPermission = true
                    viewModel.startStepTracking()
                }
            }
        }

        if (locationEnabled) {
            LaunchedEffect(Unit) {
                viewModel.registerGeofences()
            }
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
    val activityImage = when (activity) {
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

data class PermissionResult(
    val locationGranted: Boolean,
    val backgroundGranted: Boolean,
    val activityGranted: Boolean
)

@Composable
fun RequestPermissions(
    onPermissionsResult: (PermissionResult) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showBackgroundDialog by remember { mutableStateOf(false) }

    fun hasLocationPermissionNow(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasBackgroundPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasActivityPermissionNow(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    val foregroundAndActivityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val locationGranted = fineLocationGranted || coarseLocationGranted

        val activityRecognitionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions[Manifest.permission.ACTIVITY_RECOGNITION] ?: false
        } else {
            true
        }

        onPermissionsResult(
            PermissionResult(
                locationGranted = locationGranted,
                backgroundGranted = hasBackgroundPermission(),
                activityGranted = activityRecognitionGranted
            )
        )

        // Show dialog to explain background location requirement
        if (locationGranted && !hasBackgroundPermission()) {
            showBackgroundDialog = true
        }
    }

    // Show dialog explaining background location before opening settings
    if (showBackgroundDialog) {
        AlertDialog(
            onDismissRequest = { showBackgroundDialog = false },
            title = { Text("Background Location Required") },
            text = {
                Text("To track geofence visits while the app is not in use, please select 'Allow all the time' for location access on the next screen.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBackgroundDialog = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackgroundDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        foregroundAndActivityLauncher.launch(permissions.toTypedArray())
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onPermissionsResult(
                    PermissionResult(
                        locationGranted = hasLocationPermissionNow(),
                        backgroundGranted = hasBackgroundPermission(),
                        activityGranted = hasActivityPermissionNow()
                    )
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
