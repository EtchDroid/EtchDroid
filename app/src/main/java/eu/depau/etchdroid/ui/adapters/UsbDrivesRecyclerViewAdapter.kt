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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.relLayout.usbdev_name.text = "${usbDevice.manufacturerName} ${usbDevice.productName}"
            holder.relLayout.devpath.text = usbDevice.deviceName
            holder.relLayout.vidpid.text = usbDevice.vidpid
        } else {
            holder.relLayout.usbdev_name.text = usbDevice.deviceName
            holder.relLayout.devpath.text = usbDevice.vidpid
        }
    }


    override fun getItemCount(): Int = dataset.size


    fun get(position: Int): UsbMassStorageDevice {
        return dataset[position]
    }
}