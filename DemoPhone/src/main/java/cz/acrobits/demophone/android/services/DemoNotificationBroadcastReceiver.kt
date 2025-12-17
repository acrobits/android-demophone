package cz.acrobits.demophone.android.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cz.acrobits.demophone.android.MainActivity
import cz.acrobits.libsoftphone.Instance
import cz.acrobits.libsoftphone.data.Call.RejectReason
import cz.acrobits.libsoftphone.event.CallEvent

class DemoNotificationBroadcastReceiver : BroadcastReceiver()
{
    companion object
    {
        const val ACTION_DECLINE_INCOMING_CALL: String = "cz.acrobits.demophone.ACTION_DECLINE_INCOMING_CALL"
        const val ACTION_ANSWER_INCOMING_CALL: String = "cz.acrobits.demophone.ACTION_ANSWER_VOICE_INCOMING_CALL"
    }

    override fun onReceive(context: Context, intent: Intent)
    {
        if (Instance.preferences == null)
            return

        when (intent.action)
        {
            ACTION_ANSWER_INCOMING_CALL ->
            {
                val eventId = intent.getLongExtra(MainActivity.EXTRA_EVENT_ID, -1)
                if (eventId != -1L)
                    answerCall(context, eventId)
            }

            ACTION_DECLINE_INCOMING_CALL ->
            {
                DemoServices.instance.notificationHandler.cancelIncomingCallNotification()

                val eventId = intent.getLongExtra(MainActivity.EXTRA_EVENT_ID, -1)

                forAllCallsWithId(eventId) { call ->
                    Instance.Calls.rejectIncomingEverywhere(
                        call,
                        RejectReason("Declined by user from notification", RejectReason.Type.User)
                    )
                }
            }

            else -> return
        }
    }

    private fun answerCall(context: Context, eventId: Long)
    {
        forAllCallsWithId(eventId) { call ->
            DemoServices.instance.notificationHandler.cancelIncomingCallNotification()
            // The app should first open the activity before accepting a call,
            // otherwise it might not get access to microphone on Android 11
            MainActivity.startMainActivityAndAnswer(context, call.eventId)
        }
    }

    private fun forAllCallsWithId(eventId: Long, action: (call: CallEvent) -> Unit)
    {
        Instance.Calls.Conferences.list().forEach { conference ->
            Instance.Calls.Conferences.getCalls(conference)
                .filter { it.eventId == eventId }
                .forEach(action)
        }
    }
}
