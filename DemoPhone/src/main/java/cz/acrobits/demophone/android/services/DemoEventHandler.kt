package cz.acrobits.demophone.android.services

import android.content.Context
import android.widget.Toast
import cz.acrobits.demophone.android.MainActivity
import cz.acrobits.libsoftphone.Instance
import cz.acrobits.libsoftphone.data.Call
import cz.acrobits.libsoftphone.data.PushTestScheduleResult
import cz.acrobits.libsoftphone.event.CallEvent
import cz.acrobits.libsoftphone.support.Listeners

class DemoEventHandler(private val context: Context, listeners: Listeners) :
    Listeners.OnPushTestArrived,
    Listeners.OnPushTestScheduled,
    Listeners.OnCallStateChanged,
    Listeners.OnNewCall
{
    init
    {
        listeners.register(this)
    }

    override fun onPushTestArrived(accountId: String?)
    {
        Toast.makeText(context, "Push is working", Toast.LENGTH_LONG).show();
    }

    override fun onPushTestScheduled(accountId: String?, result: PushTestScheduleResult)
    {
        if (result == PushTestScheduleResult.Success)
            Toast.makeText(context, "Push test scheduled", Toast.LENGTH_LONG).show();
        else
            Toast.makeText(context, "Push test failed", Toast.LENGTH_LONG).show();
    }

    override fun onCallStateChanged(callEvent: CallEvent, callState: Call.State)
    {
        // Ensure that all terminal calls are closed and their resources freed
        if (callState.isTerminal)
            Instance.Calls.close(callEvent)
    }

    override fun onNewCall(callEvent: CallEvent)
    {
        MainActivity.startMainActivity(context, callEvent.eventId);
    }
}
