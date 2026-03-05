package com.example.androidsensorsinvestigation.ui.main.activityRepos

import android.content.Context
import com.example.androidsensorsinvestigation.ui.main.ActivityStateHolder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeActivityRepo @Inject constructor(
    @ApplicationContext context: Context
) : ActivityRecognitionRepository(context) {

    override val activityFlow: StateFlow<Int> = ActivityStateHolder.activityFlow

    override fun startTracking() {
        // no-op for simulation
    }

    override fun stopTracking() {
        // no-op
    }

    fun setActivity(activity: Int) {
        ActivityStateHolder.activityFlow.value = activity
    }
}
