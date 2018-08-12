package eu.depau.ddroid.abc

import android.content.Intent
import android.support.v4.app.Fragment
import android.view.View

abstract class WizardFragment : Fragment() {
    abstract fun nextStep(view: View)

    open fun onFragmentAdded(activity: WizardActivity) {}
    open fun onRadioButtonClicked(view: View) {}
    open fun onCheckBoxClicked(view: View) {}
    open override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {}
    open fun onButtonClicked(view: View) {}
    open override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {}
}