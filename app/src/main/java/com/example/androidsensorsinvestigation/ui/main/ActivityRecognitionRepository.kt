package com.example.androidsensorsinvestigation.ui.main

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRecognitionRepository @Inject constructor(
    @ApplicationContext private val context: Context
){
    private val client = ActivityRecognition.getClient(context)

    val activityFlow: StateFlow<Int> = ActivityStateHolder.activityFlow

    fun startTracking(){
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
                    .build()
            )
        }

        val request = ActivityTransitionRequest(transitions)

        val intent = Intent(context, ActivityTransitionReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        client.requestActivityTransitionUpdates(request, pendingIntent)
    }

    fun stopTracking(){
        val intent = Intent(context, ActivityTransitionReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        client.requestActivityTransitionUpdates(pendingIntent)
    }
}