package it.emanuelemelini.recogniseandclick.ui

import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.emanuelemelini.recogniseandclick.R
import it.emanuelemelini.recogniseandclick.ui.rv.recents.RecentsAdapter

class RecentsActivity : AppCompatActivity() {

    companion object {
        const val UPDATE_SHARED = "it.emanuelemelini.recogniseandclick.action_update_shared"
    }

    private lateinit var backBtn: Button
    private lateinit var topText: TextView
    private lateinit var recyclerRec: RecyclerView

    private lateinit var shared: SharedPreferences
    private var items: ArrayList<String> = ArrayList()
    private var recents: Set<String> = emptySet()
    private lateinit var mAdapter: RecentsAdapter
    private lateinit var mContext: Context
    private lateinit var broadcastReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recents)

        mContext = this

        backBtn = findViewById(R.id.top_bar_back_btn)
        topText = findViewById(R.id.top_bar_text)
        recyclerRec = findViewById(R.id.recents_recycler)
        recyclerRec.layoutManager = LinearLayoutManager(this)

        refreshShared()

        broadcastReceiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when(intent?.action) {
                    UPDATE_SHARED -> {
                        refreshData()
                    }
                    else -> {
                        // ignore
                    }
                }
            }
        }

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, IntentFilter(UPDATE_SHARED))

    }

    private fun refreshShared() {
        shared = getSharedPreferences("recognise_shared", MODE_PRIVATE)
        recents = shared.getStringSet("recents", null) ?: emptySet()
        items.clear()
        items.addAll(recents)
    }

    private fun refreshData() {
        refreshShared()
        mAdapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()

        mContext = this

        val topStr = "Recents"

        topText.text = topStr

        refreshShared()

        mAdapter = RecentsAdapter(mContext, items, shared)
        recyclerRec.adapter = mAdapter

        backBtn.setOnClickListener {
            finish()
        }

    }

    override fun onDestroy() {
        super.onDestroy()

        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(broadcastReceiver)

    }

}