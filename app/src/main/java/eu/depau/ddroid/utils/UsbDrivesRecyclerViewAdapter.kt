package eu.depau.ddroid.utils

import android.annotation.SuppressLint
import android.os.Build
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import com.github.mjdev.libaums.UsbMassStorageDevice
import eu.depau.ddroid.R
import kotlinx.android.synthetic.main.usb_device_row.view.*
import java.lang.Integer

class UsbDrivesRecyclerViewAdapter(private val dataset: Array<UsbMassStorageDevice>) : RecyclerView.Adapter<UsbDrivesRecyclerViewAdapter.ViewHolder>() {

    class ViewHolder(val relLayout: RelativeLayout) : RecyclerView.ViewHolder(relLayout)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            UsbDrivesRecyclerViewAdapter.ViewHolder {

        val relLayout = LayoutInflater.from(parent.context)
                .inflate(R.layout.usb_device_row, parent, false) as RelativeLayout
        return ViewHolder(relLayout)
    }


    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val usbDevice = dataset[position].usbDevice

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.relLayout.name.text = "${usbDevice.manufacturerName} ${usbDevice.productName}"
            holder.relLayout.devpath.text = usbDevice.deviceName
            holder.relLayout.vidpid.text = usbDevice.vidpid
        } else {
            holder.relLayout.name.text = usbDevice.deviceName
            holder.relLayout.devpath.text = usbDevice.vidpid
        }
    }


    override fun getItemCount(): Int = dataset.size


    fun get(position: Int): UsbMassStorageDevice {
        return dataset[position]
    }
}