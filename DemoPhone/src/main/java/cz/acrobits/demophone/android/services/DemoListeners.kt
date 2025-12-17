package cz.acrobits.demophone.android.services

import android.content.Context
import cz.acrobits.ali.Log
import cz.acrobits.libsoftphone.event.CallEvent
import cz.acrobits.libsoftphone.support.Listeners
import java.io.IOException

class DemoListeners(private val context: Context) : Listeners()
{
    companion object
    {
        private val LOG = Log("DemoListeners")
    }

    override fun getRingtone(p0: CallEvent): kotlin.Any?
    {
        try
        {
            return context.resources.assets.openFd("relax_ringtone.mp3")
        }
        catch (e: IOException)
        {
            LOG.error("Failed to open ringtone: %s", e);
        }

        return null;
    }
}