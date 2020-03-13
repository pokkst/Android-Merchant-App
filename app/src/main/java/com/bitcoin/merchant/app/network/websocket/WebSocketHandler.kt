package com.bitcoin.merchant.app.network.websocket

/**
 * Socket will reconnect even without calling again webSocketHandler.start()
 * and without ACTION_INTENT_RECONNECT being sent by the ConnectivityManager.
 * The number of Thread will stay constant about 17 or 18 on OS v5.
 */
interface WebSocketHandler {
    fun start()
    fun stop()
    val isConnected: Boolean
}