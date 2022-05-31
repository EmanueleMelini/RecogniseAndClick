package it.emanuelemelini.recogniseandclick.services


import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.OrientationEventListener
import android.view.WindowManager
import androidx.core.util.Pair
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import it.emanuelemelini.recogniseandclick.BoundingBoxes
import it.emanuelemelini.recogniseandclick.ui.MainActivity
import org.greenrobot.eventbus.EventBus


class ScreenCaptureService : Service() {


    private var mMediaProjection: MediaProjection? = null
    private var mImageReader: ImageReader? = null
    private var mHandler: Handler? = null
    private var mDisplay: Display? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mDensity = 0
    private var mWidth = 0
    private var mHeight = 0
    private var mRotation = 0
    private var mOrientationChangeCallback: OrientationChangeCallback? = null

    private var testo: String = "scacia"

    //Only true when the text recognition is not busy and ready to take an image
    @Volatile
    var readyToProcess = false

    companion object {

        private const val TAG = "ScreenCaptureService"
        private const val RESULT_CODE = "RESULT_CODE"
        private const val DATA = "DATA"
        private const val ACTION = "ACTION"
        private const val START = "START"
        private const val STOP = "STOP"
        private const val SCREENCAP_NAME = "screencap"

        private var IMAGES_PRODUCED = 0

        fun getStartIntent(context: Context?, resultCode: Int, data: Intent?): Intent {
            val intent = Intent(context, ScreenCaptureService::class.java)
            intent.putExtra(ACTION, START)
            intent.putExtra(RESULT_CODE, resultCode)
            intent.putExtra(DATA, data)
            return intent
        }

        fun getStopIntent(context: Context?): Intent {
            val intent = Intent(context, ScreenCaptureService::class.java)
            intent.putExtra(ACTION, STOP)
            return intent
        }

        private fun isStartCommand(intent: Intent): Boolean {
            return (intent.hasExtra(RESULT_CODE) && intent.hasExtra(DATA)
                    && intent.hasExtra(ACTION) && intent.getStringExtra(ACTION) == START)
        }

        private fun isStopCommand(intent: Intent): Boolean {
            return intent.hasExtra(ACTION) && intent.getStringExtra(ACTION) == STOP
        }

        private fun getVirtualDisplayFlags(): Int {
            return DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
        }

    }

    private inner class ImageAvailableListener : OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {
            var bitmap: Bitmap
            try {
                mImageReader?.acquireLatestImage().use { image ->
                    if (image != null) {
                        val planes = image.planes
                        val buffer = planes[0].buffer
                        val pixelStride = planes[0].pixelStride
                        val rowStride = planes[0].rowStride
                        val rowPadding = rowStride - pixelStride * mWidth

                        // create bitmap
                        bitmap = Bitmap.createBitmap(
                            mWidth + rowPadding / pixelStride,
                            mHeight,
                            Bitmap.Config.ARGB_8888
                        )
                        bitmap.copyPixelsFromBuffer(buffer)

                        //give screenshot to text recognizer, if ready
                        if (readyToProcess) {
                            readyToProcess = false
                            processScreenshot(bitmap)
                        }
                        IMAGES_PRODUCED++
                        Log.e(TAG, "captured image: $IMAGES_PRODUCED")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun processScreenshot(bitmap: Bitmap) {
        testo = MainActivity.getTesto()
        val testoLs = testo.split(" ")
        val multi = testoLs.size > 1
        var c = 0
        var multiFound = false
        EventBus.getDefault().post(bitmap) //send bitmap for display
        val boundingBoxes = BoundingBoxes() //this object will be sent
        val image: InputImage = InputImage.fromBitmap(bitmap, 0)
        val recognizer: TextRecognizer = TextRecognition.getClient()
        recognizer.process(image).addOnSuccessListener { text ->
            Log.d("Test", "Screenshot processed. " + text.textBlocks.size)
            for (block in text.textBlocks) {
                for (line in block.lines) {
                    for (element in line.elements) {
                        Log.d("Test", "Word found: " + element.text)
                        if (!multi) {
                            if (testo.equals(element.text, ignoreCase = true)) {
                                val box: Rect = element.boundingBox ?: return@addOnSuccessListener
                                boundingBoxes.add(box) //save the box
                            }
                        } else {
                            if (testoLs[c].equals(element.text, ignoreCase = true) && !multiFound) {
                                c++
                                if (c == testoLs.size)
                                    multiFound = true

                                if (multiFound) {
                                    val box: Rect = element.boundingBox ?: return@addOnSuccessListener
                                    boundingBoxes.add(box) //save the box
                                    multiFound = false
                                    c = 0
                                }
                            } else {
                                c = 0
                            }
                        }
                    }
                }
            }
            EventBus.getDefault().post(boundingBoxes) //send the boxes back to activity
            readyToProcess = true //ready for another image
        }
    }

    inner class OrientationChangeCallback(context: Context?) : OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            val rotation = mDisplay?.rotation ?: return
            if (rotation != mRotation) {
                mRotation = rotation
                try {
                    // clean up
                    if (mVirtualDisplay != null)
                        mVirtualDisplay?.release()
                    if (mImageReader != null)
                        mImageReader?.setOnImageAvailableListener(null, null)

                    // re-create virtual display depending on device width / height
                    createVirtualDisplay()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    inner class MediaProjectionStopCallback : MediaProjection.Callback() {
        override fun onStop() {
            Log.e(TAG, "stopping projection.")
            mHandler?.post {
                if (mVirtualDisplay != null)
                    mVirtualDisplay?.release()
                if (mImageReader != null)
                    mImageReader?.setOnImageAvailableListener(null, null)
                if (mOrientationChangeCallback != null)
                    mOrientationChangeCallback?.disable()
                mMediaProjection?.unregisterCallback(this@MediaProjectionStopCallback)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        // start capture handling thread
        object : Thread() {
            override fun run() {
                Looper.prepare()
                mHandler = Handler()
                Looper.loop()
            }
        }.start()
        readyToProcess = true
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (isStartCommand(intent)) {
            // create notification
            val notification: Pair<Int, Notification> = NotificationUtils.getNotification(this)
            startForeground(notification.first, notification.second)
            // start projection
            val resultCode = intent.getIntExtra(RESULT_CODE, Activity.RESULT_CANCELED)
            val data = intent.getParcelableExtra<Intent>(DATA)
            startProjection(resultCode, data!!)
        } else if (isStopCommand(intent)) {
            stopProjection()
            stopSelf()
        } else {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startProjection(resultCode: Int, data: Intent) {
        val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        if (mMediaProjection == null) {
            mMediaProjection = mpManager.getMediaProjection(resultCode, data)
            if (mMediaProjection != null) {
                // display metrics
                mDensity = Resources.getSystem().displayMetrics.densityDpi
                val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                mDisplay = windowManager.defaultDisplay

                // create virtual display depending on device width / height
                createVirtualDisplay()

                // register orientation change callback
                mOrientationChangeCallback = OrientationChangeCallback(this)
                if (mOrientationChangeCallback!!.canDetectOrientation()) {
                    mOrientationChangeCallback?.enable()
                }

                // register media projection stop callback
                mMediaProjection?.registerCallback(MediaProjectionStopCallback(), mHandler)
            }
        }
    }

    private fun stopProjection() {
        if (mHandler != null) {
            mHandler?.post {
                if (mMediaProjection != null) {
                    mMediaProjection?.stop()
                }
            }
        }
    }

    @SuppressLint("WrongConstant")
    fun createVirtualDisplay() {
        // get width and height
        val wm = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val size = Point()
        display.getRealSize(size)
        mWidth = size.x
        mHeight = size.y

        // start capture reader
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2)
        mVirtualDisplay = mMediaProjection?.createVirtualDisplay(
            SCREENCAP_NAME, mWidth, mHeight,
            mDensity, getVirtualDisplayFlags(), mImageReader?.surface, null, mHandler
        )
        mImageReader?.setOnImageAvailableListener(ImageAvailableListener(), mHandler)
    }

}