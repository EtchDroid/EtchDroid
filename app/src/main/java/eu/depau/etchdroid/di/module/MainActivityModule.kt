package eu.depau.etchdroid.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import eu.depau.etchdroid.MainActivity

@Suppress("unused")
@Module
abstract class MainActivityModule {
    @ContributesAndroidInjector(modules = [FragmentInjectorsModule::class])
    abstract fun contributeMainActivity(): MainActivity
}