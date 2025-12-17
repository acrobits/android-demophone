package cz.acrobits.demophone.android.services

import android.content.Context
import android.widget.Toast
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import cz.acrobits.ali.Log
import cz.acrobits.ali.Xml
import cz.acrobits.libsoftphone.Instance
import cz.acrobits.libsoftphone.key.IncomingCallsMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class DemoPushHandler(
    coroutineScope: CoroutineScope,
    private val context: Context
)
{
    companion object
    {
        private val LOG = Log("DemoPushHandler")
    }

    private val _pendingPushes = Channel<RemoteMessage>(Channel.UNLIMITED)
    private val _currentPushToken = MutableStateFlow<String?>(null)

    init
    {
        val hasPushes = configureFirebase()
        LOG.info("FirebaseMessaging configured: %s", hasPushes)

        if (hasPushes)
        {
            coroutineScope.launch {
                for (push in _pendingPushes)
                    processPush(push)
            }

            coroutineScope.launch {
                _currentPushToken
                    .filterNotNull()
                    .collect { token ->
                        Instance.Notifications.Push.setRegistrationId(token)
                    }
            }
        }
    }

    /**
     * Handle push notification.
     *
     * @param pushData Push notification data.
     */
    @AnyThread
    fun onMessageReceived(pushData: RemoteMessage)
    {
        LOG.info("Push notification %H queued for handling", pushData)
        _pendingPushes.trySend(pushData)
    }

    @AnyThread
    fun onNewToken(token: String)
    {
        LOG.info("New push token received: %s", token)
        _currentPushToken.value = token
    }

    @MainThread
    private fun processPush(remoteMessage: RemoteMessage) = runCatching {
        val map = remoteMessage.getData()
        LOG.info("Handling push notification %H", map)

        val messageData = Xml.toXml("pushData", remoteMessage.getData())
        val highPriority = remoteMessage.getPriority() == RemoteMessage.PRIORITY_HIGH

        if (!Instance.Notifications.Push.handle(messageData, highPriority))
        {
            val missed = map["s_missed"]

            if (!missed.isNullOrEmpty())
                Toast.makeText(context, "Missed call PN arrived.", Toast.LENGTH_LONG).show()
        }
    }.onFailure {
        LOG.warning("Push handling failed: %s", it)
    }

    private fun configureFirebase(): Boolean
    {
        if (FirebaseApp.getApps(context).isEmpty())
        {
            // FirebaseMessaging not configured
            // Disable push notifications
            Instance.preferences.incomingCallsMode.set(IncomingCallsMode.STANDARD)
            return false
        }

        // Set incomingCallsMode to push as FirebaseMessaging configuration is available
        Instance.preferences.incomingCallsMode.set(IncomingCallsMode.PUSH)

        // Try to load the initial token
        // This used to be necessary; nowadays you may be able to rely on onNewToken only
        FirebaseMessaging
            .getInstance()
            .getToken()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful)
                    return@addOnCompleteListener

                // Get new Instance ID token
                val token = task.getResult()
                if (!token.isNullOrEmpty())
                {
                    // Report token to libSoftphone SDK
                    LOG.info("Initial push token obtained: %s", token)
                    _currentPushToken.value = token
                }
            }

        return true
    }
}
