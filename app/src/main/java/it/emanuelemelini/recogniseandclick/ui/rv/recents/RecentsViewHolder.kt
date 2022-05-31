package it.emanuelemelini.recogniseandclick.ui.rv.recents

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import it.emanuelemelini.recogniseandclick.R

class RecentsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    var removeBtn: Button
    var textItem: TextView

    init {
        removeBtn = itemView.findViewById(R.id.recents_item_remove_btn)
        textItem = itemView.findViewById(R.id.recents_item_text)
    }

}