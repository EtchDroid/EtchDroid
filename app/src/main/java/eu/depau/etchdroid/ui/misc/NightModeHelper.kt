package eu.depau.etchdroid.ui.misc

import android.content.SharedPreferences
import android.content.res.Configuration
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import java.lang.ref.WeakReference


/**
 * Night Mode Helper
 *
 * Adapted from https://gist.github.com/slightfoot/c508cdc8828a478572e0
 *
 * Helps use utilise the night and notnight resource qualifiers without
 * being in car or dock mode.
 *
 *
 * Implementation is simple. Add the follow line at the top of your
 * activity's onCreate just after the super.onCreate(); The idea here
 * is to do it before we create any views. So the new views will use
 * the correct Configuration.
 *
 * <pre>
 * mNightModeHelper = new NightModeHelper(this, R.style.AppTheme);
</pre> *
 *
 * You can now use your instance of NightModeHelper to control which mode
 * you are in. You can choose to persist the current setting and hand
 * it back to this class as the defaultUiMode, otherwise this is done
 * for you automatically.
 *
 *
 * I'd suggest you setup your Theme as follows:
 *
 *  *
 * **res\values\styles.xml**
 * <pre>&lt;style name=&quot;AppTheme&quot; parent=&quot;AppBaseTheme&quot;&gt;&lt;/style&gt;</pre>
 *
 *  *
 * **res\values-night\styles.xml**
 * <pre>&lt;style name=&quot;AppBaseTheme&quot; parent=&quot;@android:style/Theme.Holo&quot;&gt;&lt;/style&gt;</pre>
 *
 *  *
 * **res\values-notnight\styles.xml**
 * <pre>&lt;style name=&quot;AppBaseTheme&quot; parent=&quot;@android:style/Theme.Holo.Light&quot;&gt;&lt;/style&gt;</pre>
 *
 * @author Simon Lightfoot <simon></simon>@demondevelopers.com>
 */
class NightModeHelper {

    private var mActivity: WeakReference<AppCompatActivity>? = null
    lateinit var mPrefs: SharedPreferences

    val nightMode: Boolean
        get() = uiNightMode == Configuration.UI_MODE_NIGHT_YES

    private val PREF_KEY = "nightModeState"

    companion object {
        var uiNightMode = Configuration.UI_MODE_NIGHT_UNDEFINED
    }

    /**
     * Default behaviour is to automatically save the setting and restore it.
     */
    constructor(activity: AppCompatActivity, theme: Int) {
        val currentMode = activity.resources.configuration
                .uiMode and Configuration.UI_MODE_NIGHT_MASK
        mPrefs = PreferenceManager.getDefaultSharedPreferences(activity)
        init(activity, theme, mPrefs.getInt(PREF_KEY, currentMode))
    }

    /**
     * If you don't want the autoSave feature and instead want to provide
     * your own persisted storage for the mode, use the defaultUiMode for it.
     */
    constructor(activity: AppCompatActivity, theme: Int, defaultUiMode: Int) {
        init(activity, theme, defaultUiMode)
    }

    private fun init(activity: AppCompatActivity, theme: Int, defaultUiMode: Int) {
        mActivity = WeakReference(activity)
        if (uiNightMode == Configuration.UI_MODE_NIGHT_UNDEFINED) {
            uiNightMode = defaultUiMode
        }
        updateConfig(uiNightMode)

        // This may seem pointless but it forces the Theme to be reloaded
        // with new styles that would change due to new Configuration.
        activity.setTheme(theme)
    }

    private fun updateConfig(uiNightMode: Int) {
        val activity = mActivity!!.get()
                ?: throw IllegalStateException("Activity went away while switching theme")
        val newConfig = Configuration(activity.resources.configuration)
        newConfig.uiMode = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()
        newConfig.uiMode = newConfig.uiMode or uiNightMode
        activity.resources.updateConfiguration(newConfig, null)
        Companion.uiNightMode = uiNightMode
        mPrefs.edit()?.putInt(PREF_KEY, Companion.uiNightMode)?.apply()
    }

    fun toggle() {
        when (uiNightMode) {
            Configuration.UI_MODE_NIGHT_YES -> notNight()
            else -> night()
        }
    }

    fun notNight() {
        updateConfig(Configuration.UI_MODE_NIGHT_NO)
        mActivity!!.get()!!.recreate()
    }

    fun night() {
        updateConfig(Configuration.UI_MODE_NIGHT_YES)
        mActivity!!.get()!!.recreate()
    }
}