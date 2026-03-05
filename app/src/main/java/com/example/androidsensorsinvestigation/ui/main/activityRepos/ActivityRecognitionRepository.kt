package com.example.androidsensorsinvestigation.ui.main.activityRepos

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.androidsensorsinvestigation.ui.main.ActivityStateHolder
import com.example.androidsensorsinvestigation.ui.main.ActivityTransitionReceiver
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class ActivityRecognitionRepository @Inject constructor(
    @ApplicationContext private val context: Context
){
    private val client = ActivityRecognition.getClient(context)

    open val activityFlow: StateFlow<Int> = ActivityStateHolder.activityFlow

    open fun startTracking() {
        val transitions = listOf(
            DetectedActivity.STILL,
            DetectedActivity.WALKING,
            DetectedActivity.RUNNING,
            DetectedActivity.IN_VEHICLE
        ).flatMap { activityType ->
            listOf(
                ActivityTransition.Builder()
                    .setActivityType(activityType)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(activityType)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )
        }

        val request = ActivityTransitionRequest(transitions)

        val intent = Intent(context, ActivityTransitionReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        try {
            client.requestActivityTransitionUpdates(request, pendingIntent)
        }
        catch (e: SecurityException){
            e.printStackTrace()
        }
    }

    open fun stopTracking(){
        val intent = Intent(context, ActivityTransitionReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        try {
            client.removeActivityTransitionUpdates(pendingIntent)
        }
        catch (e: SecurityException){
            e.printStackTrace()
        }
    }
}
