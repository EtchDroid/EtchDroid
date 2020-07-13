package eu.depau.etchdroid.ui.fragments.main

import androidx.lifecycle.ViewModel
import eu.depau.etchdroid.repositories.MainRepository
import javax.inject.Inject

class MainViewModel @Inject constructor(var repository: MainRepository) : ViewModel() {
    var delayedButtonClicked: Boolean = false

}