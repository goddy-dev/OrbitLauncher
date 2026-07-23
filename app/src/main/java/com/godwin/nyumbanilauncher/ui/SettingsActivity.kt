package com.godwin.nyumbanilauncher.ui

import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.godwin.nyumbanilauncher.databinding.ActivitySettingsBinding
import com.godwin.nyumbanilauncher.util.BackupManager
import com.godwin.nyumbanilauncher.util.PrefsManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: PrefsManager

    // A small, friendly palette rather than a full color picker — keeps the UI simple.
    private val palette = listOf(
        0xFF6750A4.toInt(), // purple
        0xFF1E88E5.toInt(), // blue
        0xFF43A047.toInt(), // green
        0xFFE53935.toInt(), // red
        0xFFFB8C00.toInt(), // orange
        0xFF00897B.toInt()  // teal
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PrefsManager(this)

        setupGridControls()
        setupColorSwatches()
        setupLabelsToggle()
        setupGestureSpinners()
        setupBackupRestore()

        if (intent.getBooleanExtra("open_backup", false)) {
            binding.backupSection.requestFocus()
        }
    }

    private fun setupGridControls() {
        binding.columnsSeek.max = 8 - 3
        binding.columnsSeek.progress = prefs.columns - 3
        binding.columnsValue.text = prefs.columns.toString()

        binding.rowsSeek.max = 10 - 3
        binding.rowsSeek.progress = prefs.rows - 3
        binding.rowsValue.text = prefs.rows.toString()

        binding.columnsSeek.setOnSeekBarChangeListener(simpleSeekListener { progress ->
            val cols = progress + 3
            binding.columnsValue.text = cols.toString()
            prefs.columns = cols
        })

        binding.rowsSeek.setOnSeekBarChangeListener(simpleSeekListener { progress ->
            val rows = progress + 3
            binding.rowsValue.text = rows.toString()
            prefs.rows = rows
        })
    }

    private fun simpleSeekListener(onChange: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser) onChange(progress)
        }
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    private fun setupColorSwatches() {
        binding.colorSwatchGroup.removeAllViews()
        palette.forEach { color ->
            val swatch = android.widget.Button(this)
            swatch.layoutParams = android.widget.LinearLayout.LayoutParams(80, 80).apply {
                marginEnd = 16
            }
            swatch.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
            swatch.text = ""
            swatch.setOnClickListener {
                prefs.accentColor = color
                Toast.makeText(this, "Accent color updated", Toast.LENGTH_SHORT).show()
            }
            binding.colorSwatchGroup.addView(swatch)
        }
    }

    private fun setupLabelsToggle() {
        binding.labelsSwitch.isChecked = prefs.iconLabelsVisible
        binding.labelsSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.iconLabelsVisible = isChecked
        }
    }

    private fun setupGestureSpinners() {
        val actions = listOf(
            PrefsManager.ACTION_OPEN_DRAWER to "Open app drawer",
            PrefsManager.ACTION_NOTIFICATIONS to "Show notifications",
            PrefsManager.ACTION_SEARCH to "Search apps",
            PrefsManager.ACTION_NONE to "Nothing"
        )
        val labels = actions.map { it.second }
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)

        binding.swipeUpSpinner.adapter = adapter
        binding.swipeDownSpinner.adapter = adapter

        binding.swipeUpSpinner.setSelection(actions.indexOfFirst { it.first == prefs.swipeUpAction }.coerceAtLeast(0))
        binding.swipeDownSpinner.setSelection(actions.indexOfFirst { it.first == prefs.swipeDownAction }.coerceAtLeast(0))

        binding.swipeUpSpinner.onItemSelectedListener = spinnerListener { index ->
            prefs.swipeUpAction = actions[index].first
        }
        binding.swipeDownSpinner.onItemSelectedListener = spinnerListener { index ->
            prefs.swipeDownAction = actions[index].first
        }
    }

    private fun spinnerListener(onSelect: (Int) -> Unit) = object : android.widget.AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
            onSelect(position)
        }
        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
    }

    private fun setupBackupRestore() {
        binding.backupButton.setOnClickListener {
            val json = prefs.exportAll()
            val ok = BackupManager.saveBackup(this, json)
            Toast.makeText(
                this,
                if (ok) "Backup saved to Downloads/nyumbani_launcher_backup.json" else "Backup failed",
                Toast.LENGTH_LONG
            ).show()
        }

        binding.restoreButton.setOnClickListener {
            val json = BackupManager.readBackup(this)
            if (json == null) {
                Toast.makeText(this, "No backup file found in Downloads", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val ok = prefs.importAll(json)
            Toast.makeText(
                this,
                if (ok) "Restored — go back to Home to see it" else "Restore failed, file may be invalid",
                Toast.LENGTH_LONG
            ).show()
            if (ok) {
                setupGridControls()
                binding.labelsSwitch.isChecked = prefs.iconLabelsVisible
            }
        }
    }
}
