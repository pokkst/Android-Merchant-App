package com.bitcoin.merchant.app.network.paybitcoincom;

import androidx.annotation.Keep;

@Keep
public class Bip70InvoiceJson {
    public String apiKey;
    public String address;
    public long amount;
    public String fiat;
    public String memo;
}
