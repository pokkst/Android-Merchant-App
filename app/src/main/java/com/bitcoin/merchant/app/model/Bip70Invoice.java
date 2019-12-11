package com.bitcoin.merchant.app.model;

import com.bitcoin.merchant.app.model.websocket.Tx;

public class Bip70Invoice {
    public String apiKey;
    public String address;
    public long amount;
    public String fiat;
    public String memo;
}
