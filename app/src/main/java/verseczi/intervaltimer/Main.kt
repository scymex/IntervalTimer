package verseczi.intervaltimer

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.widget.*
import java.util.concurrent.TimeUnit
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import verseczi.intervaltimer.backgroundTask.appChooser
import verseczi.intervaltimer.backgroundTask.rootAccess
import verseczi.intervaltimer.data.Database
import verseczi.intervaltimer.helpers.bindView


class Main : AppCompatActivity() {
    // Views
    //      @content_main_timelapse_calc.xml
    private val etImgQty: EditText by bindView(R.id.etIMG_NUM)
    private val etFPS: EditText by bindView(R.id.etFPS)
    private val etInterval: EditText by bindView(R.id.etInterval)
    //      @content_main_infocard
    private val tvImgQty: TextView by bindView(R.id.tvImg_num)
    private val tvClipLength: TextView by bindView(R.id.tvCliplength)
    private val tvDuration: TextView by bindView(R.id.tvDuration)
    //      @content_main_coordinate
    private val tvCoordX: TextView by bindView(R.id.tv_CoordX)
    private val tvCoordY: TextView by bindView(R.id.tv_CoordY)
    //      @content_main_delay
    private val etDelay: EditText by bindView(R.id.etDelay)
    private val swDelay: Switch by bindView(R.id.delayed_switch)
    //      @content_main_repeat
    private val swEndlessrepeat: Switch by bindView(R.id.endless_switch)
    //      @content_main_app_chooser
    private val ivAppIcon: ImageView by bindView(R.id.app_icon_n)
    private val tvAppName: TextView by bindView(R.id.app_name_n)
    private val tvAppPackageName: TextView by bindView(R.id.package_name_n)
    // Buttons
    private val bnStart: Button by bindView(R.id.start_button)
    private val bnCoordinates: Button by bindView(R.id.getcoordinates)
    private val bnChooseApp: Button by bindView(R.id.choose_app)
    private val bnDelayMinus: Button by bindView(R.id.delay_minus)
    private val bnDelayPlus: Button by bindView(R.id.delay_plus)

    // Text of the views
    var _ImgQty: Int
        get() = etImgQty.text.toString().toInt()
        set(value) {
            etImgQty.text = SpannableStringBuilder("$value")
        }

    var _Interval: Int
        get() = etInterval.text.toString().toInt()
        set(value) {
            etInterval.text = SpannableStringBuilder("$value")
        }

    var _FPS: Int
        get() = etFPS.text.toString().toInt()
        set(value) {
            etFPS.text = SpannableStringBuilder("$value")
        }

    var _tvImgQty: Int
        get() = tvImgQty.text.toString().toInt()
        set(value) {
            tvImgQty.text = value.toString()
        }

    var _ClipLength: Int
        get() {
            val durationRegex = tvClipLength.text.toString().split(":".toRegex())
            return durationRegex[0].toInt() * 3600 + durationRegex[1].toInt() * 60 + durationRegex[2].toInt()
        }
        set(value) {
            tvClipLength.text = formatTime(value)
        }

    var _Duration: Int
        get() {
            val durationRegex = tvDuration.text.toString().split(":".toRegex())
            return durationRegex[0].toInt() * 3600 + durationRegex[1].toInt() * 60 + durationRegex[2].toInt()
        }
        set(value) {
            tvDuration.text = formatTime(value)
        }

    var _coordX: Int
        get() = tvCoordX.text.toString().toInt()
        set(value) {
            tvCoordX.text = SpannableStringBuilder("$value")
        }

    var _coordY: Int
        get() = tvCoordY.text.toString().toInt()
        set(value) {
            tvCoordY.text = SpannableStringBuilder("$value")
        }

    var _delay: Int
        get() = etDelay.text.toString().toInt()
        set(value) {
            etDelay.text = SpannableStringBuilder("$value")
        }

    var _delayed: Boolean
        get() = swDelay.isChecked
        set(value) {
            swDelay.isChecked = value
        }

    var _endlessRepeat: Boolean
        get() = swEndlessrepeat.isChecked
        set(value) {
            swEndlessrepeat.isChecked = value
        }

    var _appIcon: Drawable
        get() = ivAppIcon.drawable
        set(value) {
            ivAppIcon.setImageDrawable(value)
        }

    var _appName: String
        get() = tvAppName.text.toString()
        set(value) {
            tvAppName.text = value
        }

    var _appPackageName: String
        get() = tvAppPackageName.text.toString()
        set(value) {
            tvAppPackageName.text = value
        }
    // Database
    private lateinit var db: Database
    // PackageManager
    private lateinit var pm:PackageManager
    // Context @Main
    private lateinit var mContext: Context

    private lateinit var _intentService: Intent
    private var _clickingService: clickingService? = null
    private var  isCancelled: Boolean = false
    private var currentProgressstate: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar) as Toolbar)

        db = Database(this)
        pm = packageManager
        mContext = this

        // Init
        _ImgQty = db.imgQty
        _Interval = db.interval
        _coordX = db.coordinateX
        _coordY = db.coordinateY
        _delay = db.delay
        _delayed = db.delayed
        if(!db.delayed) {
            _delay = 0
            etDelay.isEnabled = false
            bnDelayMinus.isEnabled = false
            bnDelayPlus.isEnabled = false
            bnDelayMinus.background.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY)
            bnDelayPlus.background.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY)
        }
        _tvImgQty = db.imgQty
        _FPS = db.FPS
        _ClipLength = db.imgQty / db.FPS
        _Duration = db.interval * db.imgQty
        _endlessRepeat = db.endlessRepeat
        try {
            val appinfo: ApplicationInfo = pm.getApplicationInfo(db.packageName, 0)
            _appIcon = pm.getApplicationIcon(appinfo)
            _appName = pm.getApplicationLabel(appinfo).toString()
            _appPackageName = db.packageName
        } catch (e: PackageManager.NameNotFoundException) {
            // :(
        }

        val clickListener: OnClickListener = OnClickListener { v ->
            when (v.id) {
                R.id.start_button -> startClickingService()
                R.id.getcoordinates -> {
                    val intent = Intent(mContext, GetCoordinates::class.java)
                    startActivityForResult(intent, 1)
                }
                R.id.choose_app -> appChooser(this@Main, ivAppIcon, tvAppName, tvAppPackageName).execute()
            }
        }

        class GenericTextWatcher(val view: View) : TextWatcher {

            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun afterTextChanged(editable: Editable) {
                when (view) {
                    etImgQty -> {
                        if (etImgQty.text.toString() == "")
                            _ImgQty = 0
                        db.imgQty = _ImgQty
                        updateInfoBox(db.imgQty, db.FPS, db.interval)
                    }
                    etFPS -> {
                        if (etFPS.text.toString() == "")
                            _FPS = 0
                        db.FPS = _FPS
                        updateInfoBox(db.imgQty, db.FPS, db.interval)
                    }
                    etInterval -> {
                        if (etInterval.text.toString() == "")
                            _Interval = 0
                        db.interval = _Interval
                        updateInfoBox(db.imgQty, db.FPS, db.interval)
                    }
                    etDelay -> {
                        if (etDelay.text.toString() == "")
                            _delay = 0
                        if(_delay == 0) {
                            _delayed = false
                            db.delayed = false
                            db.delay = 0
                        } else
                            db.delay = _delay
                    }
                }
            }

        }

        class isCheckedListener(val view: View) : OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                when (view) {
                    swDelay -> {
                        if (isChecked) {
                            db.delayed = true
                            if(_delay == 0)
                                db.delay = db.defaultValue(db._DELAY) as Int
                            _delay = db.delay
                            etDelay.isEnabled = true
                            bnDelayMinus.isEnabled = true
                            bnDelayPlus.isEnabled = true
                            bnDelayMinus.background.clearColorFilter()
                            bnDelayPlus.background.clearColorFilter()
                        } else {
                            db.delayed = false
                            _delay = 0
                            etDelay.isEnabled = false
                            bnDelayMinus.isEnabled = false
                            bnDelayPlus.isEnabled = false
                            bnDelayMinus.background.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY)
                            bnDelayPlus.background.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY)
                        }
                    }
                }
            }
        }

        etImgQty.addTextChangedListener(GenericTextWatcher(etImgQty))
        etFPS.addTextChangedListener(GenericTextWatcher(etFPS))
        etInterval.addTextChangedListener(GenericTextWatcher(etInterval))
        etDelay.addTextChangedListener(GenericTextWatcher(etDelay))

        swDelay.setOnCheckedChangeListener(isCheckedListener(swDelay))
        swEndlessrepeat.setOnCheckedChangeListener(isCheckedListener(swEndlessrepeat))

        bnStart.setOnClickListener(clickListener)
        bnCoordinates.setOnClickListener(clickListener)
        bnChooseApp.setOnClickListener(clickListener)

        _intentService = Intent(this@Main, clickingService::class.java)
        bindService(_intentService, mConnection, 0)
        if (savedInstanceState == null) {
            val extras = intent.extras
            if (extras == null) {
                isCancelled = false
            } else {
                isCancelled = extras.getBoolean("cancelled")
            }
        } else {
            isCancelled = savedInstanceState.getSerializable("cancalled") as Boolean
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val v: View = currentFocus

        if ((ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_MOVE) && v is EditText && !v.javaClass.name.startsWith("android.webkit.")) {
            val scrcoords: IntArray = IntArray(2)
            v.getLocationOnScreen(scrcoords)
            val x: Float = ev.rawX + v.getLeft() - scrcoords[0]
            val y: Float = ev.rawY + v.getTop() - scrcoords[1]

            if (x < v.getLeft() || x > v.getRight() || y < v.getTop() || y > v.getBottom()) {
                v.clearFocus()
                hideKeyboard((this))
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    fun hideKeyboard(activity: Activity) {
        if (activity.window != null && activity.window.decorView != null) {
            val imm: InputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
    }

    val mConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceDisconnected(name: ComponentName) {
            _clickingService = null
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val mLocalBinder = service as clickingService.LocalBinder
            _clickingService = mLocalBinder.getServerInstance()

            if(!isCancelled)
                _clickingService?.startClicking()

            if (isCancelled) {
                db.cancelled = false
                isCancelled = false
                currentProgressstate = _clickingService?.getProgress() as Int
                _clickingService?.stopClicking()
                val builder = AlertDialog.Builder(mContext)
                builder.setTitle("Result")
                builder.setMessage("Progress: " + currentProgressstate + " / " + db.imgQty + "\n " +
                        "Do you want to continue?")
                builder.setPositiveButton("Yes") { dialog, id ->
                    _clickingService?.resumeClicking()
                    startChoosedApp()
                }
                builder.setNegativeButton("No") { dialog, id ->
                    _clickingService?.stopService()
                    dialog.cancel()
                }
                builder.setOnCancelListener {
                    _clickingService?.stopService()
                }
                val dialog = builder.create()
                dialog.show()
            }
        }
    }

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            context.unregisterReceiver(this)

            val access = intent.getBooleanExtra("accessGranted", false)
            if (access) {
                if(isCancelled)
                    startClickingService(db.imgQty - currentProgressstate, false, true)
                else
                    startClickingService(db.imgQty, db.delayed, true)
            } else {
                //rootDenied()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == 1) {
            if (resultCode == 1) {
                db.coordinateX = data.getIntExtra("coordx", 0)
                db.coordinateY = data.getIntExtra("coordy", 0)

                tvCoordX.text = Integer.toString(db.coordinateX)
                tvCoordY.text = Integer.toString(db.coordinateY)
            }
        }
    }//onActivityResult

    fun startClickingService(_imqty: Int = db.imgQty, delayed: Boolean = db.delayed, rootGranted: Boolean = false) {
        // Getting root access
        if(!rootGranted) {
            val rootaccess = rootAccess(this@Main)
            val filtera = IntentFilter()
            filtera.addAction(rootaccess.rootaccess)
            registerReceiver(receiver, filtera)
            rootaccess.execute()
        }
        // Start and bind service if we have root access
        if(rootGranted) {
            bindService(_intentService, mConnection, 0)
            _intentService.putExtra("imgqty", _imqty)
            _intentService.putExtra("delayed", delayed)
            startService(_intentService)
            startChoosedApp()
        }
    }

    fun startChoosedApp () {
        try {
            pm = packageManager
            var LaunchIntent: Intent = pm.getLaunchIntentForPackage(db.packageName)
            startActivity( LaunchIntent )
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }
    fun updateInfoBox(imgQty: Int, fps: Int, interval: Int) {
        _tvImgQty = imgQty
        _ClipLength = imgQty / fps
        _Duration = interval * imgQty
    }

    fun formatTime(value: Int): String {
        val durationS = value.toLong()
        return String.format("%02d:%02d:%02d", TimeUnit.SECONDS.toHours(durationS),
                TimeUnit.SECONDS.toMinutes(durationS) - TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(durationS)),
                durationS - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(durationS)))
    }

    fun increaseInteger(view: View) {
        var tv: TextView = findViewById(view.labelFor) as TextView
        tv.text = (tv.text.toString().toInt() + 1).toString()
    }

    fun decreaseInteger(view: View) {
        var tv: TextView = findViewById(view.labelFor) as TextView
        tv.text = (tv.text.toString().toInt() - 1).toString()
    }
}