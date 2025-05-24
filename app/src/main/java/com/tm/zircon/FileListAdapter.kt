package com.tm.zircon

import android.content.Context
import android.graphics.Color
import android.view.*
import android.widget.BaseAdapter
import android.widget.TextView
import java.io.File

class FileListAdapter(
    private val context: Context,
    private val files: List<File>
) : BaseAdapter() {

    override fun getCount(): Int = files.size
    override fun getItem(position: Int): Any = files[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val file = files[position]
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)

        val title = view.findViewById<TextView>(android.R.id.text1)
        val subtitle = view.findViewById<TextView>(android.R.id.text2)

        title.text = if (file.isDirectory) "[${file.name}]" else file.name
        subtitle.text = if (file.isDirectory) {
            val count = file.listFiles()?.count { it.name.lowercase().endsWith(".mp3") } ?: 0
            context.getString(R.string.tracks_count, count)
        } else ""

        if (file.isDirectory) {
            val count = file.listFiles()?.count { it.name.lowercase().endsWith(".mp3") } ?: 0
            title.setTextColor(if (count == 0) Color.RED else Color.WHITE)
        }

        return view
    }
}
