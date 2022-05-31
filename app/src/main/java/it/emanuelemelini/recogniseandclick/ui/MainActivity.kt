package it.emanuelemelini.recogniseandclick.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import it.emanuelemelini.recogniseandclick.*
import it.emanuelemelini.recogniseandclick.services.ScreenCaptureService
import it.emanuelemelini.recogniseandclick.services.autoClickService
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


private const val PERMISSION_CODE = 110

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE = 100
        private var testo: String = ""

        fun getTesto(): String {
            return testo
        }

    }

    private var root: FrameLayout? = null

    private lateinit var text: EditText
    private lateinit var textScelto: TextView
    private lateinit var backBtn: Button
    private lateinit var textTop: TextView

    private lateinit var shared: SharedPreferences
    private var mutRecents: MutableSet<String> = mutableSetOf("")
    private var recents: Set<String> = emptySet()
    private lateinit var mContext: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        root = findViewById(R.id.root_layout)
        text = findViewById(R.id.text_to_search)
        textScelto = findViewById(R.id.text_testo_scelto)

        backBtn = findViewById(R.id.top_bar_back_btn)
        textTop = findViewById(R.id.top_bar_text)

        //testDisplay = findViewById(R.id.testDisplay);
        EventBus.getDefault().register(this)

        shared = getSharedPreferences("recognise_shared", MODE_PRIVATE)

    }

    override fun onResume() {
        super.onResume()

        mContext = this

        val btnStr = "Recents"
        backBtn.text = btnStr
        val topStr = "Home"
        textTop.text = topStr

        recents = shared.getStringSet("recents", null) ?: emptySet();

        val hasPermission = checkAccess()
        "has access? $hasPermission".logd()
        if (!hasPermission) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        if (!Settings.canDrawOverlays(this)) {
            askPermission()
        }

        backBtn.setOnClickListener {
            startActivity(Intent(mContext, RecentsActivity::class.java))
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)

        autoClickService?.let {
            "stop auto click service".logd()
            it.stopSelf()
            return it.disableSelf()
        }
    }

    //this will get called when the service sends the bounding boxes
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(boxes: BoundingBoxes) {
        for (box in boxes.boxes) {
            val left = box.left
            val top = box.top
            val right = box.right
            val bottom = box.bottom
            val location = IntArray(2)
            root!!.getLocationOnScreen(location)

            autoClickService?.click(right + 10, bottom + 10)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startService(ScreenCaptureService.getStartIntent(this, resultCode, data))
            }
        }
    }

    fun startProjection(view: View?) {
        if (!text.text.isNullOrBlank())
            if (Settings.canDrawOverlays(this)) {
                testo = text.text.toString()

                shared.addStringToSet("recents", testo)

                (shared.getStringSet("recents", null) ?: emptySet()).stream().map {
                    "SharedPreferences-> $it".logd()
                    println("SharedPreferences-> $it")
                }

                /*recents = shared.getStringSet("recents", null) ?: emptySet();
                if (recents.isEmpty())
                    recents = setOf(testo)
                if (recents.stream().noneMatch { it.equals(testo) }) {
                    with(shared.edit()) {
                        remove("shared")
                        mutRecents = recents.toMutableSet()
                        mutRecents.add(testo)
                        recents = mutRecents.toSet()
                        putStringSet("shared", recents)
                    }
                }*/

                textScelto.visibility = View.VISIBLE
                val str = "Testo scelto: ${testo.censor()}"
                textScelto.text = str
                text.text.clear()
                //btn.text = testo
                val mProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE)
            } else {
                askPermission()
            }
        else
            Toast.makeText(this, "Per continuare inserisci il testo da cercare", Toast.LENGTH_LONG).show()
    }

    fun stopProjection(view: View?) {
        startService(ScreenCaptureService.getStopIntent(this))
        textScelto.visibility = View.GONE
    }

    private fun askPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, PERMISSION_CODE)
    }


    private fun checkAccess(): Boolean {
        val string = getString(R.string.accessibility_service_id)
        val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val list = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        for (id in list) {
            if (string == id.id) {
                return true
            }
        }
        return false
    }

}
