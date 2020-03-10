package com.bitcoin.merchant.app.network.websocket.impl.bitcoincom;

import android.util.Log;

import com.bitcoin.merchant.app.model.PaymentReceived;
import com.bitcoin.merchant.app.model.Tx;
import com.bitcoin.merchant.app.network.ExpectedAmounts;
import com.bitcoin.merchant.app.network.ExpectedPayments;
import com.bitcoin.merchant.app.network.websocket.impl.TxWebSocketHandlerImpl;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;

import org.bitcoindotcom.bchprocessor.bip70.GsonHelper;

import java.io.IOException;

public class BitcoinComSocketHandler extends TxWebSocketHandlerImpl {
    public BitcoinComSocketHandler() {
        TAG = "BitcoinComSocket";
    }

    @Override
    protected WebSocket createWebSocket(WebSocketFactory factory) throws IOException {
        return factory.createSocket("wss://bch.api.wallet.bitcoin.com/bws/api/socket/v1/address");
    }

    @Override
    protected void parseTx(String message) throws Exception {
        Tx tx = GsonHelper.INSTANCE.getGson().fromJson(message, Tx.class);
        if (tx != null && tx.outputs != null) {
            Log.i(TAG, "TX found:" + tx);
            for (Tx.Output o : tx.outputs) {
                String addr = o.address;
                if (ExpectedPayments.getInstance().isValidAddress(addr)) {
                    ExpectedAmounts expected = ExpectedPayments.getInstance().getExpectedAmounts(addr);
                    long bchReceived = o.value;
                    if (webSocketListener != null) {
                        long timeInSec = System.currentTimeMillis() / 1000;
                        PaymentReceived payment = new PaymentReceived(addr, bchReceived, tx.txid, timeInSec, 0, expected);
                        Log.i(TAG, "expected payment:" + payment);
                        webSocketListener.onIncomingPayment(payment);
                    }
                }
            }
        }
    }
}
