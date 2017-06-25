package verseczi.intervaltimer.helpers

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import verseczi.intervaltimer.Main
import verseczi.intervaltimer.R
import verseczi.intervaltimer.data.Database

class NotificationHelper(private val mContext: Context) {
    private lateinit var mNotificationManager: NotificationManager
    private var db: Database = Database(mContext)
    private var mNotifyBuilder = Notification.Builder(mContext)
    private val notificationID: Int = 1

    fun createNotification() {
        val notificationIntent = Intent(mContext, Main::class.java).putExtra("cancelled", true)
        val pendingNotificationIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0)

        mNotificationManager = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotifyBuilder
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Interval Timer")
                .setContentIntent(pendingNotificationIntent)

        mNotificationManager.notify(notificationID, mNotifyBuilder.build())

    }

    fun cancelNotification() {
        mNotificationManager.cancel(notificationID)
    }

    fun progressUpdate(msg: String) {
        mNotifyBuilder.setContentText(msg)
        mNotificationManager.notify(notificationID, mNotifyBuilder.build())
    }

}