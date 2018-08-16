package eu.depau.etchdroid.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import eu.depau.etchdroid.R
import eu.depau.etchdroid.StateKeeper
import eu.depau.etchdroid.services.UsbAPIWriteService
import eu.depau.etchdroid.kotlin_exts.*
import eu.depau.etchdroid.enums.FlashMethod
import eu.depau.etchdroid.enums.WizardStep
import kotlinx.android.synthetic.main.fragment_confirminfo.view.*
import java.io.IOException

/**
 * A placeholder fragment containing a simple view.
 */
class ConfirmInfoFragment : WizardFragment() {
    val TAG = "ConfirmInfoFragment"
    var canContinue = false

    override fun nextStep(view: View?) {
        if (!canContinue) {
            view?.snackbar("Cannot write image to USB drive")
            return
        }

        context?.toast("Check notification for progress")

        val intent = Intent(activity, UsbAPIWriteService::class.java)
        intent.setDataAndType(StateKeeper.imageFile, "application/octet-stream")
        intent.putExtra("usbDevice", StateKeeper.usbDevice)
        activity?.startService(intent)
        activity?.finish()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        StateKeeper.currentFragment = this
        StateKeeper.wizardStep = WizardStep.CONFIRM

        val view = inflater.inflate(R.layout.fragment_confirminfo, container, false)

        view.confirm_sel_method.text = when (StateKeeper.flashMethod) {
            FlashMethod.FLASH_API -> getString(R.string.flash_dd_usb_api)
            FlashMethod.FLASH_DMG_API -> getString(R.string.flash_dmg_api)
            FlashMethod.FLASH_UNETBOOTIN -> getString(R.string.flash_unetbootin)
            FlashMethod.FLASH_WOEUSB -> getString(R.string.flash_woeusb)
            else -> null
        }

        view.confirm_sel_image.text = StateKeeper.imageFile?.getFileName(context!!)

        if (view.confirm_sel_image.text == null)
            view.confirm_sel_image.text = getString(R.string.unknown_filename)

        val imgSize = StateKeeper.imageFile?.getFileSize(context!!)
        view.confirm_sel_image_size.text = imgSize?.toHRSize()

        view.confirm_sel_usbdev.text = StateKeeper.usbDevice?.name

        for (trial in 0..1) {
            try {
                StateKeeper.usbMassStorageDevice!!.init()
                val blockDev = StateKeeper.usbMassStorageDevice?.blockDevice

                if (blockDev != null) {
                    val devSize = (blockDev.size.toLong() * blockDev.blockSize.toLong())
                    view.confirm_sel_usbdev_size.text = devSize.toHRSize()

                    if (imgSize!! > devSize)
                        view.confirm_extra_info.text = getString(R.string.image_bigger_than_usb)
                    else {
                        view.confirm_extra_info.text = getString(R.string.tap_next_to_write)
                        canContinue = true
                    }
                } else {
                    view.confirm_extra_info.text = getString(R.string.cant_read_usbdev)
                }
            } catch (e: IOException) {
                if (trial == 0) {
                    StateKeeper.usbMassStorageDevice!!.close()
                    continue
                } else {
                    view.confirm_extra_info.text = "Could not access USB device. Maybe you ran the app previously and it crashed? Remove and reinsert the USB drive, then restart the app."
                    break
                }
            }
        }

        return view
    }


}
