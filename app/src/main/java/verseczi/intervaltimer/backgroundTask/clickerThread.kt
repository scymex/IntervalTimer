package verseczi.intervaltimer.backgroundTask

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.util.Log
import verseczi.intervaltimer.data.Database
import java.io.DataOutputStream
import java.io.IOException

class clickerThread(private val mContext: Context, mHandler: Handler) : Thread() {
    private var db: Database = Database(mContext)
    val CLICK_DONE = "click_done"
    private var intent = Intent(CLICK_DONE)
    internal var lastTimestamp: Long = 0
    var _imgQty: Int = db.imgQty
    var _delayed: Boolean = db.delayed
    var _delay: Int = db.delay
    var _endlessRepeat: Boolean = db.endlessRepeat
    var _interval: Int = db.interval
    var _coordinateX: Int = db.coordinateX
    var _coordinateY: Int = db.coordinateY

    var _cancelled: Boolean = false

    val randomInt:Double = Math.random()

    private var pm = mContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var mLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "IntervalTimer")

    var _mHandler: Handler = mHandler
    var _bundle: Bundle = Bundle()

    override fun run() {
        // set wake lock
        if (mLock != null)
            mLock.acquire()

        DebugInfo()
        // getting root access
        var mProcess: Process

        try {
            Log.i("SCYMEX", "Get 'su' process...")
            mProcess = Runtime.getRuntime().exec("su")
        } catch (e: IOException) {
            Log.e("SCYMEX", "Exception thrown during 'su' : " + e.message)
            e.printStackTrace()
            return
        }

        // get the process output stream
        Log.i("SCYMEX", "Get 'su' process data output stream...")
        var mOutputStream = DataOutputStream(mProcess.outputStream)

        if (_delayed) {
            setDelay(10)
            for (i in 1.._delay) {
                if(_cancelled) return

                setDelay(_delay + 1 - i)
                try {
                    Thread.sleep(1000)
                } catch (ie: InterruptedException) {
                    ie.printStackTrace()
                    return
                }

            }
            setDelay(-1)
        }

        if (_endlessRepeat) {
            var i = 0
            while (true) {
                if(_cancelled) return
                lastTimestamp = System.currentTimeMillis()
                if (i == 0) progressUpdate(0)
                //forceScreenState();
                executeTap(mProcess, mOutputStream)

                progressUpdate(i + 1)
                intervalWait()
                i++
            }
        } else if (_imgQty > 1) {
            for (i in 0.._imgQty - 1) {
                if(_cancelled) return
                lastTimestamp = System.currentTimeMillis()
                if (i == 0) progressUpdate(0)
                //forceScreenState();
                executeTap(mProcess, mOutputStream)

                progressUpdate(i + 1)
                intervalWait()
            }
        } else {
            progressUpdate(0)
            executeTap(mProcess, mOutputStream)
        }
        return
    }
    /**
     * Executes the tap action
     * TODO: újra írni
     */
    private fun executeTap(mProcess: Process, mOutputStream: DataOutputStream) {
        val shellCmd = "/system/bin/input tap $_coordinateX $_coordinateY && echo \"$CLICK_DONE\" & \n"
        try {
            mOutputStream.writeBytes(shellCmd)
            val inputStream = mProcess.inputStream
            val buffer = ByteArray(1024)
            while (true) { // TODO: while(true) :(
                val read = inputStream.read(buffer)
                val out = String(buffer, 0, read)
                if (out.contains(CLICK_DONE)) break
            }
        } catch (ioe: IOException) {
            Log.e("SCYMEX", "Exception thrown during tap execution : " + ioe.message)
            ioe.printStackTrace()
        }

    }

    private fun intervalWait() {
        var between = 0
        while (between <= _interval * 1000) {
            try {
                Thread.sleep(100) // sleep 0.1 second
                between = (System.currentTimeMillis() - lastTimestamp).toInt()
            } catch (ie: InterruptedException) {
                ie.printStackTrace()
            }

        }
    }

    private fun progressUpdate(img_num: Int) {
        sendMsgToMainThread("currentProgress", img_num)
    }

    private fun setDelay(secs: Int) {
        sendMsgToMainThread("delay", secs)
    }

    private fun sendMsgToMainThread(_key: String, _value: Any) {
        when (_value){
            is String -> _bundle.putString(_key, _value)
            is Int -> _bundle.putInt(_key, _value)
        }
        val msg = _mHandler.obtainMessage()
        msg.data = _bundle
        _mHandler.sendMessage(msg)
    }

    private fun DebugInfo() {
        val sb = StringBuilder()
        sb.append("*****************************************\n")
        sb.append("Interval Timer config: \n")
        sb.append("*****************************************\n")
        sb.append("\t delayed start......: ").append(_delayed).append("\n")
        sb.append("\t delay..............: ").append(_delay).append("\n")
        sb.append("\t number of images...: ").append(_imgQty).append("\n")
        sb.append("\t endless repeat.....: ").append(_endlessRepeat).append("\n")
        sb.append("\t interval...........: ").append(_interval).append("\n")
        sb.append("\t Coordinates........: ").append(_coordinateX).append("/").append(_coordinateY).append("\n")
        sb.append("*****************************************\n")

        Log.i("SCYMEX", sb.toString())
    }
}