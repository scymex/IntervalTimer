package verseczi.intervaltimer

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import verseczi.intervaltimer.backgroundTask.clickerThread
import verseczi.intervaltimer.data.Database
import verseczi.intervaltimer.helpers.NotificationHelper

class clickingService : Service(), SensorEventListener {
    lateinit var mContext: Context
    internal var mBinder: IBinder = LocalBinder()
    private var windowManager: WindowManager? = null
    private lateinit var db: Database
    private var shot_counter: TextView? = null
    private var delay_counter: TextView? = null
    private var notify: NotificationHelper? = null
    private lateinit var clickerThread: clickerThread

    val params_delay: WindowManager.LayoutParams = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_PHONE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT)
    val params_counter: WindowManager.LayoutParams = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_PHONE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT)

    private var mCount_shoots: Int = 0
    private var landscape_mode: Boolean = false
    private var orientation: Int = 1

    private var _imgQty: Int = 0
    private var _delayed: Boolean = false

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    inner class LocalBinder : Binder() {
        fun getServerInstance(): clickingService {
            return this@clickingService
        }
    }



    override fun onCreate() {
        super.onCreate()
        mContext = this
        db = Database(mContext)
        shot_counter = TextView(mContext)
        delay_counter = TextView(mContext)
        notify = NotificationHelper(mContext)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)

        if (proximitySensor == null)
            stopSelf()
        else
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)

        params_counter.gravity = Gravity.TOP or Gravity.LEFT
        params_counter.x = 0
        params_counter.y = 100

        shot_counter?.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        shot_counter?.textSize = 15f
        shot_counter?.setShadowLayer(3f, 1f, 1f, Color.BLACK)
        shot_counter?.typeface = Typeface.DEFAULT_BOLD

        windowManager?.addView(shot_counter, params_counter)

        params_delay.gravity = Gravity.CENTER or Gravity.CENTER
        params_delay.x = 0
        params_delay.y = 0

        delay_counter?.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        delay_counter?.text = "250x"
        delay_counter?.textSize = 124f
        delay_counter?.setShadowLayer(3f, 1f, 1f, Color.BLACK)
        delay_counter?.typeface = Typeface.DEFAULT_BOLD
        delay_counter?.visibility = View.GONE

        windowManager?.addView(delay_counter, params_delay)

        // TODO: Át kell helyezni a számlálót és plusz info kell a többi dologról. (hátralévő idő stb)
    }

    fun startClicking() {
        clickerThread = clickerThread(mContext, mHandler)
        notify?.createNotification()
        clickerThread._imgQty = _imgQty
        clickerThread._delayed = _delayed
        clickerThread.start()
        shot_counter?.visibility = View.VISIBLE

    }

    fun resumeClicking() {
        clickerThread = clickerThread(mContext, mHandler)
        notify?.createNotification()
        clickerThread._imgQty = db.imgQty - mCount_shoots
        clickerThread._delayed = false
        clickerThread.start()
        shot_counter?.visibility = View.VISIBLE
    }

    fun stopClicking() {
        notify?.cancelNotification()
        clickerThread._cancelled = true
        delay_counter?.visibility = View.GONE
        shot_counter?.visibility = View.GONE
        setDelayCounter(-1)
    }

    fun getProgress(): Int {
        return mCount_shoots
    }

    fun stopService() {
        clickerThread._cancelled = true
        delay_counter?.visibility = View.GONE
        shot_counter?.visibility = View.GONE
        this.stopSelf()
    }

    var mHandler: Handler = object : Handler() {   //handles the INcoming msgs
        override fun handleMessage(msg: Message) {
            val _data = msg.data
            val cs = _data.getInt("currentProgress")
            val secs = _data.getInt("delay")
            if(!clickerThread._cancelled) {
                if (cs != mCount_shoots) {
                    val actualprogress = ((db.imgQty) - (clickerThread._imgQty)) + cs
                    mCount_shoots = actualprogress
                    updateProgress(actualprogress)
                } else if (secs != 0) {
                    setDelayCounter(secs)
                }
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val cs = intent.getIntExtra("count_shoots", 0)
            val secs = intent.getIntExtra("delay", 0)

            if (cs != mCount_shoots) {
                val actualprogress = ((db.imgQty) - (clickerThread._imgQty)) + cs
                mCount_shoots = actualprogress
                updateProgress(actualprogress)
            } else if (secs != 0) {
                setDelayCounter(secs)
            }
        }
    }

    fun updateProgress(pr: Int) {
        Log.i("IntervalTimer", "SETPROGRESS: $pr r/ ${db.imgQty})")
        shot_counter?.text = "$pr / ${db.imgQty}"
        notify?.progressUpdate("$pr / ${db.imgQty}")
    }

    fun setDelayCounter(pr: Int) {
        if (pr == db.delay) delay_counter?.visibility = View.VISIBLE
        if (pr == -1) delay_counter?.visibility = View.GONE

        delay_counter?.text = " $pr " // TODO: I had to add two spaces to this, because if the text is not wide enough it gets cut on the top and the bottom. (i didnt find a FASTER and better solution)a
    }

    override fun onSensorChanged(event: SensorEvent) {
        val roll = Math.abs(event.values[2])
        var rotation_m = 0
        if (landscape_mode)
            rotation_m = -90

        if (roll < 35) {
            if (event.values[1] < -10) { // PORTRAIT
                if (orientation != 1) {
                    delay_counter?.rotation = (0 + rotation_m).toFloat()
                    orientation = 1
                }
            } else if (event.values[1] > 10) { // REVERSE PORTRAIT
                if (orientation != 2) {
                    delay_counter?.rotation = (180 + rotation_m).toFloat()
                    orientation = 2
                }
            }
        } else {
            if (event.values[2] < -10) { // LANDSCAPE
                if (orientation != 3) {
                    delay_counter?.rotation = (270 + rotation_m).toFloat()
                    orientation = 3
                }
            } else if (event.values[2] > 10) { // REVERSE LANDSCAPE
                if (orientation != 4) {
                    delay_counter?.rotation = (90 + rotation_m).toFloat()
                    orientation = 4
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            landscape_mode = true
            orientation = 0
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            landscape_mode = false
            orientation = 0
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        _imgQty = intent.getIntExtra("imgqty", db.imgQty)
        _delayed = intent.getBooleanExtra("delayed", db.delayed)

        return super.onStartCommand(intent, flags, startId)
    }
}

