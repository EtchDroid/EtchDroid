package eu.depau.etchdroid.di

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import eu.depau.etchdroid.EtchDroidApp
import eu.depau.etchdroid.di.module.AppModule
import eu.depau.etchdroid.di.module.DatabaseModule
import eu.depau.etchdroid.di.module.MainActivityModule
import javax.inject.Singleton

@Singleton
@Component(
        modules = [
            AndroidInjectionModule::class,
            AppModule::class,
            MainActivityModule::class,
            DatabaseModule::class
        ]
)
interface AppComponent {

    fun inject(etchDroidApp: EtchDroidApp)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder
        fun build(): AppComponent
    }
}