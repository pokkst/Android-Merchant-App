package com.bitcoin.merchant.app.model

import com.google.gson.annotations.SerializedName
import org.bitcoindotcom.bchprocessor.bip70.GsonHelper
import java.util.*

data class InvoiceRequestSlp @JvmOverloads constructor(
        @SerializedName("tokenId") var tokenId: String,
        @SerializedName("amount") var amount: Double,
        @SerializedName("address") var address: String? = null) {
    companion object {
        @JvmStatic
        fun fromJson(message: String): InvoiceRequestSlp {
            return GsonHelper.gson.fromJson(message, InvoiceRequestSlp::class.java)
        }
    }
}