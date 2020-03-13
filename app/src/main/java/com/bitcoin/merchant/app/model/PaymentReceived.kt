package com.bitcoin.merchant.app.model

import android.content.Intent
import com.bitcoin.merchant.app.network.ExpectedAmounts

class PaymentReceived {
    val addr: String?
    val bchReceived: Long
    val bchExpected: Long
    val fiatExpected: String?
    val txHash: String?
    val timeInSec: Long
    val confirmations: Int

    constructor(addr: String?, bchReceived: Long, txHash: String?, timeInSec: Long, confirmations: Int,
                expected: ExpectedAmounts) {
        this.addr = addr
        this.bchReceived = bchReceived
        bchExpected = expected.bch
        fiatExpected = expected.fiat
        this.txHash = txHash
        this.timeInSec = timeInSec
        this.confirmations = confirmations
    }

    constructor(intent: Intent) {
        addr = intent.getStringExtra("payment_address")
        bchReceived = intent.getLongExtra("payment_received_amount", 0L)
        bchExpected = intent.getLongExtra("payment_expected_amount", 0L)
        fiatExpected = intent.getStringExtra("payment_expected_fiat")
        txHash = intent.getStringExtra("payment_tx_hash")
        timeInSec = intent.getLongExtra("payment_ts_seconds", 0L)
        confirmations = intent.getIntExtra("payment_conf", 0)
    }

    fun toIntent(intent: Intent) {
        intent.putExtra("payment_address", addr)
        intent.putExtra("payment_received_amount", bchReceived)
        intent.putExtra("payment_expected_amount", bchExpected)
        intent.putExtra("payment_expected_fiat", fiatExpected)
        intent.putExtra("payment_tx_hash", txHash)
        intent.putExtra("payment_ts_seconds", timeInSec)
        intent.putExtra("payment_conf", confirmations)
    }

    val isUnderpayment: Boolean
        get() = bchReceived < bchExpected

    val isOverpayment: Boolean
        get() = bchReceived > bchExpected

    override fun toString(): String {
        return "PaymentReceived{" +
                "txHash='" + txHash + '\'' +
                ", addr='" + addr + '\'' +
                ", bchReceived=" + bchReceived +
                ", bchExpected=" + bchExpected +
                ", fiatExpected='" + fiatExpected + '\'' +
                ", timeInSec=" + timeInSec +
                ", confirmations=" + confirmations +
                '}'
    }
}