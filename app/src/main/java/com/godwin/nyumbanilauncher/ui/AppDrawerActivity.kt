package com.godwin.nyumbanilauncher.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.godwin.nyumbanilauncher.adapter.AppDrawerAdapter
import com.godwin.nyumbanilauncher.databinding.ActivityAppDrawerBinding
import com.godwin.nyumbanilauncher.util.AppRepository
import com.godwin.nyumbanilauncher.util.PrefsManager

/**
 * Shows every launchable app on the device with a search box.
 * Tapping an app either launches it directly, or (when opened in "pick mode"
 * from the home-screen + button) returns its key to MainActivity to be added
 * to the home grid.
 */
class AppDrawerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppDrawerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppDrawerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = PrefsManager(this)
        val pickMode = intent.getBooleanExtra(EXTRA_PICK_MODE, true)
        val allApps = AppRepository.getAllLaunchableApps(this)

        val adapter = AppDrawerAdapter(
            context = this,
            allApps = allApps,
            onAppClick = { app ->
                if (pickMode) {
                    setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_PICKED_APP_KEY, app.key))
                    finish()
                } else {
                    AppRepository.launch(this, app)
                    finish()
                }
            },
            onAppLongPress = { app ->
                // Long-press in the drawer always offers "add to home".
                setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_PICKED_APP_KEY, app.key))
                finish()
            }
        )

        binding.appDrawerList.layoutManager = GridLayoutManager(this, prefs.columns)
        binding.appDrawerList.adapter = adapter

        binding.searchBox.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { adapter.filter.filter(s) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        if (intent.getBooleanExtra(EXTRA_FOCUS_SEARCH, false)) {
            binding.searchBox.requestFocus()
        }
    }

    companion object {
        const val EXTRA_PICKED_APP_KEY = "picked_app_key"
        const val EXTRA_PICK_MODE = "pick_mode"
        const val EXTRA_FOCUS_SEARCH = "focus_search"
    }
}
