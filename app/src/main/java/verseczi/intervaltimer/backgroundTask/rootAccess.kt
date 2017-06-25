package verseczi.intervaltimer.backgroundTask

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.util.Log
import java.io.DataInputStream
import java.io.DataOutputStream


class rootAccess(private val mContext: Context) :  AsyncTask<Void, Void, Void>() {

    val rootaccess = "ROOTACCESS"
    private var intent = Intent(rootaccess)
    private var accessGranted = false

    override fun doInBackground(vararg params: Void): Void? {
        val suProcess: Process

        try {
            suProcess = Runtime.getRuntime().exec("su")

            val os = DataOutputStream(suProcess.outputStream)
            val osRes = DataInputStream(suProcess.inputStream)

            if (null != os && null != osRes) {
                // Getting the id of the current user to check if this is root
                os.writeBytes("id\n")
                os.flush()

                val currUid = osRes.readLine()
                var exitSu = false

                if (null == currUid) {
                    accessGranted = false
                    Log.e("ROOT", "Can't get root access or denied by user")
                } else if (currUid.contains("uid=0")) {
                    accessGranted = true
                    Log.e("ROOT", "Root access granted")
                } else {
                    accessGranted = false
                    exitSu = true
                    Log.e("ROOT", "Root access rejected: " + currUid)
                }

                if (exitSu) {
                    accessGranted = false
                    os.writeBytes("exit\n")
                    os.flush()
                }
            }
        } catch (e: Exception) {
            accessGranted = false
            Log.e("ROOT", "Root access rejected [" + e.javaClass.name + "] : " + e.message)
        }

        intent.putExtra("accessGranted", accessGranted)
        mContext.sendBroadcast(intent)

        return null
    }
}
