package it.emanuelemelini.recogniseandclick.ui.rv.recents

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import it.emanuelemelini.recogniseandclick.R
import it.emanuelemelini.recogniseandclick.removeFromStringSet
import it.emanuelemelini.recogniseandclick.toast
import it.emanuelemelini.recogniseandclick.ui.RecentsActivity

class RecentsAdapter(private val mContext: Context, private val data: ArrayList<String>, private val shared: SharedPreferences) : RecyclerView.Adapter<RecentsViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentsViewHolder {
        return RecentsViewHolder(LayoutInflater.from(mContext).inflate(R.layout.recents_item, parent, false))
    }

    override fun onBindViewHolder(holder: RecentsViewHolder, position: Int) {
        val str = data[position]
        holder.textItem.text = str

        holder.removeBtn.setOnClickListener {
            mContext.toast(str)
            shared.removeFromStringSet("recents", data[position])
            val intent = Intent()
            intent.action = RecentsActivity.UPDATE_SHARED
            mContext.sendBroadcast(intent)
        }

    }

    override fun getItemCount() = data.size

}