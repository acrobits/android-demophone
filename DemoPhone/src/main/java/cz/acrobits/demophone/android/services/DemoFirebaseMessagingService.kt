package cz.acrobits.demophone.android.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class DemoFirebaseMessagingService : FirebaseMessagingService()
{
    override fun onMessageReceived(message: RemoteMessage) =
        DemoServices.instance.pushHandler.onMessageReceived(message)

    override fun onNewToken(token: String) =
        DemoServices.instance.pushHandler.onNewToken(token)
}
