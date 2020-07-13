package eu.depau.etchdroid

import android.os.Bundle
import dagger.android.AndroidInjection
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import eu.depau.etchdroid.ui.activities.ActivityBase
import javax.inject.Inject

class MainActivity : ActivityBase(), HasAndroidInjector {

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun androidInjector() = dispatchingAndroidInjector
}