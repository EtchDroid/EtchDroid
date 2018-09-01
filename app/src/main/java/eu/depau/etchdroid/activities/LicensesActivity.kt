package eu.depau.etchdroid.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.MenuItem
import android.view.View
import eu.depau.etchdroid.R
import eu.depau.etchdroid.adapters.LicenseRecyclerViewAdapter
import eu.depau.etchdroid.utils.ClickListener
import eu.depau.etchdroid.utils.License
import eu.depau.etchdroid.utils.RecyclerViewTouchListener
import kotlinx.android.synthetic.main.activity_licenses.*


class LicensesActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var viewAdapter: LicenseRecyclerViewAdapter

    lateinit var licenses: Array<License>

    private fun updateLicenses() {
        if (!::licenses.isInitialized) {
            licenses = arrayOf(
                    License(getString(R.string.this_app), Uri.parse("https://github.com/Depau/EtchDroid"), getString(R.string.license_gpl3)),
                    License("Storage Chooser 2.0", Uri.parse("https://github.com/codekidX/storage-chooser"), getString(R.string.license_mpl_2_0), getString(R.string.storagechooser_license_description)),
                    License("libaums (fork)", Uri.parse("https://github.com/Depau/libaums"), getString(R.string.license_apache2_0), getString(R.string.libaums_license_desc)),
                    License("dmg2img (fork)", Uri.parse("https://github.com/Depau/dmg2img-cmake"), getString(R.string.license_gpl2), getString(R.string.dmg2img_license_desc)),
                    License("bzip2", Uri.parse("https://github.com/LuaDist/bzip2/"), getString(R.string.license_bzip2)),
                    License("LibreSSL", Uri.parse("https://github.com/libressl-portable/portable"), getString(R.string.license_custom))
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_licenses)
        updateLicenses()

        // Enable back button in action bar
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        viewManager = LinearLayoutManager(this)
        recyclerView = licenses_recycler_view
        viewAdapter = LicenseRecyclerViewAdapter(licenses)
        recyclerView.adapter = viewAdapter
        recyclerView.layoutManager = viewManager

        recyclerView.addOnItemTouchListener(RecyclerViewTouchListener(this, recyclerView, object : ClickListener {
            override fun onClick(view: View, position: Int) {
                val intent = Intent(Intent.ACTION_VIEW, viewAdapter.get(position).url)
                startActivity(intent)
            }

            override fun onLongClick(view: View, position: Int) {}
        }))
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
