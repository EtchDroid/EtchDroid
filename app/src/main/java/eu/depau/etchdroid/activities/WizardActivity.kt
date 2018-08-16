package eu.depau.etchdroid.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.github.isabsent.filepicker.SimpleFilePickerDialog
import eu.depau.etchdroid.StateKeeper
import eu.depau.etchdroid.fragments.WizardFragment

abstract class WizardActivity : AppCompatActivity(), SimpleFilePickerDialog.InteractionListenerString {
    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (StateKeeper.currentFragment is SimpleFilePickerDialog.InteractionListenerString)
            return (StateKeeper.currentFragment as SimpleFilePickerDialog.InteractionListenerString).onResult(dialogTag, which, extras)
        throw RuntimeException("Wrong fragment fsType")
    }

    override fun showListItemDialog(title: String?, folderPath: String?, mode: SimpleFilePickerDialog.CompositeMode?, dialogTag: String?) {
        if (StateKeeper.currentFragment is SimpleFilePickerDialog.InteractionListenerString)
            return (StateKeeper.currentFragment as SimpleFilePickerDialog.InteractionListenerString).showListItemDialog(title, folderPath, mode, dialogTag)
    }

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