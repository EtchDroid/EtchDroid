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
import eu.depau.etchdroid.R
import eu.depau.etchdroid.StateKeeper
import eu.depau.etchdroid.fragments.WizardFragment
import eu.depau.etchdroid.fragments.FlashMethodFragment
import eu.depau.etchdroid.fragments.UsbDriveFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : WizardActivity() {
    val TAG = "MainActivity"
    val ACTION_USB_PERMISSION = "eu.depau.etchdroid.USB_PERMISSION"
    lateinit var mUsbPermissionIntent: PendingIntent

    private val mUsbReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_USB_PERMISSION) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                    val result = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    if (result)
                        device?.apply {
                            StateKeeper.usbDevice = this
                        }

                    if (StateKeeper.currentFragment is UsbDriveFragment)
                        (StateKeeper.currentFragment as UsbDriveFragment).onUsbPermissionResult(device, result)
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        fab.setOnClickListener(::nextStep)

        // Create new fragment and transaction
        val fragment = FlashMethodFragment()
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_layout, fragment)
        transaction.commit()
        fragment.onFragmentAdded(this)

        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        mUsbPermissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(mUsbReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(mUsbReceiver)
    }

    override fun goToNewFragment(fragment: WizardFragment) {
        StateKeeper.currentFragment?.onFragmentRemoving(this)

        val transaction = supportFragmentManager.beginTransaction()
        transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
        transaction.replace(R.id.fragment_layout, fragment)
        transaction.addToBackStack(null)
        transaction.commit()

        fragment.onFragmentAdded(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_licenses -> {
                val intent = Intent(this, LicensesActivity::class.java)
                startActivity(intent)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
