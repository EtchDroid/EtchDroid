package eu.depau.etchdroid

import android.app.Application
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import eu.depau.etchdroid.di.DaggerAppComponent
import javax.inject.Inject

class EtchDroidApp : Application(), HasAndroidInjector {

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    override fun onCreate() {
        super.onCreate()

        DaggerAppComponent.builder()
                .application(this)
                .build()
                .inject(this)
    }

    override fun androidInjector() = dispatchingAndroidInjector

}