package com.bitcoin.merchant.app.network.paybitcoincom;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RequestHelper {
    private static final String API_URL = "https://pay.bitcoin.com/";
    private static final String ENDPOINT_PAIRING_CODE = "use-pairing-code";
    private static final String ENDPOINT_REGISTER_MERCHANT = "register-merchant";
    private static final String ENDPOINT_CREATE_INVOICE = "create_invoice";

    //TODO use the actual endpoints rather than these placeholders since i can't remember the real ones right now
    public String usePairingCode(String json) throws IOException {
        return this.post(ENDPOINT_PAIRING_CODE, json);
    }

    public String registerMerchant(String json) throws IOException {
        return this.post(ENDPOINT_REGISTER_MERCHANT, json);
    }

    public String createInvoice(String json) throws IOException {
        return this.post(ENDPOINT_CREATE_INVOICE, json);
    }

    private String post(String endpoint, String json) throws IOException {
        URL url = new URL(API_URL + endpoint);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setInstanceFollowRedirects(false);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setUseCaches(false);
        con.connect();
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.write(json.getBytes());
        wr.flush();
        wr.close();

        BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
        return rd.readLine();
    }
}
