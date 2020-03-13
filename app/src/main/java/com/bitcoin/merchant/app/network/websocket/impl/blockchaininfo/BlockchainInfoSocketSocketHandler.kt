package com.bitcoin.merchant.app.network.websocket.impl.blockchaininfo

import android.util.Log
import com.bitcoin.merchant.app.model.PaymentReceived
import com.bitcoin.merchant.app.network.ExpectedAmounts
import com.bitcoin.merchant.app.network.ExpectedPayments
import com.bitcoin.merchant.app.network.websocket.impl.TxWebSocketHandlerImpl
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketFactory
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class BlockchainInfoSocketSocketHandler : TxWebSocketHandlerImpl() {
    @Throws(IOException::class)
    override fun createWebSocket(factory: WebSocketFactory): WebSocket {
        return factory.createSocket("wss://ws.blockchain.info/bch/inv")
                .addHeader("Origin", "https://blockchain.info")
    }

    @Throws(JSONException::class)
    override fun parseTx(message: String?) {
        val jsonObject: JSONObject?
        jsonObject = try {
            JSONObject(message)
        } catch (je: JSONException) {
            null
        }
        if (jsonObject == null) {
            return
        }
        val op = jsonObject["op"] as String
        if (op == "utx" && jsonObject.has("x")) {
            val objX = jsonObject["x"] as JSONObject
            var foundAddr: String? = null
            var bchReceived = 0L
            val txHash = objX["hash"] as String
            Log.i(TAG, "TX found:$txHash")
            if (objX.has("out")) {
                val outArray = objX["out"] as JSONArray
                for (j in 0 until outArray.length()) {
                    val outObj = outArray[j] as JSONObject
                    if (outObj.has("addr")) {
                        val addr = outObj["addr"] as String
                        if (ExpectedPayments.instance.isValidAddress(addr)) {
                            foundAddr = addr
                            bchReceived = if (outObj.has("value")) outObj.getLong("value") else 0
                            break
                        }
                    }
                }
            }
            if (bchReceived > 0L && foundAddr != null && ExpectedPayments.instance.isValidAddress(foundAddr)) {
                val expected: ExpectedAmounts = ExpectedPayments.instance.getExpectedAmounts(foundAddr)
                if (webSocketListener != null) {
                    val timeInSec = System.currentTimeMillis() / 1000
                    val payment = PaymentReceived(foundAddr, bchReceived, txHash, timeInSec, 0, expected)
                    Log.i(TAG, "expected payment:$payment")
                    webSocketListener!!.onIncomingPayment(payment)
                }
            }
        }
    }

    init {
        TAG = "BlockchainInfoSocket"
    }
}