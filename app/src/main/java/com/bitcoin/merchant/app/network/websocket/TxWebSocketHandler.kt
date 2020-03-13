package com.bitcoin.merchant.app.network.websocket

interface TxWebSocketHandler : WebSocketHandler {
    fun setListener(webSocketListener: WebSocketListener?)
    fun subscribeToAddress(address: String)
}