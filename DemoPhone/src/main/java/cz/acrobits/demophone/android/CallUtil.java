package cz.acrobits.demophone.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import cz.acrobits.ali.Json;
import cz.acrobits.ali.Log;
import cz.acrobits.libsoftphone.Instance;
import cz.acrobits.libsoftphone.InstanceExt;
import cz.acrobits.libsoftphone.data.Call;
import cz.acrobits.libsoftphone.data.DialAction;
import cz.acrobits.libsoftphone.event.CallEvent;
import cz.acrobits.libsoftphone.event.Event;
import cz.acrobits.libsoftphone.event.EventStream;
import cz.acrobits.libsoftphone.event.StreamParty;
import cz.acrobits.libsoftphone.event.history.StreamQuery;

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

        Stream<CallEvent> firstConferenceCalls = InstanceExt.Calls.Conferences.getAllCalls(firstConferenceId);
        Stream<CallEvent> secConferenceCalls = InstanceExt.Calls.Conferences.getAllCalls(secondConferenceId);

        String newConferenceId = Instance.Calls.Conferences.generate("newConf");
        Stream.concat(firstConferenceCalls, secConferenceCalls)
                .forEach(call -> Instance.Calls.Conferences.move(call, newConferenceId));

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

        return Instance.Events.post(event) == Instance.Events.PostResult.SUCCESS;
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
        InstanceExt.Calls.Conferences.getAllCalls(confId)
                .forEach(callEvent ->
                        {
                            Instance.Calls.Conferences.split(callEvent, false);
                            Instance.Calls.setHeld(callEvent, true);
                        });
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
        return InstanceExt.Calls.Conferences.getAllCalls(confId).anyMatch(call -> !Instance.Calls.getState(call).isTerminal());
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