package eu.depau.etchdroid.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import eu.depau.etchdroid.R
import eu.depau.etchdroid.kotlin_exts.toast
import eu.depau.etchdroid.utils.NightModeHelper


abstract class ActivityBase : AppCompatActivity() {
    protected lateinit var nightModeHelper: NightModeHelper
    val DISMISSED_DIALOGS_PREFS = "dismissed_dialogs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nightModeHelper = NightModeHelper(this, R.style.AppTheme)
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
            R.id.action_reset_warnings -> {
                getSharedPreferences(DISMISSED_DIALOGS_PREFS, 0)
                        .edit().clear().apply()
                toast(getString(R.string.warnings_reset))
                return true
            }
            R.id.action_nightmode -> {
                nightModeHelper.toggle()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}