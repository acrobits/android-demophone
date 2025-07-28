package cz.acrobits.demophone.android;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cz.acrobits.ali.AndroidUtil;
import cz.acrobits.libsoftphone.Instance;
import cz.acrobits.libsoftphone.data.Call;
import cz.acrobits.libsoftphone.event.CallEvent;

//******************************************************************
public class DemoNotificationBroadcastReceiver extends BroadcastReceiver
//******************************************************************
{
    public static final String ACTION_DECLINE_INCOMING_CALL =
            "cz.acrobits.demophone.ACTION_DECLINE_INCOMING_CALL";
    public static final String ACTION_ANSWER_INCOMING_CALL =
            "cz.acrobits.demophone.ACTION_ANSWER_VOICE_INCOMING_CALL";

    //******************************************************************
    @Override
    public void onReceive(Context context, Intent intent)
    //******************************************************************
    {
        if(Instance.preferences == null)
            return;

        if(intent.getAction().equals(ACTION_ANSWER_INCOMING_CALL))
        {
            long eventId = intent.getLongExtra(MainActivity.EXTRA_EVENT_ID, -1);
            if(eventId != -1)
                answerCall(context, eventId);
        }
        else if(intent.getAction().equals(ACTION_DECLINE_INCOMING_CALL))
        {
            DemoPhoneApplication.instance().cancelIncomingCallNotification();
            long eventId = intent.getLongExtra(MainActivity.EXTRA_EVENT_ID, -1);
            for(String conf: Instance.Calls.Conferences.list())
                for(CallEvent call: Instance.Calls.Conferences.getCalls(conf))
                    if(call.getEventId() == eventId)
                        Instance.Calls.rejectIncomingEverywhere(
                                call,
                                new Call.RejectReason("Declined by user from notification", Call.RejectReason.Type.User)
                        );
        }
    }

    //******************************************************************
    private void answerCall(Context context, long eventId)
    //******************************************************************
    {
        for (String conf : Instance.Calls.Conferences.list())
        {
            for (CallEvent call : Instance.Calls.Conferences.getCalls(conf))
            {
                if (call.getEventId() == eventId)
                {
                    DemoPhoneApplication.instance().cancelIncomingCallNotification();
                    //The app should first open the activity before accepting a call,
                    // otherwise it might not get access to microphone on Android 11
                    MainActivity.startMainActivityAndAnswer(context, call.getEventId());
                }
            }
        }
    }
}
