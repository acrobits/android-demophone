package cz.acrobits.demophone.android;

import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import cz.acrobits.ali.AndroidUtil;
import cz.acrobits.ali.Log;
import cz.acrobits.ali.Xml;
import cz.acrobits.commons.util.CollectionUtil;
import cz.acrobits.libsoftphone.Instance;
import cz.acrobits.libsoftphone.SDK;
import cz.acrobits.libsoftphone.account.AccountXml;
import cz.acrobits.libsoftphone.data.Account;
import cz.acrobits.libsoftphone.data.Call;
import cz.acrobits.libsoftphone.data.PushTestScheduleResult;
import cz.acrobits.libsoftphone.event.CallEvent;
import cz.acrobits.libsoftphone.key.IncomingCallsMode;
import cz.acrobits.libsoftphone.mergeable.MergeableNodeAttributes;
import cz.acrobits.libsoftphone.support.Listeners;
import cz.acrobits.libsoftphone.support.lifecycle.LifecycleTracker;
import cz.acrobits.libsoftphone.support.lifecycle.list.ActivityList;

/**
 * DemoPhoneApplication.
 *
 * Together with {@link MainActivity} serves as an example of how to use the libSoftphone SDK.
 * It demonstrates SDK initialization, basics of SIP account management,
 * Push notification handling and more.
 */
//******************************************************************
public class DemoPhoneApplication
        extends android.app.Application
        implements Listeners.OnPushTestArrived,
        Listeners.OnPushTestScheduled,
        Listeners.OnNewCall,
        Listeners.OnCallStateChanged
//******************************************************************
{
    /**
     * Logging.
     */
    public static final Log LOG = new Log(DemoPhoneApplication.class);

    /**
     * Push notifications to be processed.
     */
    private static final List<RemoteMessage> sPushes = new LinkedList<>();

    /**
     * Whether the application is running.
     */
    private boolean mRunning = false;

    /** Password for test account. */
    private static final byte[] PASSWORD = {(byte)0x6d, (byte)0x69, (byte)0x73, (byte)0x73, (byte)0x63, (byte)0x6f, (byte)0x6d};

    /** Generated phone number. */
    private String mNumber;

    private DemoNotificationHandler mNotificationHandler;

    /** Observer listeners. */
    public static final Listeners sListeners = new Listeners()
    {
        //******************************************************************
        @Override
        public @Nullable
        Object getRingtone(@NonNull CallEvent call)
        //******************************************************************
        {
            try
            {
                return AndroidUtil.getResources().getAssets().openFd("relax_ringtone.mp3");
            }
            catch (IOException e)
            {
                LOG.error("Failed to open ringtone: %s", e);
            }

            return null;
        }
    };

    // ******************************************************************
    public String getNumber()
    // ******************************************************************
    {
        return mNumber;
    }

    // ******************************************************************
    @Override
    public void onPushTestArrived(@Nullable String accountId)
    // ******************************************************************
    {
        Toast.makeText(AndroidUtil.getContext(), "Push is working", Toast.LENGTH_LONG).show();
    }

    // ******************************************************************
    @Override
    public void onPushTestScheduled(@Nullable String accountId, @NonNull PushTestScheduleResult result)
    // ******************************************************************
    {
        if(result == PushTestScheduleResult.Success)
            Toast.makeText(AndroidUtil.getContext(), "Push test scheduled", Toast.LENGTH_LONG).show();
        else
            Toast.makeText(AndroidUtil.getContext(), "Push test failed", Toast.LENGTH_LONG).show();
    }

    // ******************************************************************
    @Override
    public void onNewCall(@NonNull CallEvent call)
    // ******************************************************************
    {
        if(getActivityList().isAnyActive())
            MainActivity.startMainActivity(this, call.getEventId());
    }

    // ******************************************************************
    @Override
    public void onCallStateChanged(@NonNull CallEvent call, @NonNull Call.State state)
    // ******************************************************************
    {
        if(!getActivityList().isAnyActive())
            mNotificationHandler.doNotification(call, state);
        if(state.isTerminal() && getActivityList().getLast(MainActivity.class) == null)
        {
            // We don't have any call UI now, we can close the call immediately
            Instance.Calls.close(call);
        }
    }

    // ******************************************************************
    public void cancelIncomingCallNotification()
    // ******************************************************************
    {
        mNotificationHandler.cancelIncomingCallNotification();
    }

    /**
     * Get instance of the application.
     *
     * @return Instance of the application.
     */
    // ******************************************************************
    public static @NonNull
    DemoPhoneApplication instance()
    // ******************************************************************
    {
        if (!AndroidUtil.hasContext())
            throw new IllegalStateException("Invalid application specified in Android Manifest");
        return (DemoPhoneApplication) AndroidUtil.getApplicationContext();
    }


    /**
     * The application is created.
     * <p>
     * Sets context.
     * <p>
     * It does not start the application and load the library. You need
     * to call {@link #start} to do that.
     */
    // ******************************************************************
    @MainThread
    @Override
    public void onCreate()
    // ******************************************************************
    {
        super.onCreate();

        LOG.info("Creating...");
        mNotificationHandler = new DemoNotificationHandler();
        AndroidUtil.setContext(this);
    }


    /**
     * The application is started.
     * <p>
     * Initializes necessary listeners and starts
     * service.
     * <p>
     * Do not call this directly, use {@link #start} instead.
     */
    // ******************************************************************
    @MainThread
    public void onStart()
    // ******************************************************************
    {
        // This is how you can check your JWT key:
        assert !Instance.isValidJwtLicense(this, "my-jwt-key");

        // If the SDK is not yet initialized, initialize it
        if (Instance.preferences == null)
        {
            Xml prov = null;

            try
            {
                // The library has to be loaded before any SDK code can be used
                Instance.loadLibrary(this);
            }
            catch (Throwable e)
            {
                throw new RuntimeException(e);
            }

            // For SaaS SDK only
            if (CollectionUtil.contains(SDK.features, SDK.Feature.Provisioning))
            {
                /* Load provisioning XML. */
                prov = Xml.parse(AndroidUtil.getAsset("provisioning.xml"));

                // The license identifier is loaded from gradle.properties at compile time.
                Xml saas = new Xml("saas");
                saas.setChildValue("identifier", BuildConfig.SAAS_IDENTIFIER);
                prov.setChild(saas);
            }

            try
            {
                Instance.init(this, prov, DemoPreferences.class);
            }
            catch (Throwable e)
            {
                throw new RuntimeException(e);
            }
            Instance.setObserver(sListeners);
        }
        else
        {
            // SDK already initialized, just respawn the state
            Instance.State.respawn();
        }

        if(Instance.Registration.getAccountCount() == 0)
        {
            // Create unique test account number that stays the same on the same device
            int code = 0;
            for (byte b: Instance.System.getUniqueId().getBytes())
                // djb2 hash
                code = (code << 6) + code + b;
            mNumber = String.format(Locale.ROOT, "10%02d", Math.abs(code % 100));

            // Register a test account on Acrobits PBX
            Xml account = new Xml("account");
            account.setAttribute(Account.Attributes.ID, "Test Account");
            account.setChildValue(Account.USERNAME, mNumber);
            account.setChildValue(Account.PASSWORD, new String(PASSWORD));
            account.setChildValue(Account.HOST, "pbx.acrobits.cz");
            // Example of setting DTMF order
            account.setChildValue(Account.DTMF_ORDER, "rfc2833,audio");

            // Create/update an account
            /* The account is updated based on ID. If you do not specify ID, an unique one is generated
             * but each start will create a new account in the app. But we have fixed ID in the XML
             * above so if the account already exists, it's only updated. */
            Instance.Registration.saveAccount(new AccountXml(account, MergeableNodeAttributes.gui()));
        }
        else
        {
            AccountXml accountXml = Instance.Registration.getAccount(0);
            mNumber = accountXml.getString("username");
        }

        mRunning = true;

        sListeners.register(this);

        if(FirebaseApp.getApps(this).size() == 0)
        {
            //FirebaseMessaging not configured
            //Set incoming calls mode to STANDARD
            Instance.preferences.incomingCallsMode.set(IncomingCallsMode.STANDARD);
            return;
        }

        //Set incomingCallsMode to push as FirebaseMessaging configuration is available
        Instance.preferences.incomingCallsMode.set(IncomingCallsMode.PUSH);

        //Try registering the firebase so application can receive PNs.
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if(!task.isSuccessful())
                return;

            // Get new Instance ID token
            String token = task.getResult();
            if(!TextUtils.isEmpty(token))
            {
                // Report token to libSoftphone SDK
                AndroidUtil.handler.post(() -> Instance.Notifications.Push.setRegistrationId(token));
            }
        });
        Instance.Registration.updateAll();
    }

    /**
     * Start the application.
     * <p>
     * Initializes necessary listeners, starts
     * service and handles unhandled push messages.
     */
    // ******************************************************************
    @MainThread
    public static void start()
    // ******************************************************************
    {
        DemoPhoneApplication instance = instance();

        if (!isRunning())
            instance.onStart();

        instance.handlePushes();
    }

    // ******************************************************************
    @MainThread
    public static void stop()
    // ******************************************************************
    {
        DemoPhoneApplication instance = instance();

        if (isRunning())
        {
            instance.mRunning = false;
            sListeners.unregister(instance);
        }
    }

    /**
     * Check if the application is running.
     *
     * @return Whether the application is running.
     */
    // ******************************************************************
    public static boolean isRunning()
    // ******************************************************************
    {
        return instance().mRunning;
    }

    /**
     * Handle push notification.
     *
     * @param pushData Map with the push notification.
     */
    // ******************************************************************
    public static void handlePush(@NonNull RemoteMessage pushData)
    // ******************************************************************
    {
        synchronized (sPushes)
        {
            LOG.info("Push notification %H queued for handling", pushData);
            sPushes.add(pushData);
        }

        /* Use rendezvous so that the wakelock from GCM won't be released
         * until the application is started and MessageLoop's wakelock is
         * acquired */
        AndroidUtil.rendezvous(DemoPhoneApplication::start);
    }

    /**
     * Handle all pending push notifications.
     */
    // ******************************************************************
    @MainThread
    protected void handlePushes()
    // ******************************************************************
    {
        for (RemoteMessage message : sPushes)
        {
            try
            {
                Map<String, String> map = message.getData();
                LOG.info("Handling push notification %H", map);

                Xml messageData = Xml.toXml("pushData", message.getData());
                boolean highPriority = message.getPriority() == RemoteMessage.PRIORITY_HIGH;
                if (!Instance.Notifications.Push
                        .handle(messageData, highPriority))
                {
                    String missed = map.get("s_missed");

                    if (!TextUtils.isEmpty(missed)) {
                        Toast.makeText(AndroidUtil.getContext(), "Missed call PN arrived.", Toast.LENGTH_LONG).show();
                    }
                }
            }
            catch (Throwable t)
            {
                LOG.warning("Push handling failed: %s", t);
            }
        }
        sPushes.clear();
    }

    /**
     * Get list of activities
     */
    // ******************************************************************
    private ActivityList getActivityList()
    // ******************************************************************
    {
        return LifecycleTracker.getInstance().getActivityList();
    }
}
