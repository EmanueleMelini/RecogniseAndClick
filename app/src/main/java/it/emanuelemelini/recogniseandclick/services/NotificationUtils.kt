package it.emanuelemelini.recogniseandclick.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.util.Pair
import it.emanuelemelini.recogniseandclick.R


class NotificationUtils {

    companion object {
        private const val NOTIFICATION_ID = 1337
        private const val NOTIFICATION_CHANNEL_ID = "it.emanuelemelini.recogniseandcheck.app"
        private const val NOTIFICATION_CHANNEL_NAME = "it.emanuelemelini.recogniseandcheck.app"

        fun getNotification(context: Context): Pair<Int, Notification> {
            createNotificationChannel(context)
            val notification = createNotification(context)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
            return Pair(NOTIFICATION_ID, notification)
        }

        private fun createNotificationChannel(context: Context) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        private fun createNotification(context: Context): Notification {
            val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            builder.setSmallIcon(R.drawable.ic_launcher_background)
            builder.setContentTitle(context.getString(R.string.app_name))
            builder.setContentText(context.getString(R.string.app_name))
            builder.setOngoing(true)
            builder.setCategory(Notification.CATEGORY_SERVICE)
            builder.priority = NotificationManager.IMPORTANCE_LOW
            builder.setShowWhen(true)
            return builder.build()
        }

    }

}