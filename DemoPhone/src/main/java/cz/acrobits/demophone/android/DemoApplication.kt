package cz.acrobits.demophone.android

import android.app.Application
import cz.acrobits.demophone.android.services.DemoServices

class DemoApplication : Application()
{
    override fun onCreate()
    {
        super.onCreate()

        // Initialize Demo SDK singleton
        DemoServices.initialize(this)
    }
}