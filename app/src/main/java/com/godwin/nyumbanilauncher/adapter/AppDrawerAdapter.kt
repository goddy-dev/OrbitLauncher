package com.godwin.nyumbanilauncher.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.godwin.nyumbanilauncher.R
import com.godwin.nyumbanilauncher.model.AppInfo

class AppDrawerAdapter(
    private val context: Context,
    private val allApps: List<AppInfo>,
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongPress: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppDrawerAdapter.ViewHolder>(), Filterable {

    private var visibleApps: List<AppInfo> = allApps
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
        val app = visibleApps[position]
        holder.label.visibility = View.VISIBLE
        holder.label.text = app.label
        holder.icon.setImageDrawable(
            try {
                pm.getActivityIcon(android.content.ComponentName(app.packageName, app.activityName))
            } catch (e: Exception) {
                null
            }
        )
        holder.container.setOnClickListener { onAppClick(app) }
        holder.container.setOnLongClickListener {
            onAppLongPress(app)
            true
        }
    }

    override fun getItemCount(): Int = visibleApps.size

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val query = constraint?.toString()?.trim()?.lowercase().orEmpty()
            val filtered = if (query.isEmpty()) allApps
            else allApps.filter { it.label.lowercase().contains(query) }
            return FilterResults().apply { values = filtered; count = filtered.size }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            visibleApps = (results?.values as? List<AppInfo>) ?: allApps
            notifyDataSetChanged()
        }
    }
}
