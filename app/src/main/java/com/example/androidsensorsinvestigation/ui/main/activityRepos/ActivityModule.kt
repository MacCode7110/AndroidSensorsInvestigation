package com.example.androidsensorsinvestigation.ui.main.activityRepos

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ActivityModule {

    //Uncomment to test debug repo with buttons to change activity
    /*
    @Binds
    @Singleton
    abstract fun bindActivityRepository(
        fakeRepo: FakeActivityRepo
    ): ActivityRecognitionRepository

     */
}
