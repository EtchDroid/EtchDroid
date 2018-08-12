package eu.depau.ddroid.fragments

import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.mjdev.libaums.UsbMassStorageDevice
import eu.depau.ddroid.R
import eu.depau.ddroid.StateKeeper
import eu.depau.ddroid.abc.WizardFragment
import eu.depau.ddroid.values.WizardStep


/**
 * A placeholder fragment containing a simple view.
 */
class USBDriveFragment : WizardFragment() {
    val TAG = "USBDriveFragment"
    val ACTION_USB_PERMISSION = "eu.depau.ddroid.USB_PERMISSION"


    override fun nextStep(view: View) {
//        if (StateKeeper.flashMethod == null)
//            Snackbar.make(view, "Please select writing method", Snackbar.LENGTH_LONG).show()
//        else
//            (activity as WizardActivity).goToNewFragment(ImageLocationFragment())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        StateKeeper.currentFragment = this
        StateKeeper.wizardStep = WizardStep.SELECT_USB_DRIVE

        return inflater.inflate(R.layout.fragment_select_usb_drive, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onButtonClicked(view: View) {
//        val usbManager = activity!!.getSystemService(Context.USB_SERVICE) as UsbManager

        val devices = UsbMassStorageDevice.getMassStorageDevices(activity)

        for (device in devices) {
            Log.d(TAG, """USB device ${device.usbDevice.deviceName}
                | proto ${device.usbDevice.deviceProtocol}
                | id ${device.usbDevice.deviceId}
                | class ${device.usbDevice.deviceClass}
                | subclass ${device.usbDevice.deviceSubclass}
                | iface count ${device.usbDevice.interfaceCount}
                | manuf name ${device.usbDevice.manufacturerName}
                | product name ${device.usbDevice.productName}
                | id ${device.usbDevice.vendorId}:${device.usbDevice.productId}
            """.trimMargin())
//            val permissionIntent = PendingIntent.getBroadcast(activity, 0, Intent(MY_PERMISSIONS_REQUEST_USB_DRIVE), 0)
//            usbManager.requestPermission(device.usbDevice, permissionIntent)
//            // before interacting with a device you need to call init()!
//            device.init()
//
//            // Only uses the first partition on the device
//            val currentFs = device.partitions[0].fileSystem
//            Log.d(TAG, "Capacity: " + currentFs.capacity)
//            Log.d(TAG, "Occupied Space: " + currentFs.occupiedSpace)
//            Log.d(TAG, "Free Space: " + currentFs.freeSpace)
//            Log.d(TAG, "Chunk size: " + currentFs.chunkSize)
        }
    }
}
