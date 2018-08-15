package eu.depau.etchdroid.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import eu.depau.etchdroid.R
import eu.depau.etchdroid.StateKeeper
import eu.depau.etchdroid.abc.WizardActivity
import eu.depau.etchdroid.abc.WizardFragment
import eu.depau.etchdroid.utils.getFileName
import eu.depau.etchdroid.utils.snackbar
import eu.depau.etchdroid.values.FlashMethod
import eu.depau.etchdroid.values.ImageLocation
import eu.depau.etchdroid.values.WizardStep
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_select_location.*
import kotlinx.android.synthetic.main.wizard_fragment_layout.*


/**
 * A placeholder fragment containing a simple view.
 */
class ImageLocationFragment : WizardFragment() {
    val READ_REQUEST_CODE = 42
    val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 29
    val TAG = "ImageLocationFragment"

    fun isStreamingAvailable(): Boolean {
        if (StateKeeper.imageLocation != ImageLocation.REMOTE)
            return false
        if (StateKeeper.flashMethod != FlashMethod.FLASH_DD && StateKeeper.flashMethod != FlashMethod.FLASH_API)
            return false
        return true
    }

    fun setStreamingCheckBoxAvailability(context: WizardActivity) {
        val checkBox = streaming_write_checkbox

        if (checkBox == null)
            return

        val curEnabled = checkBox.isEnabled
        var enabled = isStreamingAvailable()

        if (curEnabled != enabled) {
            checkBox.isEnabled = enabled
            onCheckBoxClicked(checkBox)
        }
    }

    override fun onCheckBoxClicked(view: View) {
        super.onCheckBoxClicked(view)

        if (view.id == R.id.streaming_write_checkbox)
            StateKeeper.streamingWrite = view.isActivated && view.isEnabled
    }

    override fun onRadioButtonClicked(view: View) {
        StateKeeper.imageLocation = when (view.id) {
            R.id.download_img_radio -> ImageLocation.REMOTE
            R.id.use_local_img_radio -> ImageLocation.LOCAL
            else -> null
        }

        fab?.show()

        pick_file_btn?.isEnabled = StateKeeper.imageLocation == ImageLocation.LOCAL
        img_url_textview?.isEnabled = StateKeeper.imageLocation == ImageLocation.REMOTE

        setStreamingCheckBoxAvailability(activity as WizardActivity)
        updateFileButtonLabel(activity as WizardActivity)
    }

    override fun onButtonClicked(view: View) {
        if (view.id == R.id.pick_file_btn) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.setType("*/*");
            activity?.startActivityForResult(intent, READ_REQUEST_CODE)
        }
    }

    override fun nextStep(view: View?) {
        if (StateKeeper.imageLocation == null) {
            view?.snackbar(getString(R.string.select_image_location))
            return
        }

        if (StateKeeper.imageLocation == ImageLocation.REMOTE) {
            try {
                StateKeeper.imageFile = getRemoteImageUri(activity as WizardActivity)
            } catch (e: RuntimeException) {
                Log.e(TAG, "Invalid URI specified", e)
                view?.snackbar(getString(R.string.provided_url_invalid))
                return
            }
        }

        if (StateKeeper.imageFile == null) {
            view?.snackbar(getString(R.string.provide_image_file))
            return
        }

        if (StateKeeper.imageLocation == ImageLocation.REMOTE && !StateKeeper.streamingWrite) {
            // Request permission to download file
            if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Permission is not granted
                ActivityCompat.requestPermissions(activity!!,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)
                return
            }


        }
        (activity as WizardActivity).goToNewFragment(UsbDriveFragment())
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    nextStep(fragment_layout)
                } else {
                    Snackbar.make(fragment_layout, getString(R.string.storage_perm_required_explaination), Snackbar.LENGTH_LONG).show()
                }
                return
            }

            else -> {
            }
        }

    }

    override fun onFragmentAdded(activity: WizardActivity) {
        super.onFragmentAdded(activity)
        setStreamingCheckBoxAvailability(activity)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        StateKeeper.currentFragment = this
        StateKeeper.wizardStep = WizardStep.SELECT_LOCATION

        return inflater.inflate(R.layout.fragment_select_location, container, false)
    }

    fun getRemoteImageUri(context: WizardActivity): Uri {
        val text = img_url_textview.text.toString()
        return Uri.parse(text)
    }

    fun updateFileButtonLabel(context: WizardActivity) {
        val button = pick_file_btn
        val uri = StateKeeper.imageFile

        val text = uri?.getFileName(context)

        if (text != null)
            button.text = text
        else
            button.text = getString(R.string.pick_a_file)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            var uri: Uri? = null
            if (data != null) {
                uri = data.getData()
                Log.d(TAG, "Uri: " + uri!!.toString())
                StateKeeper.imageFile = uri
                updateFileButtonLabel(activity as WizardActivity)
            }
        }

    }
}
