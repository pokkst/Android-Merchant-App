package com.bitcoin.merchant.app.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bitcoin.merchant.app.Action;
import com.bitcoin.merchant.app.MainActivity;

/**
 * Created by riaanvos on 11/12/15.
 */
public class NetworkStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getExtras() != null) {
            //TODO reconnect
           // LocalBroadcastManager.getInstance(context).sendBroadcastSync(new Intent(Action.ACTION_INTENT_RECONNECT));
        }
    }
}