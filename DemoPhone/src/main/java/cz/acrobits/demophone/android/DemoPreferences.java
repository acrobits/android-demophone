package cz.acrobits.demophone.android;

import cz.acrobits.ali.AndroidUtil;
import cz.acrobits.libsoftphone.Preferences;
import cz.acrobits.libsoftphone.SDK;

/**
 * DemoPhone preferences.
 */
//******************************************************************
public class DemoPreferences
        extends Preferences
//******************************************************************
{
    //******************************************************************
    @Override
    protected void overrideDefaults()
    //******************************************************************
    {
        super.overrideDefaults();
        // This is how you can redefine the User-Agent:
        userAgentOverride.overrideDefault(String.format("Acrobits DemoPhone/%s (build %s; demo)",
                AndroidUtil.getApplicationVersion(),
                SDK.build));
    }
}
