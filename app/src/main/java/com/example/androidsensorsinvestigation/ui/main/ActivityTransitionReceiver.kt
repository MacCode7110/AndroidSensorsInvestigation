package com.example.androidsensorsinvestigation.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityRecognitionResult
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
        // 1. Handle Real-time Activity Recognition Results (Faster updates)
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = ActivityRecognitionResult.extractResult(intent)
            result?.mostProbableActivity?.let { activity ->
                // Only update if confidence is high enough (e.g., > 75%)
                if (activity.confidence >= 75) {
                    updateActivity(activity.type)
                }
            }
        }
        
        // 2. Handle Activity Transition Events (Confirmed transitions)
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            result?.transitionEvents?.forEach { event ->
                if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                    updateActivity(event.activityType)
                }
            }
        } 
        
        // 3. Handle Debug Signal
        if (intent.action == ACTION_DEBUG_ACTIVITY) {
            val activityType = intent.getIntExtra(EXTRA_ACTIVITY_TYPE, DetectedActivity.STILL)
            updateActivity(activityType)
        }
    }

    private fun updateActivity(type: Int) {
        if (ActivityStateHolder.activityFlow.value != type) {
            ActivityStateHolder.activityFlow.value = type
        }
    }
}
