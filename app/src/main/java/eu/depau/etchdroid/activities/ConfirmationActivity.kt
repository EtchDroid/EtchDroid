package eu.depau.etchdroid.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import eu.depau.etchdroid.R
import eu.depau.etchdroid.StateKeeper
import eu.depau.etchdroid.adapters.PartitionTableRecyclerViewAdapter
import eu.depau.etchdroid.enums.FlashMethod
import eu.depau.etchdroid.img_types.DMGImage
import eu.depau.etchdroid.kotlin_exts.*
import eu.depau.etchdroid.services.UsbApiDmgWriteService
import eu.depau.etchdroid.services.UsbApiImgWriteService
import eu.depau.etchdroid.utils.DoNotShowAgainDialogFragment
import kotlinx.android.synthetic.main.activity_confirmation.*
import java.io.IOException

class ConfirmationActivity : ActivityBase() {
    var canContinue: Boolean = false
    var issuesFound: String? = null
    val DISMISSED_DIALOGS_PREFS = "dismissed_dialogs"

    var shouldShowDataLossAlertDialog: Boolean
        get() {
            val settings = getSharedPreferences(DISMISSED_DIALOGS_PREFS, 0)
            return !settings.getBoolean("data_loss_alert", false)
        }
        set(value) {
            val settings = getSharedPreferences(DISMISSED_DIALOGS_PREFS, 0)
            val editor = settings.edit()
            editor.putBoolean("data_loss_alert", !value)
            editor.apply()
        }


    fun showDataLossAlertDialog() {
        val dialogFragment = DoNotShowAgainDialogFragment()
        dialogFragment.title = getString(R.string.warning)
        dialogFragment.message = getString(R.string.dataloss_confirmation_dialog_message)
        dialogFragment.positiveButton = getString(R.string.confirm_flash_image)
        dialogFragment.negativeButton = getString(R.string.cancel)
        dialogFragment.listener = object : DoNotShowAgainDialogFragment.DialogListener {
            override fun onDialogNegative(dialog: DoNotShowAgainDialogFragment, showAgain: Boolean) {}
            override fun onDialogPositive(dialog: DoNotShowAgainDialogFragment, showAgain: Boolean) {
                shouldShowDataLossAlertDialog = showAgain
                nextStep(false)
            }
        }
        dialogFragment.show(supportFragmentManager, "DataLossAlertDialogFragment")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmation)
        actionBar?.setDisplayHomeAsUpEnabled(true)

        // displayImageLayout must be called before displayDetails
        // to ensure uncompressed image size is available
        displayImageLayout()
        displayDetails()
    }

    fun displayDetails() {
        confirm_sel_method.text = when (StateKeeper.flashMethod) {
            FlashMethod.FLASH_API -> getString(R.string.flash_dd_usb_api)
            FlashMethod.FLASH_DMG_API -> getString(R.string.flash_dmg_api)
            FlashMethod.FLASH_UNETBOOTIN -> getString(R.string.flash_unetbootin)
            FlashMethod.FLASH_WOEUSB -> getString(R.string.flash_woeusb)
            else -> null
        }

        confirm_sel_image.text = StateKeeper.imageFile?.getFileName(this)

        if (confirm_sel_image.text == null)
            confirm_sel_image.text = getString(R.string.unknown_filename)

        val imgSize: Long?
        val sizeStr: String?
        if (StateKeeper.imageRepr?.size != null) {
            imgSize = StateKeeper.imageRepr?.size
            sizeStr = imgSize?.toHRSize() + " " + getString(R.string.uncompressed)
        } else {
            imgSize = StateKeeper.imageFile?.getFileSize(this)
            sizeStr = imgSize?.toHRSize()
        }

        confirm_sel_image_size.text = sizeStr

        confirm_sel_usbdev.text = StateKeeper.usbDevice?.name

        for (trial in 0..1) {
            try {
                StateKeeper.usbMassStorageDevice!!.init()
                val blockDev = StateKeeper.usbMassStorageDevice?.blockDevice

                if (blockDev != null) {
                    val devSize = (blockDev.size.toLong() * blockDev.blockSize.toLong())
                    confirm_sel_usbdev_size.text = devSize.toHRSize()

                    if (imgSize!! > devSize)
                        confirm_extra_info.text = getString(R.string.image_bigger_than_usb)
                    else {
                        val text = getString(R.string.tap_next_to_write)
                        confirm_extra_info.text = text
                        canContinue = true
                    }
                } else {
                    confirm_extra_info.text = getString(R.string.cant_read_usbdev)
                }
            } catch (e: IOException) {
                if (trial == 0) {
                    StateKeeper.usbMassStorageDevice!!.close()
                    continue
                } else {
                    confirm_extra_info.text = getString(R.string.could_not_access_usb_error)
                    break
                }
            }
        }
    }

    fun displayImageLayout() {
        val uri = StateKeeper.imageFile ?: return
        val text = uri.getFileName(this)

        if (StateKeeper.flashMethod == FlashMethod.FLASH_DMG_API) {
            StateKeeper.imageRepr = DMGImage(uri, this)
            val imgRepr = StateKeeper.imageRepr as DMGImage

            if (imgRepr.tableType == null && (imgRepr.partitionTable == null || imgRepr.partitionTable?.size == 0)) {
                issuesFound = getString(R.string.image_is_not_dmg)
                part_table_header.text = issuesFound
                return
            } else {
                part_table_header.text = if (imgRepr.tableType != null) getString(R.string.partition_table_title) else ""
                part_table_header_side.text = imgRepr.tableType?.getString(this) ?: ""
                issuesFound = null

                val viewAdapter = PartitionTableRecyclerViewAdapter(imgRepr.partitionTable!!)
                part_table_recycler.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(context)
                    adapter = viewAdapter
                }
            }
        }
    }

    fun nextStep(showDialog: Boolean = true) {
        if (!canContinue || issuesFound != null) {
            confirm_fab.snackbar(issuesFound ?: getString(R.string.cannot_write))
            return
        }

        if (showDialog && shouldShowDataLossAlertDialog) {
            showDataLossAlertDialog()
            return
        }

        toast(getString(R.string.check_notification_progress), Toast.LENGTH_LONG)

        val intent: Intent = when (StateKeeper.flashMethod) {
            FlashMethod.FLASH_API -> Intent(this, UsbApiImgWriteService::class.java)
            FlashMethod.FLASH_DMG_API -> Intent(this, UsbApiDmgWriteService::class.java)
            else -> null!!
        }

        intent.setDataAndType(StateKeeper.imageFile, "application/octet-stream")
        intent.putExtra("usbDevice", StateKeeper.usbDevice)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(intent)
        else
            startService(intent)

        moveTaskToBack(true);
        finish()
    }


    fun onButtonClicked(view: View) {
        when (view.id) {
            R.id.confirm_fab -> nextStep()
        }
    }

}
