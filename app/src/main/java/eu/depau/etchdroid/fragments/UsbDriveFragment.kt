package eu.depau.etchdroid.fragments

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import com.github.mjdev.libaums.UsbMassStorageDevice
import eu.depau.etchdroid.R
import eu.depau.etchdroid.StateKeeper
import eu.depau.etchdroid.utils.ClickListener
import eu.depau.etchdroid.activities.WizardActivity
import eu.depau.etchdroid.activities.MainActivity
import eu.depau.etchdroid.adapters.UsbDrivesRecyclerViewAdapter
import eu.depau.etchdroid.kotlin_exts.name
import eu.depau.etchdroid.kotlin_exts.snackbar
import eu.depau.etchdroid.enums.WizardStep
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_select_usb_drive.view.*


/**
 * A placeholder fragment containing a simple view.
 */
class UsbDriveFragment : WizardFragment(), SwipeRefreshLayout.OnRefreshListener {
    val TAG = "UsbDriveFragment"

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: UsbDrivesRecyclerViewAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var refreshLayout: SwipeRefreshLayout


    class RecyclerViewTouchListener(context: Context, val recyclerView: RecyclerView, val clickListener: ClickListener) : RecyclerView.OnItemTouchListener {
        private var gestureDetector: GestureDetector

        init {
            gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    val child = recyclerView.findChildViewUnder(e.x, e.y)
                    if (child != null)
                        clickListener.onLongClick(child, recyclerView.getChildAdapterPosition(child))
                }
            })
        }

        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}

        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            val child = rv.findChildViewUnder(e.x, e.y)
            if (child != null && gestureDetector.onTouchEvent(e)) {
                clickListener.onClick(child, rv.getChildAdapterPosition(child))
            }
            return false
        }

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
    }


    override fun onRefresh() {
        loadUsbDevices()
    }


    override fun nextStep(view: View?) {
        (activity as WizardActivity).goToNewFragment(ConfirmInfoFragment())
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        StateKeeper.currentFragment = this
        StateKeeper.wizardStep = WizardStep.SELECT_USB_DRIVE

        val view = inflater.inflate(R.layout.fragment_select_usb_drive, container, false)

        refreshLayout = view.usbdevs_refresh_layout
        refreshLayout.setOnRefreshListener(this)
        refreshLayout.post {
            refreshLayout.isRefreshing = true
            loadUsbDevices()
        }

        viewManager = LinearLayoutManager(activity)
        recyclerView = view.usbdevs_recycler_view

        recyclerView.addOnItemTouchListener(RecyclerViewTouchListener(activity!!, recyclerView, object : ClickListener {
            override fun onClick(view: View, position: Int) {
                val device = viewAdapter.get(position)
                val manager = activity!!.getSystemService(Context.USB_SERVICE) as UsbManager
                manager.requestPermission(device.usbDevice, (activity as MainActivity).mUsbPermissionIntent)
            }

            override fun onLongClick(view: View, position: Int) {}
        }))

        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    fun loadUsbDevices() {
        try {
            viewAdapter = UsbDrivesRecyclerViewAdapter(UsbMassStorageDevice.getMassStorageDevices(activity))

            recyclerView.apply {
                setHasFixedSize(true)
                layoutManager = viewManager
                adapter = viewAdapter
            }
        } finally {
            refreshLayout.isRefreshing = false
        }
    }

    override fun onFragmentAdded(activity: WizardActivity) {
        activity.fab.hide()
    }

    override fun onFragmentRemoving(activity: WizardActivity) {
        activity.fab.show()
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.usb_devices_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_refresh -> {
                loadUsbDevices()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun onUsbPermissionResult(usbDevice: UsbDevice?, granted: Boolean) {
        if (!granted) {
            if (usbDevice != null) {
                recyclerView.snackbar(getString(R.string.usb_perm_denied) + usbDevice.name)
            } else {
                recyclerView.snackbar(getString(R.string.usb_perm_denied_noname))
            }
            return
        }

        StateKeeper.usbDevice = usbDevice
        StateKeeper.usbMassStorageDevice = UsbMassStorageDevice.getMassStorageDevices(activity).find { it.usbDevice == usbDevice }

        nextStep(null)
    }
}

