package com.bitcoin.merchant.app.network.paybitcoincom;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RequestHelper {
    public String post(String endpoint, String json) throws IOException {
        URL url = new URL("https://api.pay.bitcoin.com/" + endpoint);
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
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
