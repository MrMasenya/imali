package za.ac.imali.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ImaliFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: "iMali"
        val body = message.notification?.body ?: message.data["body"] ?: "Update"
        Log.i(TAG, "FCM received: $title")
        NotificationHelper.show(this, title, body, message.hashCode())
    }

    override fun onNewToken(token: String) {
        Log.i(TAG, "FCM token refreshed")
        getSharedPreferences("imali_fcm", MODE_PRIVATE).edit().putString("token", token).apply()
    }

    companion object {
        private const val TAG = "ImaliFCM"
    }
}
