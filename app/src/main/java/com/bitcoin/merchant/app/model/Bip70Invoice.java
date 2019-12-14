package com.bitcoin.merchant.app.model;

import androidx.annotation.Keep;

@Keep
public class Bip70Invoice {
    public String apiKey;
    public String address;
    public long amount;
    public String fiat;
    public String memo;
}
