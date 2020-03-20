package com.bitcoin.merchant.app.screens

import android.content.*
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bitcoin.merchant.app.MainActivity
import com.bitcoin.merchant.app.R
import com.bitcoin.merchant.app.model.Analytics
import com.bitcoin.merchant.app.model.CountryCurrencyLocale
import com.bitcoin.merchant.app.model.PaymentTarget
import com.bitcoin.merchant.app.screens.dialogs.DialogHelper
import com.bitcoin.merchant.app.screens.dialogs.SnackHelper
import com.bitcoin.merchant.app.screens.features.ToolbarAwareFragment
import com.bitcoin.merchant.app.util.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bitcoindotcom.bchprocessor.bip70.Bip70Manager
import org.bitcoindotcom.bchprocessor.bip70.Bip70PayService
import org.bitcoindotcom.bchprocessor.bip70.model.Bip70Action
import org.bitcoindotcom.bchprocessor.bip70.model.InvoiceRequest
import org.bitcoindotcom.bchprocessor.bip70.model.InvoiceStatus
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.*

class CurrencySelectionFragment : ToolbarAwareFragment() {
    // Ensure that pressing 'BACK' button stays on the 'Payment REQUEST' screen to NOT lose the active invoice
    // unless we are exiting the screen
    private var backButtonAllowed: Boolean = true
    lateinit var currencies: List<CountryCurrencyLocale>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val v = inflater.inflate(R.layout.fragment_currency_selection, container, false)
        initViews(v)
        setToolbarVisible(true)
        return v
    }

    private fun initViews(v: View) {
        currencies = CountryCurrencyLocale.getAll(activity)
    }

    private fun save(cc: CountryCurrencyLocale) {
        if (Settings.getCountryCurrencyLocale(activity) != cc) {
            Settings.setCountryCurrencyLocale(activity, cc)
            Analytics.settings_currency_changed.send()
            SnackHelper.show(activity, activity.getString(R.string.notify_changes_have_been_saved))
        }
        val intent = Intent(PaymentInputFragment.ACTION_INTENT_RESET_AMOUNT)
        LocalBroadcastManager.getInstance(activity).sendBroadcast(intent)
        //TODO set currency summary
        //settingsController.setCurrencySummary(cc)
    }

    override val isBackAllowed: Boolean
        get() {
            return backButtonAllowed
        }

    companion object {

    }
}