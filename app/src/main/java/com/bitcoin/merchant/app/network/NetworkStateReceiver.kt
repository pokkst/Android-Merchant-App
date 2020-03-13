package com.bitcoin.merchant.app.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Created by riaanvos on 11/12/15.
 */
class NetworkStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.extras != null) { //TODO reconnect
// LocalBroadcastManager.getInstance(context).sendBroadcastSync(new Intent(Action.ACTION_INTENT_RECONNECT));
        }
    }
}