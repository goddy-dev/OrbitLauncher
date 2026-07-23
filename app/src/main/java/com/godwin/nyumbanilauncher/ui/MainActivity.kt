package com.godwin.nyumbanilauncher.ui

import android.app.AlertDialog
import android.app.StatusBarManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.godwin.nyumbanilauncher.R
import com.godwin.nyumbanilauncher.adapter.HomeGridAdapter
import com.godwin.nyumbanilauncher.databinding.ActivityMainBinding
import com.godwin.nyumbanilauncher.model.AppInfo
import com.godwin.nyumbanilauncher.model.GridItem
import com.godwin.nyumbanilauncher.util.AppRepository
import com.godwin.nyumbanilauncher.util.GestureHelper
import com.godwin.nyumbanilauncher.util.PrefsManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PrefsManager
    private lateinit var adapter: HomeGridAdapter
    private lateinit var gestureHelper: GestureHelper
    private var homeItems = mutableListOf<GridItem>()

    private val drawerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val key = result.data?.getStringExtra(AppDrawerActivity.EXTRA_PICKED_APP_KEY)
        if (result.resultCode == RESULT_OK && key != null) {
            addAppToHome(key)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)
        applyTheme()

        homeItems = prefs.loadLayout()
        setupGrid()
        setupGestures()

        binding.addAppFab.setOnClickListener { openDrawer() }
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
        adapter.notifyDataSetChanged()
    }

    private fun applyTheme() {
        binding.root.setBackgroundColor(prefs.backgroundColor)
        binding.addAppFab.backgroundTintList = android.content.res.ColorStateList.valueOf(prefs.accentColor)
    }

    private fun setupGrid() {
        val layoutManager = GridLayoutManager(this, prefs.columns)
        binding.homeGrid.layoutManager = layoutManager

        adapter = HomeGridAdapter(
            context = this,
            items = homeItems,
            prefs = prefs,
            onAppClick = { app -> AppRepository.launch(this, app) },
            onFolderClick = { folder -> showFolder(folder) },
            onItemLongPress = { item, position -> showItemOptions(item, position) }
        )
        binding.homeGrid.adapter = adapter

        // Drag-to-reorder support.
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                adapter.moveItem(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
                prefs.saveLayout(homeItems)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        })
        touchHelper.attachToRecyclerView(binding.homeGrid)
    }

    private fun setupGestures() {
        gestureHelper = GestureHelper(
            context = this,
            onSwipeUp = { handleAction(prefs.swipeUpAction) },
            onSwipeDown = { handleAction(prefs.swipeDownAction) },
            onLongPress = { showHomeOptions() }
        )
        binding.homeGrid.setOnTouchListener { _, event -> gestureHelper.onTouchEvent(event) }
        binding.root.setOnTouchListener { _, event -> gestureHelper.onTouchEvent(event) }
    }

    private fun handleAction(action: String) {
        when (action) {
            PrefsManager.ACTION_OPEN_DRAWER -> openDrawer()
            PrefsManager.ACTION_NOTIFICATIONS -> expandNotifications()
            PrefsManager.ACTION_SEARCH -> openDrawer(focusSearch = true)
            else -> { /* no-op */ }
        }
    }

    private fun expandNotifications() {
        // Requires OEM-granted notification-shade expansion; on modern Android this
        // is restricted, so we degrade gracefully if it's not available.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("WrongConstant")
                val statusBarService = getSystemService("statusbar")
                val statusBarManager = statusBarService as? StatusBarManager
                statusBarManager?.let {
                    val method = it.javaClass.getMethod("expandNotificationsPanel")
                    method.invoke(it)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, R.string.notifications_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openDrawer(focusSearch: Boolean = false) {
        val intent = Intent(this, AppDrawerActivity::class.java)
        intent.putExtra(AppDrawerActivity.EXTRA_FOCUS_SEARCH, focusSearch)
        intent.putExtra(AppDrawerActivity.EXTRA_PICK_MODE, false)
        drawerLauncher.launch(intent)
    }

    private fun addAppToHome(appKey: String) {
        val newItem = GridItem(position = homeItems.size, appKey = appKey)
        homeItems.add(newItem)
        prefs.saveLayout(homeItems)
        adapter.notifyItemInserted(homeItems.size - 1)
    }

    private fun showItemOptions(item: GridItem, position: Int) {
        val options = if (item.isFolder) {
            arrayOf(getString(R.string.rename_folder), getString(R.string.remove))
        } else {
            arrayOf(getString(R.string.make_folder), getString(R.string.remove))
        }
        AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when {
                    item.isFolder && which == 0 -> renameFolder(item)
                    !item.isFolder && which == 0 -> promptMakeFolder(item, position)
                    which == 1 -> removeItem(position)
                }
            }.show()
    }

    private fun removeItem(position: Int) {
        homeItems.removeAt(position)
        homeItems.forEachIndexed { index, gridItem -> gridItem.position = index }
        prefs.saveLayout(homeItems)
        adapter.notifyDataSetChanged()
    }

    private fun promptMakeFolder(item: GridItem, position: Int) {
        val input = android.widget.EditText(this).apply { hint = getString(R.string.folder_name_hint) }
        AlertDialog.Builder(this)
            .setTitle(R.string.make_folder)
            .setView(input)
            .setPositiveButton(R.string.ok) { _, _ ->
                val name = input.text.toString().ifBlank { getString(R.string.folder) }
                val folder = GridItem(
                    position = item.position,
                    folderName = name,
                    folderApps = mutableListOf(item.appKey ?: "")
                )
                homeItems[position] = folder
                prefs.saveLayout(homeItems)
                adapter.notifyItemChanged(position)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun renameFolder(item: GridItem) {
        val input = android.widget.EditText(this).apply { setText(item.folderName) }
        AlertDialog.Builder(this)
            .setTitle(R.string.rename_folder)
            .setView(input)
            .setPositiveButton(R.string.ok) { _, _ ->
                item.folderName = input.text.toString().ifBlank { item.folderName }
                prefs.saveLayout(homeItems)
                adapter.notifyDataSetChanged()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showFolder(folder: GridItem) {
        val apps: List<AppInfo> = folder.folderApps.mapNotNull { AppRepository.findByKey(this, it) }
        val labels = apps.map { it.label }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(folder.folderName)
            .setItems(labels) { _, which -> AppRepository.launch(this, apps[which]) }
            .show()
    }

    private fun showHomeOptions() {
        val options = arrayOf(getString(R.string.settings_title), getString(R.string.backup_restore))
        AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, SettingsActivity::class.java))
                    1 -> startActivity(Intent(this, SettingsActivity::class.java).putExtra("open_backup", true))
                }
            }.show()
    }

    // Home screen has no "back" destination — swallow it like a real launcher does.
    override fun onBackPressed() { /* intentionally empty */ }
}
