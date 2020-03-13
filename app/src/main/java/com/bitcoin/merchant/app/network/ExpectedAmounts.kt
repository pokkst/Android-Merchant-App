package com.bitcoin.merchant.app.network

class ExpectedAmounts(val bch: Long, val fiat: String?) {
    override fun toString(): String {
        return "ExpectedAmounts{" +
                "bch=" + bch +
                ", fiat='" + fiat + '\'' +
                '}'
    }

    companion object {
        val UNDEFINED = ExpectedAmounts(0, null)
    }

}