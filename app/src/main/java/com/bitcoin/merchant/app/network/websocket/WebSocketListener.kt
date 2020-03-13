package com.bitcoin.merchant.app.network.websocket

import com.bitcoin.merchant.app.model.PaymentReceived

interface WebSocketListener {
    fun onIncomingPayment(paymentReceived: PaymentReceived?)
}