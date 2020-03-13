package com.bitcoin.merchant.app.network.websocket.impl.bitcoincom

import android.util.Log
import com.bitcoin.merchant.app.model.PaymentReceived
import com.bitcoin.merchant.app.model.Tx
import com.bitcoin.merchant.app.network.ExpectedAmounts
import com.bitcoin.merchant.app.network.ExpectedPayments
import com.bitcoin.merchant.app.network.websocket.impl.TxWebSocketHandlerImpl
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketFactory
import org.bitcoindotcom.bchprocessor.bip70.GsonHelper.gson
import java.io.IOException

class BitcoinComSocketHandler : TxWebSocketHandlerImpl() {
    @Throws(IOException::class)
    override fun createWebSocket(factory: WebSocketFactory): WebSocket {
        return factory.createSocket("wss://bch.api.wallet.bitcoin.com/bws/api/socket/v1/address")
    }

    @Throws(Exception::class)
    override fun parseTx(message: String?) {
        val tx = gson.fromJson(message, Tx::class.java)
        if (tx != null && tx.outputs != null) {
            Log.i(TAG, "TX found:$tx")
            for (o in tx.outputs!!) {
                val addr = o.address
                if (ExpectedPayments.instance.isValidAddress(addr)) {
                    val expected: ExpectedAmounts = ExpectedPayments.instance.getExpectedAmounts(addr)
                    val bchReceived = o.value
                    if (webSocketListener != null) {
                        val timeInSec = System.currentTimeMillis() / 1000
                        val payment = PaymentReceived(addr, bchReceived, tx.txid, timeInSec, 0, expected)
                        Log.i(TAG, "expected payment:$payment")
                        webSocketListener!!.onIncomingPayment(payment)
                    }
                }
            }
        }
    }

    init {
        TAG = "BitcoinComSocket"
    }
}