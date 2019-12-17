package com.bitcoin.merchant.app.network.paybitcoincom;

import androidx.annotation.Keep;

@Keep
public class RegisterMerchantJson {
    public String businessName;
    public String emailAddress;
    //TODO: change businessType to enum
    public String businessType;
    public String country;
}
