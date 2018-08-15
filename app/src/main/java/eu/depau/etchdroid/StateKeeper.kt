package eu.depau.etchdroid

import android.hardware.usb.UsbDevice
import android.net.Uri
import com.github.mjdev.libaums.UsbMassStorageDevice
import eu.depau.etchdroid.abc.WizardFragment
import eu.depau.etchdroid.values.FlashMethod
import eu.depau.etchdroid.values.ImageLocation
import eu.depau.etchdroid.values.WizardStep

object StateKeeper {
    var wizardStep: WizardStep = WizardStep.SELECT_FLASH_METHOD
    var currentFragment: WizardFragment? = null
    var flashMethod: FlashMethod? = null
    var imageLocation: ImageLocation? = null
    var streamingWrite: Boolean = false
    var imageFile: Uri? = null

    var usbDevice: UsbDevice? = null
    var usbMassStorageDevice: UsbMassStorageDevice? = null
}