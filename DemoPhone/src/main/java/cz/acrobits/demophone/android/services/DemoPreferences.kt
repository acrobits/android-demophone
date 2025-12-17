package cz.acrobits.demophone.android.services

import android.content.Context
import cz.acrobits.libsoftphone.SDK
import cz.acrobits.preferences.VideoPreferences

// Make sure you inherit from VideoPreferences when you want to use video features
class DemoPreferences(private val context: Context) : VideoPreferences()
{
    override fun overrideDefaults()
    {
        super.overrideDefaults()

        val applicationVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName!!;

        // This is how you can redefine the User-Agent:
        userAgentOverride.overrideDefault(String.format(
            "Acrobits DemoPhone/%s (build %s; demo)",
            applicationVersion,
            SDK.build
        ))
    }
}