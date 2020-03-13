package com.bitcoin.merchant.app.network.websocket.impl

import android.util.Log
import com.bitcoin.merchant.app.network.ExpectedPayments
import com.bitcoin.merchant.app.network.websocket.TxWebSocketHandler
import com.bitcoin.merchant.app.network.websocket.WebSocketListener
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketFactory
import com.neovisionaries.ws.client.WebSocketFrame
import java.io.IOException
import java.util.*

/**
 * Socket will reconnect even without calling again webSocketHandler.start()
 * and without ACTION_INTENT_RECONNECT being sent by the ConnectivityManager.
 * The number of Thread will stay constant about 17 or 18 on OS v5.
 */
abstract class TxWebSocketHandlerImpl : TxWebSocketHandler {
    private val webSocketFactory: WebSocketFactory
    @Volatile
    private var handler: ConnectionHandler? = null
    protected var webSocketListener: WebSocketListener? = null
    protected var TAG = "WebSocketHandler"
    override fun setListener(webSocketListener: WebSocketListener?) {
        this.webSocketListener = webSocketListener
    }

    override fun start() {
        try {
            Log.i(TAG, "start threads:" + Thread.activeCount())
            stop()
            ConnectionThread().start()
        } catch (e: Exception) {
            Log.e(TAG, "start", e)
        }
    }

    override fun stop() {
        if (handler != null) {
            handler!!.stop()
        }
    }

    override val isConnected: Boolean
        get() = handler != null && handler!!.isConnected() && !handler!!.isBroken

    private fun send(message: String) {
        if (handler != null) {
            handler!!.send(message)
        }
    }

    @Synchronized
    override fun subscribeToAddress(address: String) {
        send(getSubscribeMessage(address))
    }

    private inner class ConnectionThread : Thread() {
        override fun run() {
            try {
                handler = ConnectionHandler()
            } catch (e: Exception) {
                Log.e(TAG, "Connect", e)
            }
        }

        init {
            name = "ConnectWebSocket"
            isDaemon = true
        }
    }

    @Throws(IOException::class)
    protected abstract fun createWebSocket(factory: WebSocketFactory): WebSocket

    @Throws(Exception::class)
    protected abstract fun parseTx(message: String?)

    private inner class ConnectionHandler : WebSocketAdapter() {
        private val sentMessageSet: MutableSet<String> = HashSet()
        private val mConnection: WebSocket?
        @Volatile
        private var autoReconnect = true
        private var timeLastAlive: Long
        @Throws(Exception::class)
        override fun onPongFrame(websocket: WebSocket, frame: WebSocketFrame) {
            super.onPongFrame(websocket, frame)
            timeLastAlive = System.currentTimeMillis()
            Log.d(TAG, "PongSuccess threads:" + Thread.activeCount())
        }

        @Throws(Exception::class)
        override fun onPingFrame(websocket: WebSocket, frame: WebSocketFrame) {
            super.onPingFrame(websocket, frame)
            timeLastAlive = System.currentTimeMillis()
            Log.d(TAG, "PingSuccess threads:" + Thread.activeCount())
        }

        @Throws(Exception::class)
        override fun onConnected(websocket: WebSocket, headers: Map<String, List<String>>) {
            super.onConnected(websocket, headers)
            Log.i(TAG, "onConnected threads:" + Thread.activeCount())
        }

        @Throws(Exception::class)
        override fun onDisconnected(websocket: WebSocket, serverCloseFrame: WebSocketFrame, clientCloseFrame: WebSocketFrame, closedByServer: Boolean) {
            super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer)
            Log.e(TAG, "onDisconnected threads:" + Thread.activeCount())
            if (autoReconnect) { // reconnect on involuntary disconnection
                start()
            }
        }

        override fun onTextMessage(websocket: WebSocket, message: String) {
            try {
                parseTx(message)
            } catch (e: Exception) {
                Log.e(TAG, message, e)
            }
        }

        fun isConnected(): Boolean {
            return mConnection != null && mConnection.isOpen
        }

        fun stop() {
            if (isConnected()) {
                autoReconnect = false
                mConnection!!.clearListeners()
                mConnection.disconnect()
            }
        }

        fun send(message: String) { // Make sure each message is only sent once per socket lifetime
            if (!sentMessageSet.contains(message)) {
                try {
                    if (isConnected()) {
                        directSend(message)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "send:$message")
                }
            }
        }

        private fun directSend(message: String) {
            mConnection!!.sendText(message)
            sentMessageSet.add(message)
        }

        // considered broken when older than 1 minute and with no ping or pong during that time
        val isBroken: Boolean
            get() =// considered broken when older than 1 minute and with no ping or pong during that time
                timeLastAlive + MINUTE_IN_MS < System.currentTimeMillis()

        val MINUTE_IN_MS = 60 * 1000


        init {
            timeLastAlive = System.currentTimeMillis()
            mConnection = createWebSocket(webSocketFactory)
                    .recreate()
                    .addListener(this)
            mConnection.pingInterval = PING_INTERVAL
            mConnection.connect()
            for (address in ExpectedPayments.instance.addresses) {
                directSend(getSubscribeMessage(address))
            }
            timeLastAlive = System.currentTimeMillis()
        }
    }

    companion object {
        private const val PING_INTERVAL = 20 * 1000L // ping every 20 seconds
        private fun getSubscribeMessage(address: String): String {
            return "{\"op\":\"addr_sub\", \"addr\":\"$address\"}"
        }
    }

    init {
        webSocketFactory = WebSocketFactory()
    }
}