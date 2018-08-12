package eu.depau.ddroid.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import eu.depau.ddroid.R
import eu.depau.ddroid.abc.WizardActivity
import eu.depau.ddroid.abc.WizardFragment
import eu.depau.ddroid.fragments.FlashMethodFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : WizardActivity() {
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
    }

    override fun goToNewFragment(fragment: WizardFragment) {
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
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
