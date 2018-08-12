package eu.depau.ddroid.fragments

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import eu.depau.ddroid.R
import eu.depau.ddroid.StateKeeper
import eu.depau.ddroid.abc.WizardActivity
import eu.depau.ddroid.abc.WizardFragment
import eu.depau.ddroid.values.FlashMethod
import eu.depau.ddroid.values.WizardStep

/**
 * A placeholder fragment containing a simple view.
 */
class FlashMethodFragment : WizardFragment() {
    override fun nextStep(view: View) {
        if (StateKeeper.flashMethod == null)
            Snackbar.make(view, "Please select writing method", Snackbar.LENGTH_LONG).show()
        else
            (activity as WizardActivity).goToNewFragment(ImageLocationFragment())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        StateKeeper.currentFragment = this
        StateKeeper.wizardStep = WizardStep.SELECT_FLASH_METHOD

        return inflater.inflate(R.layout.fragment_select_flash_method, container, false)
    }

    override fun onRadioButtonClicked(view: View) {
        StateKeeper.flashMethod = when (view.id) {
            R.id.flash_dd_root_radio -> FlashMethod.FLASH_DD
            R.id.flash_dd_usb_api_radio -> FlashMethod.FLASH_API
            R.id.flash_unetbootin_radio -> FlashMethod.FLASH_UNETBOOTIN
            R.id.flash_woeusb_radio -> FlashMethod.FLASH_WOEUSB
            else -> null
        }
    }
}
