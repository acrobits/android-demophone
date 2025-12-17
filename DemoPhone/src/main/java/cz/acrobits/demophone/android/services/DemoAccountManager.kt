package cz.acrobits.demophone.android.services

import android.content.Context
import cz.acrobits.ali.Xml
import cz.acrobits.libsoftphone.Instance
import cz.acrobits.libsoftphone.account.AccountXml
import cz.acrobits.libsoftphone.data.Account
import cz.acrobits.libsoftphone.mergeable.MergeableNodeAttributes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import kotlin.math.abs

class DemoAccountManager(private val context: Context)
{
    companion object
    {
        /** Password for test account.  */
        private val PASSWORD = byteArrayOf(
            0x6d.toByte(),
            0x69.toByte(),
            0x73.toByte(),
            0x73.toByte(),
            0x63.toByte(),
            0x6f.toByte(),
            0x6d.toByte()
        )
    }

    private val _currentNumber = MutableStateFlow<String?>(null)
    val currentNumber: StateFlow<String?> = _currentNumber

    init
    {
        if (Instance.Registration.getAccountCount() == 0)
            createDemoAccount()
        else
        {
            // Load current number from existing account
            val accountXml = Instance.Registration.getAccount(0)
            _currentNumber.value = accountXml.getString("username")
        }
    }

    private fun createDemoAccount()
    {
        var code = 0
        for (b in Instance.System.getUniqueId().toByteArray())  // djb2 hash
            code = (code shl 6) + code + b

        _currentNumber.value = String.format(Locale.ROOT, "10%02d", abs(code % 100))

        // Register a test account on Acrobits PBX
        val accountXml = Xml("account").apply {
            setAttribute(Account.Attributes.ID, "Test Account")
            setChildValue(Account.USERNAME, _currentNumber.value!!)
            setChildValue(Account.PASSWORD, String(PASSWORD))
            setChildValue(Account.HOST, "pbx.acrobits.cz")

            // Example of setting DTMF order
            setChildValue(Account.DTMF_ORDER, "rfc2833,audio")
        }

        // Create/update an account
        /* The account is updated based on ID. If you do not specify ID, an unique one is generated
             * but each start will create a new account in the app. But we have fixed ID in the XML
             * above so if the account already exists, it's only updated. */
        Instance.Registration.saveAccount(AccountXml(accountXml, MergeableNodeAttributes.gui()))
    }
}
