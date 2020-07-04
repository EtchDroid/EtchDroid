package eu.depau.etchdroid.ui.adapters

import android.annotation.SuppressLint
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.github.mjdev.libaums.UsbMassStorageDevice
import eu.depau.etchdroid.R
import eu.depau.etchdroid.utils.ktexts.vidpid
import kotlinx.android.synthetic.main.usb_device_row.view.*

class UsbDrivesRecyclerViewAdapter(private val dataset: Array<UsbMassStorageDevice>) : RecyclerView.Adapter<UsbDrivesRecyclerViewAdapter.ViewHolder>() {

    class ViewHolder(val relLayout: RelativeLayout) : RecyclerView.ViewHolder(relLayout)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            ViewHolder {

        val relLayout = LayoutInflater.from(parent.context)
                .inflate(R.layout.usb_device_row, parent, false) as RelativeLayout
        return ViewHolder(relLayout)
    }


    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val usbDevice = dataset[position].usbDevice
        val usbDeviceName = usbDevice.deviceName.trim()
        val usbDeviceVidPid = usbDevice.vidpid
        val usbDeviceManufacturer = usbDevice.manufacturerName?.trim()
        val usbProductName = usbDevice.productName?.trim()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.relLayout.usbdev_name.text = "$usbDeviceManufacturer $usbProductName"
            holder.relLayout.devpath.text = usbDeviceName
            holder.relLayout.vidpid.text = usbDeviceVidPid
        } else {
            holder.relLayout.usbdev_name.text = usbDeviceName
            holder.relLayout.devpath.text = usbDeviceVidPid
        }
    }


    override fun getItemCount(): Int = dataset.size


    fun get(position: Int): UsbMassStorageDevice {
        return dataset[position]
    }
}