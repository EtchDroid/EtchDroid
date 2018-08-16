package eu.depau.etchdroid

import android.hardware.usb.UsbDevice
import android.net.Uri
import com.github.mjdev.libaums.UsbMassStorageDevice
import eu.depau.etchdroid.fragments.WizardFragment
import eu.depau.etchdroid.enums.FlashMethod
import eu.depau.etchdroid.enums.ImageLocation
import eu.depau.etchdroid.enums.WizardStep
import eu.depau.etchdroid.img_types.Image

object StateKeeper {
    var wizardStep: WizardStep = WizardStep.SELECT_FLASH_METHOD
    var currentFragment: WizardFragment? = null
    var flashMethod: FlashMethod? = null
    var imageLocation: ImageLocation? = null
    var streamingWrite: Boolean = false
    var imageFile: Uri? = null
    var imageRepr: Image? = null

    var usbDevice: UsbDevice? = null
    var usbMassStorageDevice: UsbMassStorageDevice? = null
}