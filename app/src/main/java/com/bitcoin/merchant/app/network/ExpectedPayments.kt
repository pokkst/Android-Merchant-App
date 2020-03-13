package com.bitcoin.merchant.app.network

import android.util.Log
import java.util.*

class ExpectedPayments private constructor() {
    private val addressToAmounts: MutableMap<String, ExpectedAmounts?>
    fun addExpectedPayment(receivingAddress: String, bchAmount: Long, fiatAmount: String?) {
        addressToAmounts[receivingAddress] = ExpectedAmounts(bchAmount, fiatAmount)
        Log.i(ExpectedPayments::class.java.simpleName, addressToAmounts.size.toString() + " Pending payments: " + addressToAmounts.toString())
    }

    fun removePayment(receivingAddress: String?) {
        addressToAmounts.remove(receivingAddress)
        Log.i(ExpectedPayments::class.java.simpleName, addressToAmounts.size.toString() + " Pending payments: " + addressToAmounts.toString())
    }

    fun getExpectedAmounts(receivingAddress: String?): ExpectedAmounts {
        val amounts = addressToAmounts[receivingAddress]
        return amounts ?: ExpectedAmounts.Companion.UNDEFINED
    }

    fun isValidAddress(receivingAddress: String?): Boolean {
        return addressToAmounts.containsKey(receivingAddress)
    }

    val addresses: Set<String>
        get() = TreeSet(addressToAmounts.keys)

    companion object {
        val instance = ExpectedPayments()
    }

    init {
        val cache: Map<String, ExpectedAmounts> = object : LinkedHashMap<String, ExpectedAmounts>() {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ExpectedAmounts>?): Boolean {
                return size > 8
            }
        }
        addressToAmounts = Collections.synchronizedMap(cache)
    }
}