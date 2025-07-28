package cz.acrobits.demophone.android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import java.util.Objects;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;
import cz.acrobits.ali.AndroidUtil;
import cz.acrobits.libsoftphone.contacts.ContactSource;
import cz.acrobits.libsoftphone.data.Call;
import cz.acrobits.libsoftphone.data.ResolvedPeerAddress;
import cz.acrobits.libsoftphone.event.CallEvent;
import cz.acrobits.libsoftphone.event.Event;
import cz.acrobits.libsoftphone.event.RemoteUser;
import cz.acrobits.libsoftphone.event.StreamParty;
import cz.acrobits.libsoftphone.internal.contacts.ContactsUtil;

//******************************************************************
public class DemoNotificationHandler
//******************************************************************
{
    public static final int ID_INCOMING_CALL = 200;
    protected static final String CHANNEL_INCOMING_CALL = "demophone_incomnig_call";

    private final NotificationManager mNotificationManager;

    //******************************************************************
    public DemoNotificationHandler()
    //******************************************************************
    {
        mNotificationManager = AndroidUtil.getSystemService(NotificationManager.class);
        mNotificationManager.cancelAll();
        if (Build.VERSION.SDK_INT >= 26)
            createNotificationChannel(CHANNEL_INCOMING_CALL, AndroidUtil.getResources().getString(R.string.notification_call_incoming), NotificationManager.IMPORTANCE_HIGH);
    }

    //******************************************************************
    @RequiresApi(26)
    protected void createNotificationChannel(String id,
                                             String name,
                                             int importance)
    //******************************************************************
    {
        NotificationChannel channel = new NotificationChannel(id, name, importance);
        channel.enableLights(false);
        channel.enableVibration(false);
        mNotificationManager.createNotificationChannel(channel);
    }

    //******************************************************************
    public void doNotification(@NonNull CallEvent callEvent,
                               @NonNull Call.State state)
    //******************************************************************
    {
        Context context = AndroidUtil.getContext();
        switch(state)
        {
            case IncomingRinging:
                mNotificationManager.notify(ID_INCOMING_CALL, createRingingNotification(context, callEvent));
                return;
        }

        cancelIncomingCallNotification();
    }

    //******************************************************************
    private Notification createRingingNotification(@NonNull Context context, @NonNull CallEvent callEvent)
    //******************************************************************
    {
        String messageText = context.getResources().getString(R.string.notification_call_incoming);

        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(MainActivity.EXTRA_EVENT_ID, callEvent.getEventId());

        PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent,
                                                          PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        RemoteUser remoteUser = callEvent.getRemoteUser();
        StreamParty streamParty = Objects.requireNonNull(remoteUser).toStreamParty();

        String personUri = null;
        if (streamParty.contactId != null && streamParty.contactId.source == ContactSource.ADDRESS_BOOK)
            personUri = ContactsUtil.getLookupUri(streamParty.contactId.id);

        Person person = new Person.Builder()
                .setName(remoteUser.getDisplayName())
                .setIcon(IconCompat.createWithResource(context, R.drawable.person_24px))
                .setUri(personUri)
                .setKey(remoteUser.getTransportUri())
                .setImportant(true)
                .build();

        PendingIntent answer = Objects.requireNonNull(createAnswerAction(context, callEvent.getEventId()).getActionIntent());
        PendingIntent dismiss = Objects.requireNonNull(createDismissAction(context, callEvent.getEventId()).getActionIntent());

        NotificationCompat.Style style = NotificationCompat.CallStyle
                .forIncomingCall(person, dismiss, answer)
                .setIsVideo(false);

        return new NotificationCompat.Builder(context, CHANNEL_INCOMING_CALL)
                .setSmallIcon(R.drawable.ic_call_black_24dp)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pIntent)
                .setTicker(messageText)
                .setContentTitle(getEventAddress(callEvent))
                .setContentText(messageText)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(pIntent, true)
                .setStyle(style)
                .setAutoCancel(false)
                .setOngoing(true)
                .build();
    }

    //******************************************************************
    private NotificationCompat.Action createDismissAction(Context context, long callEventId)
    //******************************************************************
    {
        PendingIntent dismissIntent =
                createCallNotificationPendingIntent(context, DemoNotificationBroadcastReceiver.ACTION_DECLINE_INCOMING_CALL, callEventId);
        return new NotificationCompat.Action(R.drawable.ic_call_end_black_24dp, AndroidUtil.getResources().getString(R.string.dismiss), dismissIntent);
    }

    //******************************************************************
    private NotificationCompat.Action createAnswerAction(Context context, long callEventId)
    //******************************************************************
    {
        PendingIntent answerIntent =
                createCallNotificationPendingIntent(context, DemoNotificationBroadcastReceiver.ACTION_ANSWER_INCOMING_CALL, callEventId);
        return new NotificationCompat.Action(R.drawable.ic_call_black_24dp, AndroidUtil.getResources().getString(R.string.answer), answerIntent);
    }

    //******************************************************************
    private static PendingIntent createCallNotificationPendingIntent(Context context, String action, long callId)
    //******************************************************************
    {
        Intent intent = new Intent(action, null,
                context, DemoNotificationBroadcastReceiver.class);
        Bundle extras = new Bundle();
        extras.putLong(MainActivity.EXTRA_EVENT_ID, callId);
        intent.putExtras(extras);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
    }

    //******************************************************************
    public void cancelIncomingCallNotification()
    //******************************************************************
    {
        cancelNotification(ID_INCOMING_CALL);
    }

    //******************************************************************
    private void cancelNotification(int notificationId)
    //******************************************************************
    {
        mNotificationManager.cancel(notificationId);
    }

    //******************************************************************
    public @NonNull
    String getEventAddress(Event event)
    //******************************************************************
    {
        StreamParty streamParty = event.getRemoteUser(0).toStreamParty();
        return TextUtils.isEmpty(streamParty.displayName) ? new ResolvedPeerAddress(streamParty.getCurrentOriginalTransportUri()).getHumanReadable():
                streamParty.displayName;
    }
}
