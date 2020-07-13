package eu.depau.etchdroid.ui.fragments.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import com.codekidlabs.storagechooser.StorageChooser
import com.github.mjdev.libaums.usb.UsbCommunicationFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.android.support.AndroidSupportInjection
import eu.depau.etchdroid.MainActivity
import eu.depau.etchdroid.R
import eu.depau.etchdroid.StateKeeper
import eu.depau.etchdroid.databinding.FragmentMainBinding
import eu.depau.etchdroid.ui.activities.ActivityBase
import eu.depau.etchdroid.ui.activities.UsbDrivePickerActivity
import eu.depau.etchdroid.utils.enums.FlashMethod
import me.jahnen.libaums.libusbcommunication.LibusbCommunicationCreator
import java.io.File
import javax.inject.Inject


class MainFragment : Fragment() {

    @Inject
    lateinit var viewModel: MainViewModel

    private lateinit var binding: FragmentMainBinding

    private lateinit var mainActivity: MainActivity

    private var shouldShowDMGAlertDialog: Boolean = true

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mainActivity = (requireActivity() as MainActivity)

        if (!StateKeeper.libusbRegistered) {
            UsbCommunicationFactory.apply {
                registerCommunication(LibusbCommunicationCreator())
                underlyingUsbCommunication = UsbCommunicationFactory.UnderlyingUsbCommunication.OTHER
            }
            StateKeeper.libusbRegistered = true
        }

        shouldShowDMGAlertDialog = getAlertSettings()
    }

    /*
    * Use this functions because sharedPreferences won't be called after var change.
    * We don't need to get sPref on each getting for this var.
    * */
    private fun getAlertSettings(): Boolean {
        val settings = requireActivity().getSharedPreferences(ActivityBase.DISMISSED_DIALOGS_PREFS, 0)
        return settings.getBoolean("DMG_beta_alert", true)
    }

    /*
    * The setter will be called after initialization in onActivityCreated.
    * But we don't need to update sPref after getting.
    * */
    private fun updateAlertSettings(value: Boolean) {
        val settings = requireActivity().getSharedPreferences(ActivityBase.DISMISSED_DIALOGS_PREFS, 0)
        settings.edit().putBoolean("DMG_beta_alert", value).apply()
        shouldShowDMGAlertDialog = value
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel

        binding.buttonRaw.setOnClickListener(this::onButtonClicked)
        binding.buttonDmg.setOnClickListener(this::onButtonClicked)
        binding.buttonBrokenUsb.setOnClickListener(this::onButtonClicked)

        return binding.root
    }

    private fun openBrokenUsbPage() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://etchdroid.depau.eu/broken_usb/"))
        startActivity(intent)
    }

    private fun showDMGBetaAlertDialog(callback: () -> Unit) {
        // TODO extract to new dialog builder
        val dialogFragment = MaterialAlertDialogBuilder(requireContext())
        val inflater = layoutInflater
        val dialogView: View = inflater.inflate(R.layout.do_not_show_again, null) // this line

        dialogFragment.setView(dialogView)

        val showNextTimeCheckBox = dialogView.findViewById<CheckBox>(R.id.do_not_show_again)

        dialogFragment.setTitle(getString(R.string.here_be_dragons))
        dialogFragment.setMessage(getString(R.string.dmg_alert_dialog_text))
        dialogFragment.setPositiveButton(getString(R.string.i_understand)) { _, _ ->
            updateAlertSettings(!showNextTimeCheckBox.isChecked)
            callback()
        }

        dialogFragment.show()
    }

    private fun onButtonClicked(view: View?) {
        view?.let {
            when (it.id) {
                R.id.buttonBrokenUsb -> {
                    openBrokenUsbPage()
                    return
                }
                R.id.buttonRaw -> StateKeeper.flashMethod = FlashMethod.FLASH_API
                R.id.buttonDmg -> StateKeeper.flashMethod = FlashMethod.FLASH_DMG_API
            }

            if (shouldShowDMGAlertDialog && StateKeeper.flashMethod == FlashMethod.FLASH_DMG_API) {
                showDMGBetaAlertDialog {
                    onButtonClicked(view)
                }
                return
            }

        }
        showFilePicker()
    }

    /**
     * [Opened issue](https://github.com/codekidX/storage-chooser/issues/91)
     */
    @Deprecated("Will be patched in Storage chooser 3.0")
    private fun showFilePicker() {
        when (StateKeeper.flashMethod) {
            FlashMethod.FLASH_API -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "*/*"
                startActivityForResult(intent, ActivityBase.READ_REQUEST_CODE)
            }
            FlashMethod.FLASH_DMG_API -> {

                if (checkAndRequestStorageReadPerm()) {
                    val sdcard = Environment.getExternalStorageDirectory().absolutePath

                    val chooser = StorageChooser.Builder()
                            .withActivity(mainActivity)
                            .withFragmentManager(mainActivity.fragmentManager)
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
                    viewModel.delayedButtonClicked = true
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

    // In ActivityBase.kt this function doesn't work with the fragment.
    private fun checkAndRequestStorageReadPerm(): Boolean {
        if ((checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), ActivityBase.READ_EXTERNAL_STORAGE_PERMISSION)
        } else {
            // Permission granted
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            ActivityBase.READ_EXTERNAL_STORAGE_PERMISSION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (viewModel.delayedButtonClicked)
                        onButtonClicked(null)
                    return
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ActivityBase.READ_REQUEST_CODE && resultCode == AppCompatActivity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            var uri: Uri? = null
            if (data != null) {
                StateKeeper.imageFile = data.data

                nextStep()
            }
        }
    }

    private fun nextStep() {
        val intent = Intent(requireContext(), UsbDrivePickerActivity::class.java)
        startActivity(intent)
    }
}