package eu.depau.etchdroid.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import eu.depau.etchdroid.R
import eu.depau.etchdroid.StateKeeper
import eu.depau.etchdroid.enums.FlashMethod
import eu.depau.etchdroid.kotlin_exts.snackbar
import eu.depau.etchdroid.utils.DoNotShowAgainDialogFragment
import kotlinx.android.synthetic.main.activity_start.*
import java.io.File


class StartActivity : ActivityBase() {
    val TAG = "StartActivity"
    val READ_REQUEST_CODE = 42
    val READ_EXTERNAL_STORAGE_PERMISSION = 29
    val DISMISSED_DIALOGS_PREFS = "dismissed_dialogs"
    var delayedButtonClicked: Boolean = false

    var shouldShowDMGAlertDialog: Boolean
        get() {
            val settings = getSharedPreferences(DISMISSED_DIALOGS_PREFS, 0)
            return !settings.getBoolean("DMG_beta_alert", false)
        }
        set(value) {
            val settings = getSharedPreferences(DISMISSED_DIALOGS_PREFS, 0)
            val editor = settings.edit()
            editor.putBoolean("DMG_beta_alert", !value)
            editor.apply()
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
    }

    fun onButtonClicked(view: View) = onButtonClicked(view, true)

    private fun onButtonClicked(view: View?, showDialog: Boolean = true) {
        if (view != null)
            StateKeeper.flashMethod = when (view.id) {
                R.id.btn_image_raw -> FlashMethod.FLASH_API
                R.id.btn_image_dmg -> FlashMethod.FLASH_DMG_API
                else -> null
            }

        if (StateKeeper.flashMethod != FlashMethod.FLASH_DMG_API || !shouldShowDMGAlertDialog || !showDialog)
            showFilePicker()
        else
            showDMGBetaAlertDialog()
    }

    fun showDMGBetaAlertDialog() {
        val dialogFragment = DoNotShowAgainDialogFragment(nightModeHelper.nightMode)
        dialogFragment.title = getString(R.string.here_be_dragons)
        dialogFragment.message = getString(R.string.dmg_alert_dialog_text)
        dialogFragment.positiveButton = getString(R.string.i_understand)
        dialogFragment.listener = object : DoNotShowAgainDialogFragment.DialogListener {
            override fun onDialogNegative(dialog: DoNotShowAgainDialogFragment, showAgain: Boolean) {}
            override fun onDialogPositive(dialog: DoNotShowAgainDialogFragment, showAgain: Boolean) {
                shouldShowDMGAlertDialog = showAgain
                showFilePicker()
            }
        }
        dialogFragment.show(supportFragmentManager, "DMGBetaAlertDialogFragment")
    }

    fun showFilePicker() {
        when (StateKeeper.flashMethod) {
            FlashMethod.FLASH_API -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.setType("*/*");
                startActivityForResult(intent, READ_REQUEST_CODE)
            }
            FlashMethod.FLASH_DMG_API -> {
                if (checkAndRequestStorageReadPerm()) {
                    val sdcard = Environment.getExternalStorageDirectory().absolutePath

                    val chooser = StorageChooser.Builder()
                            .withActivity(this)
                            .withFragmentManager(fragmentManager)
                            .withMemoryBar(true)
                            .allowCustomPath(true)
                            .setType(StorageChooser.FILE_PICKER)
                            .customFilter(arrayListOf("dmg"))
                            .build()
                    chooser.show()
                    chooser.setOnSelectListener {
                        StateKeeper.imageFile = Uri.fromFile(File(it))
                        nextStep()
                    }
                } else {
                    delayedButtonClicked = true
                }
            }
            FlashMethod.FLASH_UNETBOOTIN -> {
            }
            FlashMethod.FLASH_WOEUSB -> {
            }
            null -> {
            }

        }
    }

    private fun checkAndRequestStorageReadPerm(): Boolean {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.READ_EXTERNAL_STORAGE)) {
                btn_image_dmg.snackbar("Storage permission is required to read DMG images")
            } else {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        READ_EXTERNAL_STORAGE_PERMISSION)
            }
        } else {
            // Permission granted
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            READ_EXTERNAL_STORAGE_PERMISSION -> {
                if (delayedButtonClicked)
                    onButtonClicked(null, showDialog = false)
                return
            }
            else -> {
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == READ_REQUEST_CODE && resultCode == AppCompatActivity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            var uri: Uri? = null
            if (data != null) {
                uri = data.getData()
                StateKeeper.imageFile = uri

                nextStep()
            }
        }
    }


    fun nextStep() {
        val intent = Intent(this, UsbDrivePickerActivity::class.java)
        startActivity(intent)
    }
}
