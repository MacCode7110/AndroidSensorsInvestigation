package com.example.androidsensorsinvestigation.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import kotlinx.coroutines.flow.MutableStateFlow

object ActivityStateHolder {
    val activityFlow = MutableStateFlow(DetectedActivity.STILL)
}

class ActivityTransitionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DEBUG_ACTIVITY = "com.example.androidsensorsinvestigation.DEBUG_ACTIVITY"
        const val EXTRA_ACTIVITY_TYPE = "extra_activity_type"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Handle Real System Result
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            result?.transitionEvents?.forEach { event ->
                if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                    updateActivity(event.activityType)
                }
            }
        } 
        // Handle Debug Signal
        else if (intent.action == ACTION_DEBUG_ACTIVITY) {
            val activityType = intent.getIntExtra(EXTRA_ACTIVITY_TYPE, DetectedActivity.STILL)
            updateActivity(activityType)
        }
    }

    private fun updateActivity(type: Int) {
        ActivityStateHolder.activityFlow.value = type
    }
}
