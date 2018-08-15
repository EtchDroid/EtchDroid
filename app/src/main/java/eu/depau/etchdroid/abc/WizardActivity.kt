package eu.depau.etchdroid.abc

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.view.View
import eu.depau.etchdroid.StateKeeper

abstract class WizardActivity : AppCompatActivity() {
    abstract fun goToNewFragment(fragment: WizardFragment)

    open fun onCheckBoxClicked(view: View) {
        StateKeeper.currentFragment?.onCheckBoxClicked(view)
    }

    open fun onButtonClicked(view: View) {
        StateKeeper.currentFragment?.onButtonClicked(view)
    }

    open fun onRadioButtonClicked(view: View) {
        StateKeeper.currentFragment?.onRadioButtonClicked(view)
    }

    open fun nextStep(view: View) {
        StateKeeper.currentFragment?.nextStep(view)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        StateKeeper.currentFragment?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        StateKeeper.currentFragment?.onActivityResult(requestCode, resultCode, data)
    }
}