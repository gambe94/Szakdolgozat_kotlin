package demeter.gabor.tracker.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import demeter.gabor.tracker.R
import demeter.gabor.tracker.Util.Constants
import java.util.HashMap

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {


        // Check if message contains a notification payload.
        if (remoteMessage!!.notification != null) {
            val title = remoteMessage.notification.title //get title
            val message = remoteMessage.notification.body //get message

            val extras = HashMap<String, String?>()

            if (remoteMessage.data.size > 0) {
                extras[Constants.LONGITUDE] = remoteMessage.data[Constants.LONGITUDE]
                extras[Constants.LATITUDE] = remoteMessage.data[Constants.LATITUDE]
                extras[Constants.USERNAME] = remoteMessage.data[Constants.USERNAME]
                extras[Constants.CURRENTUSER_UID] = remoteMessage.data[Constants.CURRENTUSER_UID]
            }

            Log.d(TAG, "Message Notification Title: " + title!!)
            Log.d(TAG, "Message Notification Body: " + message!!)

            sendNotification(title, message, extras)
        }
    }

    override fun onDeletedMessages() {

    }

    private fun sendNotification(title: String?, messageBody: String?, extras: Map<String, String>) {
        val intent = Intent(this, UserMapsActivity::class.java)
        for (key in extras.keys) {
            if (key === Constants.LATITUDE || key === Constants.LONGITUDE) {
                intent.putExtra(key, java.lang.Double.parseDouble(extras[key]))
            } else {
                intent.putExtra(key, extras[key])
            }
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT)


        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id))
                .setSmallIcon(R.drawable.ic_message_alert_black_24dp)
                .setColor(resources.getColor(R.color.colorPrimary))
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }

    companion object {

        private val TAG = "FirebaseMessagingServce"
    }
}