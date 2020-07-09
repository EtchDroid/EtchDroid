package eu.depau.etchdroid.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import eu.depau.etchdroid.ui.fragments.main.MainFragment

@Suppress("unused")
@Module
abstract class FragmentInjectorsModule {

    @ContributesAndroidInjector
    abstract fun injectMainFragment(): MainFragment

}