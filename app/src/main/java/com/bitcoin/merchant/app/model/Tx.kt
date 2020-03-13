package com.bitcoin.merchant.app.model

import java.util.*

// {
// "txid":"ABCDEF...", "fees":0, "confirmations":0, "amount":27420,
// "outputs": [{"address":"1...", "value":27420}]
// }
class Tx {
    // do NOT change names as they are used by Gson
    var txid: String? = null
    var fees: Long = 0
    var confirmations: Long = 0
    var amount: Long = 0
    var outputs: Array<Output>? = null

    class Output {
        var address: String? = null
        var value: Long = 0
        override fun toString(): String {
            return "Output{" +
                    "address='" + address + '\'' +
                    ", value=" + value +
                    '}'
        }
    }

    override fun toString(): String {
        return "Tx{" +
                "txid='" + txid + '\'' +
                ", fees=" + fees +
                ", confirmations=" + confirmations +
                ", amount=" + amount +
                ", outputs=" + Arrays.toString(outputs) +
                '}'
    }
}