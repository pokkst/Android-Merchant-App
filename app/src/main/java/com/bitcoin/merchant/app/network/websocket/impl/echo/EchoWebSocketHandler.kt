package com.bitcoin.merchant.app.network.websocket.impl.echo

import com.bitcoin.merchant.app.network.websocket.impl.TxWebSocketHandlerImpl
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketFactory
import java.io.IOException

class EchoWebSocketHandler : TxWebSocketHandlerImpl() {
    @Throws(IOException::class)
    override fun createWebSocket(factory: WebSocketFactory): WebSocket {
        return factory.createSocket("wss://echo.websocket.org")
    }

    @Throws(Exception::class)
    override fun parseTx(message: String?) {
    }

    init {
        TAG = "NoOpSocket"
    }
}