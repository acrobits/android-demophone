package cz.acrobits.demophone.android;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cz.acrobits.ali.AndroidUtil;
import cz.acrobits.ali.Json;
import cz.acrobits.ali.Log;
import cz.acrobits.libsoftphone.Instance;
import cz.acrobits.libsoftphone.data.Call;
import cz.acrobits.libsoftphone.data.DialAction;
import cz.acrobits.libsoftphone.event.CallEvent;
import cz.acrobits.libsoftphone.event.Event;
import cz.acrobits.libsoftphone.event.EventStream;
import cz.acrobits.libsoftphone.event.StreamParty;
import cz.acrobits.libsoftphone.event.history.StreamQuery;
import cz.acrobits.libsoftphone.telecom.TelecomUtil;

/**
 * Utility class containing methods that handle common calling scenarios.
 * <p>
 *
 * Some methods are for illustration purposes only as in real-world applications
 * they would include Activity/Fragments transitions and more.
 *
 */
//******************************************************************
public class CallUtil
//******************************************************************
{
    private static final Log LOG = new Log(CallUtil.class);

    public static final int REDIRECT_NONE = 0;
    public static final int REDIRECT_TRANSFER = REDIRECT_NONE + 1;
    public static final int REDIRECT_ATT_TRANSFER = REDIRECT_TRANSFER + 1;
    public static final int REDIRECT_FORWARD = REDIRECT_ATT_TRANSFER + 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({REDIRECT_NONE, REDIRECT_TRANSFER, REDIRECT_ATT_TRANSFER, REDIRECT_FORWARD})
    public @interface RedirectMode
    {
    }

    //******************************************************************
    public static class RedirectStateHolder
    //******************************************************************
    {
        private @RedirectMode int mRedirectMode;
        private CallEvent mRedirectEvent;

        //******************************************************************
        public RedirectStateHolder(@RedirectMode int redirectMode, @NonNull CallEvent redirectEvent)
        //******************************************************************
        {
            mRedirectMode = redirectMode;
            mRedirectEvent = redirectEvent;
        }

        //******************************************************************
        public @RedirectMode int getRedirectMode()
        //******************************************************************
        {
            return mRedirectMode;
        }

        //******************************************************************
        public CallEvent getRedirectEvent()
        //******************************************************************
        {
            return mRedirectEvent;
        }
    }

    private static RedirectStateHolder sRedirectState;

    /**
     * Att. Transfer a call
     *
     * @param callEvent The call to transfer.
     */
    //******************************************************************
    public static void attTransferCall(@NonNull CallEvent callEvent)
    //******************************************************************
    {
        // if call attended transfer flow already started
        if (getRedirectMode() == REDIRECT_ATT_TRANSFER)
        {
            if(callEvent.getEventId() != sRedirectState.getRedirectEvent().getEventId())
            {
                Instance.Calls.Conferences.attendedTransfer(callEvent, sRedirectState.getRedirectEvent());
                clearRedirectState();
            }
        }
        else
        {
            // If a call is a conference call and has exactly two participants
            if (isConference(callEvent))
            {
                CallEvent[] calls = Instance.Calls.Conferences.getCalls(callEvent);
                if(calls.length == 2)
                {
                    Instance.Calls.Conferences.attendedTransfer(calls[0], calls[1]);
                    return;
                }
                else
                {
                    // No suitable calls, A conference call cannot be transferred.
                    // Show some UI with warning
                }
            }

            int establishedSipCallCount = getEstablishedSipCallCount();
            if(establishedSipCallCount > 2)
            {
                // Show UI which allows user to select which call he wants to transfer
                return;
            }
            if (establishedSipCallCount == 2)
            {
                for (String conf : Instance.Calls.Conferences.list())
                {
                    for (CallEvent call : Instance.Calls.Conferences.getCalls(conf))
                    {
                        if (callEvent.getEventId() != call.getEventId())
                        {
                            Instance.Calls.Conferences.attendedTransfer(callEvent, call);
                            return;
                        }
                    }
                }
            }
            else
            {
                // Begins the call transfer, Now user needs to dial a new call to transfer to
                beginRedirect(callEvent, REDIRECT_ATT_TRANSFER);
                // Keypad or contact picker Fragment/Activity should be shown NOW.
            }
        }
    }


    /**
     * Transfer a call
     *
     * @param callEvent The call to transfer.
     */
    //******************************************************************
    public static void transferCall(@NonNull CallEvent callEvent)
    //******************************************************************
    {
        // Transfer call flow begins, user has to dial a new call to transfer to
        // Keypad or contact picker Fragment/Activity should be shown.
        beginRedirect(callEvent, REDIRECT_TRANSFER);
        // Keypad or contact picker Fragment/Activity should be shown NOW.
    }

    /**
     * Forward a call
     *
     * @param callEvent The call that should be forwarded.
     */
    //******************************************************************
    public static void forwardCall(@NonNull CallEvent callEvent)
    //******************************************************************
    {
        // Forward call flow begins, user has to dial a new call to forward to.
        beginRedirect(callEvent, REDIRECT_FORWARD);
        // Keypad or contact picker Fragment/Activity should be shown NOW.
    }

    /**
     * Split a call from a conference
     *
     * @param callEvent The call that needs to be split.
     */
    //******************************************************************
    public static void splitCall(@NonNull CallEvent callEvent)
    //******************************************************************
    {
        if (!isConference(callEvent))
            return;

        split(Instance.Calls.Conferences.get(callEvent));
    }

    /**
     * Join a call with a conference
     *
     * @param callEvent The call to join into conference.
     */
    //******************************************************************
    public static void joinCall(@NonNull CallEvent callEvent)
    //******************************************************************
    {
        String selectedConference = Instance.Calls.Conferences.get(callEvent);
        List<String> conferences = getAlternativeConferences(callEvent);
        if(conferences.size() == 0)
            //There are no conferences to be merged with.
            return;
        else if(conferences.size() == 1)
            join(selectedConference, conferences.get(0));
        else
            // multiple alternative conferences, present e.g. a list of conferences to user
            return;
    }

    /**
     * Merges conferences.
     * Each conference can cointain one or more calls.
     *
     * @param firstConferenceId The ID of the first conference to merge.
     * @param secondConferenceId The ID of the second conference to merge.
     */
    //******************************************************************
    public static void join(@NonNull String firstConferenceId, @NonNull  String secondConferenceId)
    //******************************************************************
    {
        if(firstConferenceId.equals(secondConferenceId))
            return;

        CallEvent[] firstConferenceCalls = Instance.Calls.Conferences.getCalls(secondConferenceId);
        CallEvent[] secConferenceCalls = Instance.Calls.Conferences.getCalls(firstConferenceId);

        String newConferenceId = Instance.Calls.Conferences.generate("newConf");
        for (CallEvent event : firstConferenceCalls)
            Instance.Calls.Conferences.move(event, newConferenceId);
        for (CallEvent event : secConferenceCalls)
            Instance.Calls.Conferences.move(event, newConferenceId);

        Instance.Calls.Conferences.setActive(newConferenceId);
    }


    /**
     * Perform a call action
     *
     * @param phoneNumber the number on which to perform an action
     * @param dialAction the action to perform
     * @param source the GUI location from where the action was triggered
     * @param transients transient data to store into the newly created CallEvent
     * @return Whether the action was successfully initiated.
     */
    //******************************************************************
    public static boolean performCallAction(@NonNull String phoneNumber,
                                            @NonNull DialAction dialAction,
                                            @Nullable String source,
                                            @Nullable Json.Dict transients)
    //******************************************************************
    {
        StreamParty party = new StreamParty();
        party.setCurrentTransportUri(phoneNumber);
        party.match(Instance.Registration.getDefaultAccountId());
        return performCallAction(party, dialAction, source, transients);
    }

    /**
     * Perform a call action
     *
     * @param party the party on which to perform an action
     * @param dialAction the action to perform
     * @param source the GUI location from where the action was triggered
     * @param transients transient data to store into the newly created CallEvent
     * @return Whether the action was successfully initiated.
     */
    //******************************************************************
    public static boolean performCallAction(@NonNull StreamParty party,
                                            @NonNull DialAction dialAction,
                                            @Nullable String source,
                                            @Nullable Json.Dict transients)
    //******************************************************************
    {
        return performCallAction(party, dialAction, source, transients,
                Instance.Registration.getDefaultAccountId());
    }

    /**
     * Perform a call action
     *
     * @param phoneNumber the number on which to perform an action
     * @param dialAction the action to perform
     * @param source the GUI location from where the action was triggered
     * @param transients transient data to store into the newly created CallEvent
     * @param accountId the id of account to be used to perform action
     * @return Whether the action was successfully initiated.
     */
    //******************************************************************
    public static boolean performCallFromAccount(@NonNull String phoneNumber,
                                                 @NonNull DialAction dialAction,
                                                 @Nullable String source,
                                                 @Nullable Json.Dict transients,
                                                 @Nullable String accountId)
    //******************************************************************
    {
        StreamParty party = new StreamParty();
        party.setCurrentTransportUri(phoneNumber);
        party.match(accountId);
        return performCallAction(party, dialAction, source, transients, accountId);
    }

    /**
     * Perform a call action
     *
     * @param party the party on which to perform an action
     * @param dialAction the action to perform
     * @param source the GUI location from where the action was triggered
     * @param transients transient data to store into the newly created CallEvent
     * @param accountId the id of account to be used to perform action
     * @return Whether the action was successfully initiated.
     */
    //******************************************************************
    public static boolean performCallAction(@NonNull StreamParty party,
                                            @NonNull DialAction dialAction,
                                            @Nullable String source,
                                            @Nullable Json.Dict transients,
                                            @Nullable String accountId)
    //******************************************************************
    {
        CallEvent event = new CallEvent();
        event.setStream(EventStream.load(StreamQuery.legacyCallHistoryStreamKey()));
        event.setDirection(Event.Direction.OUTGOING);
        event.setAttribute(Event.Attributes.GUI, source);
        event.addRemoteUser(party.toRemoteUser());
        event.setAccount(accountId);
        event.transients.put(CallEvent.Transients.DIAL_ACTION, dialAction.id);

        if (transients != null)
            for (Map.Entry<String, Json> entry : transients.entrySet())
                event.transients.put(entry.getKey(), entry.getValue());

        int redirectMode = sRedirectState == null
                ? REDIRECT_NONE
                : sRedirectState.getRedirectMode();

        switch (redirectMode)
        {
            case REDIRECT_NONE:
            case REDIRECT_ATT_TRANSFER:
                int result = Instance.Events.post(event);
                return result == Instance.Events.PostResult.SUCCESS;
            case REDIRECT_TRANSFER:
                Instance.Calls.Conferences.beginTransfer(sRedirectState.getRedirectEvent());
                clearRedirectState();
                return Instance.Events.post(event) == Instance.Events.PostResult.SUCCESS;
            case REDIRECT_FORWARD:
                Instance.Calls.Conferences.beginForward(sRedirectState.getRedirectEvent());
                clearRedirectState();
                return Instance.Events.post(event) == Instance.Events.PostResult.SUCCESS;
        }
        return false;
    }

    /**
     * Begin call redirection flow.
     *
     * @param callEvent The call to be redirected.
     * @param mode The redirection mode.
     */
    //******************************************************************
    public static void beginRedirect(@NonNull CallEvent callEvent, @RedirectMode int mode)
    //******************************************************************
    {
        sRedirectState = new RedirectStateHolder(mode, callEvent);

        switch (mode)
        {
            case REDIRECT_TRANSFER:
            case REDIRECT_ATT_TRANSFER:
                Instance.Calls.setHeld(callEvent, true);
                break;
            case REDIRECT_FORWARD:
                Instance.Calls.ignoreIncoming(callEvent);
                break;
            case REDIRECT_NONE:
            default:
                break;
        }
    }

    /**
     * Reset call redirection state.
     */
    //******************************************************************
    public static void clearRedirectState()
    //******************************************************************
    {
        sRedirectState = null;
    }

    //******************************************************************
    public static int getRedirectMode()
    //******************************************************************
    {
        return sRedirectState == null ? REDIRECT_NONE : sRedirectState.getRedirectMode();
    }

    /**
     * Cancel call redirection flow.
     */
    //******************************************************************
    public static void cancelRedirect()
    //******************************************************************
    {
        if(sRedirectState == null)
            return;

        switch (sRedirectState.getRedirectMode())
        {
            case REDIRECT_TRANSFER:
            case REDIRECT_ATT_TRANSFER:
                Instance.Calls.setHeld(sRedirectState.getRedirectEvent(), false);
                break;
            case REDIRECT_NONE:
            case REDIRECT_FORWARD:
                break;
        }

        sRedirectState = null;
    }

    /**
     * Split a conference
     *
     * @param confId The ID of the conference to split.
     */
    //******************************************************************
    public static void split(@NonNull String confId)
    //******************************************************************
    {
        CallEvent[] confCalls = Instance.Calls.Conferences.getCalls(confId);
        for (CallEvent callEvent : confCalls)
        {
            Instance.Calls.Conferences.split(callEvent,false);
            Instance.Calls.setHeld(callEvent, true);
        }
    }

    /**
     * Check if a call is part of conference
     *
     * @param call The call to be checked if it belongs to a conference.
     * @return Whether the call belongs to a conference.
     */
    //******************************************************************
    public static boolean isConference(@NonNull CallEvent call)
    //******************************************************************
    {
        return Instance.Calls.Conferences.getSize(call) > 1;
    }

    /**
     * Find all alternative conferences for a call
     *
     * @param selectedCall The id of id to find alternative conferences for.
     * @return List of all conferences with active calls excluding the one selected call is part of.
     */
    //******************************************************************
    public static List<String> getAlternativeConferences(@NonNull CallEvent selectedCall)
    //******************************************************************
    {
        String selectedConference = Instance.Calls.Conferences.get(selectedCall);
        List<String> candidateConferences = new ArrayList<>();
        if(Instance.Calls.Conferences.count() < 2)
            return candidateConferences;
        for(String conference: Instance.Calls.Conferences.list())
        {
            if(!selectedConference.equals(conference) && groupContainsCallInNonTerminalState(conference))
                candidateConferences.add(conference);
        }
        return candidateConferences;
    }

    /**
     * Check if conference contains a call in non-terminal state.
     *
     * @param confId The conference id.
     * @return Whether the conference contains a call in non-terminal state.
     */
    //******************************************************************
    public static boolean groupContainsCallInNonTerminalState(@NonNull String confId)
    //******************************************************************
    {
        for(CallEvent call : Instance.Calls.Conferences.getCalls(confId))
        {
            if(!Instance.Calls.getState(call).isTerminal())
                return true;
        }
        return false;
    }


    /**
     * Get number of established calls.
     *
     * @return Number of calls in {@linkplain Call.State#Established state}.
     */
    //******************************************************************
    public static int getEstablishedSipCallCount()
    //******************************************************************
    {
        int establishedCallCount = 0;

        for (String conf: Instance.Calls.Conferences.list())
        {
            for(CallEvent call : Instance.Calls.Conferences.getCalls(conf))
            {
                if(Instance.Calls.getState(call) != Call.State.Established)
                    continue;
                establishedCallCount++;
            }
        }
        return establishedCallCount;
    }

    //******************************************************************
    public static CallEvent getCallById(long eventId)
    //******************************************************************
    {
        if (eventId > 0)
            for (String conference : Instance.Calls.Conferences.list())
                for (CallEvent call : Instance.Calls.Conferences.getCalls(conference))
                    if (call.getEventId() == eventId)
                        return call;
        return null;
    }
}