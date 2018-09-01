package eu.depau.etchdroid.activities

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.mjdev.libaums.UsbMassStorageDevice
import eu.depau.etchdroid.R
import eu.depau.etchdroid.StateKeeper
import eu.depau.etchdroid.adapters.UsbDrivesRecyclerViewAdapter
import eu.depau.etchdroid.kotlin_exts.name
import eu.depau.etchdroid.kotlin_exts.snackbar
import eu.depau.etchdroid.utils.ClickListener
import eu.depau.etchdroid.utils.EmptyRecyclerView
import eu.depau.etchdroid.utils.RecyclerViewTouchListener
import kotlinx.android.synthetic.main.activity_usb_drive_picker.*

class UsbDrivePickerActivity : ActivityBase(), SwipeRefreshLayout.OnRefreshListener {
    val USB_PERMISSION = "eu.depau.etchdroid.USB_PERMISSION"
    lateinit var mUsbPermissionIntent: PendingIntent

    private lateinit var recyclerView: EmptyRecyclerView
    private lateinit var viewAdapter: UsbDrivesRecyclerViewAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var refreshLayout: SwipeRefreshLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usb_drive_picker)
        actionBar?.setDisplayHomeAsUpEnabled(true)

        mUsbPermissionIntent = PendingIntent.getBroadcast(this, 0, Intent(USB_PERMISSION), 0)
        val usbPermissionFilter = IntentFilter(USB_PERMISSION)
        val usbAttachedFilter = IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        val usbDetachedFilter = IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED)

        registerReceiver(mUsbReceiver, usbPermissionFilter)
        registerReceiver(mUsbReceiver, usbAttachedFilter)
        registerReceiver(mUsbReceiver, usbDetachedFilter)

        refreshLayout = usbdevs_swiperefreshlayout
        refreshLayout.setOnRefreshListener(this)
        refreshLayout.post {
            refreshLayout.isRefreshing = true
            loadUsbDevices()
        }

        viewManager = LinearLayoutManager(this)
        recyclerView = usbdevs_recycler_view
        recyclerView.emptyView = usbdevs_recycler_empty_view

        recyclerView.addOnItemTouchListener(RecyclerViewTouchListener(this, recyclerView, object : ClickListener {
            override fun onClick(view: View, position: Int) {
                val device = viewAdapter.get(position)
                val manager = getSystemService(Context.USB_SERVICE) as UsbManager
                manager.requestPermission(device.usbDevice, mUsbPermissionIntent)
            }

            override fun onLongClick(view: View, position: Int) {}
        }))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mUsbReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.usb_devices_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                loadUsbDevices()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRefresh() {
        loadUsbDevices()
    }


    private val mUsbReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                USB_PERMISSION -> synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                    val result = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    if (result)
                        device?.apply {
                            StateKeeper.usbDevice = this
                        }

                    if (!result) {
                        if (device != null) {
                            recyclerView.snackbar(getString(R.string.usb_perm_denied) + device.name)
                        } else {
                            recyclerView.snackbar(getString(R.string.usb_perm_denied_noname))
                        }
                        return
                    }

                    StateKeeper.usbDevice = device
                    StateKeeper.usbMassStorageDevice = UsbMassStorageDevice.getMassStorageDevices(context).find { it.usbDevice == device }

                    nextStep()
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED, UsbManager.ACTION_USB_DEVICE_DETACHED -> loadUsbDevices()
            }
        }
    }

    fun loadUsbDevices() {
        try {
            viewAdapter = UsbDrivesRecyclerViewAdapter(UsbMassStorageDevice.getMassStorageDevices(this))

            recyclerView.apply {
                setHasFixedSize(true)
                layoutManager = viewManager
                adapter = viewAdapter
            }
        } finally {
            refreshLayout.isRefreshing = false
        }
    }


    fun nextStep() {
        val intent = Intent(this, ConfirmationActivity::class.java)
        startActivity(intent)
    }
}
