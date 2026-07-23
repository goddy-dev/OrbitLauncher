package com.godwin.nyumbanilauncher.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.godwin.nyumbanilauncher.R
import com.godwin.nyumbanilauncher.model.AppInfo
import com.godwin.nyumbanilauncher.model.GridItem
import com.godwin.nyumbanilauncher.util.AppRepository
import com.godwin.nyumbanilauncher.util.PrefsManager

/**
 * Renders the home screen grid: each slot is either an app icon or a folder icon.
 * Supports drag-reorder (via the RecyclerView's ItemTouchHelper attached in MainActivity)
 * and long-press for a context action (handled by the click listeners below).
 */
class HomeGridAdapter(
    private val context: Context,
    private val items: MutableList<GridItem>,
    private val prefs: PrefsManager,
    private val onAppClick: (AppInfo) -> Unit,
    private val onFolderClick: (GridItem) -> Unit,
    private val onItemLongPress: (GridItem, Int) -> Unit
) : RecyclerView.Adapter<HomeGridAdapter.ViewHolder>() {

    private val pm = context.packageManager

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: android.widget.ImageView = view.findViewById(R.id.iconImage)
        val label: android.widget.TextView = view.findViewById(R.id.iconLabel)
        val container: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.label.visibility = if (prefs.iconLabelsVisible) View.VISIBLE else View.GONE

        if (item.isFolder) {
            holder.label.text = item.folderName
            holder.icon.setImageResource(R.drawable.ic_folder)
            holder.container.setOnClickListener { onFolderClick(item) }
        } else {
            val app: AppInfo? = item.appKey?.let { AppRepository.findByKey(context, it) }
            if (app != null) {
                holder.label.text = app.label
                holder.icon.setImageDrawable(loadIcon(app))
                holder.container.setOnClickListener { onAppClick(app) }
            } else {
                // App was uninstalled since layout was saved.
                holder.label.text = context.getString(R.string.unknown_app)
                holder.icon.setImageResource(R.drawable.ic_folder)
                holder.container.setOnClickListener(null)
            }
        }

        holder.container.setOnLongClickListener {
            onItemLongPress(item, position)
            true
        }
    }

    private fun loadIcon(app: AppInfo): Drawable? {
        return try {
            pm.getActivityIcon(android.content.ComponentName(app.packageName, app.activityName))
        } catch (e: Exception) {
            null
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<GridItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun moveItem(from: Int, to: Int) {
        val moved = items.removeAt(from)
        items.add(to, moved)
        items.forEachIndexed { index, gridItem -> gridItem.position = index }
        notifyItemMoved(from, to)
    }
}
