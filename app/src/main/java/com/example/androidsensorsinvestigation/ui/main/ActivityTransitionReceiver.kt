package com.example.androidsensorsinvestigation.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import kotlinx.coroutines.flow.MutableStateFlow

object ActivityStateHolder {
    val activityFlow = MutableStateFlow(DetectedActivity.STILL)
}

class ActivityTransitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)

            result?.transitionEvents?.forEach { event ->

                if (event.transitionType ==
                    ActivityTransition.ACTIVITY_TRANSITION_ENTER
                ) {
                    ActivityStateHolder.activityFlow.value =
                        event.activityType
                }
            }
        }
    }
}