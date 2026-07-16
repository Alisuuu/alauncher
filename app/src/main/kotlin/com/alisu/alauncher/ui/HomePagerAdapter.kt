package com.alisu.alauncher.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alisu.alauncher.R

class HomePagerAdapter(
    private val onBindWidgets: (View) -> Unit,
    private val onBindDesktop: (View) -> Unit,
    private val onBindLibrary: (View) -> Unit
) : RecyclerView.Adapter<HomePagerAdapter.PageViewHolder>() {

    override fun getItemCount(): Int = 3

    override fun getItemViewType(position: Int): Int = position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val layoutRes = when (viewType) {
            0 -> R.layout.page_widgets
            1 -> R.layout.page_home_desktop
            else -> R.layout.page_app_library
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        when (position) {
            0 -> onBindWidgets(holder.itemView)
            1 -> onBindDesktop(holder.itemView)
            2 -> onBindLibrary(holder.itemView)
        }
    }

    class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
