package cz.acrobits.demophone.android;

import com.google.firebase.messaging.RemoteMessage;
import org.jetbrains.annotations.NotNull;
import androidx.annotation.NonNull;
import cz.acrobits.ali.AndroidUtil;
import cz.acrobits.libsoftphone.Instance;

//******************************************************************
public class DemoFirebaseMessagingService
        extends com.google.firebase.messaging.FirebaseMessagingService
//******************************************************************
{
    //******************************************************************
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage)
    //******************************************************************
    {
        DemoPhoneApplication.handlePush(remoteMessage);
    }

    //******************************************************************
    @Override
    public void onNewToken(@NotNull String s)
    //******************************************************************
    {
        //libsoftphone SDK is not initialized yet
        if(!DemoPhoneApplication.isRunning())
            return;

        // Report renewed token to the libSoftphone SDK
        AndroidUtil.rendezvous(()-> Instance.Notifications.Push.setRegistrationId(s));
    }
}
