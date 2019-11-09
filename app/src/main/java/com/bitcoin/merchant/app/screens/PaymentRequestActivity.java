package com.bitcoin.merchant.app.screens;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bitcoin.merchant.app.MainActivity;
import com.bitcoin.merchant.app.R;
import com.bitcoin.merchant.app.network.ExpectedPayments;
import com.bitcoin.merchant.app.screens.dialogs.PaymentTooHighDialog;
import com.bitcoin.merchant.app.screens.dialogs.PaymentTooLowDialog;
import com.bitcoin.merchant.app.util.AmountUtil;
import com.bitcoin.merchant.app.util.AppUtil;
import com.bitcoin.merchant.app.util.MonetaryUtil;
import com.bitcoin.merchant.app.util.ToastCustom;
import com.github.kiulian.converter.AddressConverter;
import com.google.bitcoin.uri.BitcoinCashURI;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.Coin;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.bitcoin.merchant.app.MainActivity.TAG;

public class PaymentRequestActivity extends Activity {
    private LinearLayout waitingLayout = null;
    private LinearLayout receivedLayout = null;
    private TextView tvFiatAmount = null;
    private TextView tvBtcAmount = null;
    private ImageView ivReceivingQr = null;
    private LinearLayout progressLayout = null;
    private Button ivCancel = null;
    private Button ivDone = null;
    private TextView tvStatus = null;
    private String receivingBip70Invoice = null;
    private boolean hasAPIKey = false;
    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            if (MainActivity.ACTION_INTENT_EXPECTED_PAYMENT_RECEIVED.equals(intent.getAction())) {
                PaymentReceived p = new PaymentReceived(intent);
                if(receivingBip70Invoice == null)
                    return;
                else
                {
                    if (!receivingBip70Invoice.equalsIgnoreCase(p.addr)) {
                        // different address: might be a previous one, keep the payment request
                        return;
                    }
                }
                onPaymentReceived(p);
            }
        }
    };
    private String qrCodeUri;

    private void onPaymentReceived(PaymentReceived p) {
        if (p.isUnderpayment()) {
            Runnable closingAction = new Runnable() {
                @Override
                public void run() {
                    showCheckMark();
                }
            };
            new PaymentTooLowDialog(this).showUnderpayment(p.bchReceived, p.bchExpected, closingAction);
        } else if (p.isOverpayment()) {
            showCheckMark();
            new PaymentTooHighDialog(this).showOverpayment();
        } else {
            // expected amount
            showCheckMark();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_receive);
        initViews();
        // avoid to mistakenly discard the window
        setFinishOnTouchOutside(false);
        //Register receiver (Listen for incoming tx)
        IntentFilter filter = new IntentFilter(MainActivity.ACTION_INTENT_EXPECTED_PAYMENT_RECEIVED);
        hasAPIKey = this.getIntent().getBooleanExtra(PaymentInputFragment.INVOICE_HAS_API_KEY, false);
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        broadcastManager.registerReceiver(receiver, filter);
        double amountFiat = this.getIntent().getDoubleExtra(PaymentInputFragment.AMOUNT_PAYABLE_FIAT, 0.0);
        AmountUtil f = new AmountUtil(this);
        double amountBch = this.getIntent().getDoubleExtra(PaymentInputFragment.AMOUNT_PAYABLE_BTC, 0.0);
        tvFiatAmount.setText(f.formatFiat(amountFiat));
        tvBtcAmount.setText(f.formatBch(amountBch));
        getBIP70Invoice(PaymentRequestActivity.this, amountBch, tvFiatAmount.getText().toString(), hasAPIKey);
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onDestroy();
    }

    private void initViews() {
        tvFiatAmount = findViewById(R.id.tv_fiat_amount);
        tvBtcAmount = findViewById(R.id.tv_btc_amount);
        ivReceivingQr = findViewById(R.id.qr);
        progressLayout = findViewById(R.id.progressLayout);
        waitingLayout = findViewById(R.id.layout_waiting);
        receivedLayout = findViewById(R.id.layout_complete);
        ivCancel = findViewById(R.id.iv_cancel);
        ivDone = findViewById(R.id.iv_done);
        tvStatus = findViewById(R.id.tv_status);
        ivReceivingQr.setVisibility(View.GONE);
        progressLayout.setVisibility(View.VISIBLE);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickButton(v);
            }
        };
        ivCancel.setOnClickListener(listener);
        ivReceivingQr.setOnClickListener(listener);
        waitingLayout.setVisibility(View.VISIBLE);
        receivedLayout.setVisibility(View.GONE);
    }

    private void clickButton(View v) {
        switch (v.getId()) {
            case R.id.iv_cancel:
                cancelPayment();
                break;
            case R.id.qr:
                copyQrCodeToClipboard();
                break;
        }
    }

    private void cancelPayment() {
        Log.d(TAG, "Canceling payment...");
        onBackPressed();
        ExpectedPayments.getInstance().removePayment(receivingBip70Invoice);
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(MainActivity.ACTION_STOP_LISTENING_FOR_BIP70));
    }

    private void copyQrCodeToClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(qrCodeUri, qrCodeUri);
            clipboard.setPrimaryClip(clip);
            Log.i(TAG, "Copied to clipboard: " + qrCodeUri);
        } catch (Exception e) {
            Log.i(TAG, "Failed to copy to clipboard: " + qrCodeUri);
        }
    }

    private void displayQRCode(String invoiceId) {
        if (!invoiceId.equals("")) {
            qrCodeUri = "bitcoincash:?r=https://pay.bitcoin.com/i/" + invoiceId;
            generateQRCode(qrCodeUri);
            write2NFC(qrCodeUri);
        }
    }

    private void generateQRCode(final String uri) {
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                //Show generating QR message
                ivReceivingQr.setVisibility(View.GONE);
                progressLayout.setVisibility(View.VISIBLE);
            }

            @Override
            protected Bitmap doInBackground(Void... params) {
                Bitmap bitmap = null;
                int qrCodeDimension = 260;
                QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(uri, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension);
                try {
                    bitmap = qrCodeEncoder.encodeAsBitmap();
                } catch (WriterException e) {
                    e.printStackTrace();
                }
                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                progressLayout.setVisibility(View.GONE);
                ivReceivingQr.setVisibility(View.VISIBLE);
                ivReceivingQr.setImageBitmap(bitmap);
            }
        }.execute();
    }

    private void getBIP70Invoice(final Context context, final double amountBch, final String strFiat, final boolean hasAPIKey) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressLayout.setVisibility(View.VISIBLE);
            }

            @Override
            protected String doInBackground(Void... params) {
                try {
                    String json = "";
                    long lAmount = getLongAmount(amountBch);

                    if(hasAPIKey) {
                        String paybitcoincomApiKey = AppUtil.getReceivingAddress(context);
                        json = "{\"apiKey\":\"" + paybitcoincomApiKey + "\",\"amount\":" + lAmount + ", \"webhook\":\"http://somedomain.com/webhook\", \"fiat\":\"USD\", \"memo\":\"Your message here\"}";
                    } else {
                        String tempAddress = null;
                        AppUtil util = AppUtil.getInstance(PaymentRequestActivity.this);
                        if (util.isValidXPub()) {
                            try {
                                tempAddress = util.getWallet().generateAddressFromXPub();
                                Log.i(TAG, "BCH-address(xPub) to receive: " + tempAddress);
                            } catch (Exception e) {
                                tempAddress = null;
                                e.printStackTrace();
                            }
                        } else {
                            tempAddress = AppUtil.getReceivingAddress(context);
                        }
                        if (StringUtils.isEmpty(tempAddress)) {
                            ToastCustom.makeText(PaymentRequestActivity.this, getText(R.string.unable_to_generate_address), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                            return null;
                        }

                        json = "{\"address\":\"" + tempAddress + "\",\"amount\":" + lAmount + ", \"webhook\":\"http://somedomain.com/webhook\", \"fiat\":\"USD\", \"memo\":\"Your message here\"}";
                    }

                    URL url = new URL("https://pay.bitcoin.com/create_invoice");
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
                    String res = rd.readLine();
                    JSONObject jsonObject = new JSONObject(res);
                    receivingBip70Invoice = jsonObject.getString("paymentId");
                    displayQRCode(receivingBip70Invoice);
                    Intent listenForBip70 = new Intent(MainActivity.ACTION_START_LISTENING_FOR_BIP70);
                    listenForBip70.putExtra("invoice_id", receivingBip70Invoice);
                    ExpectedPayments.getInstance().addExpectedPayment(receivingBip70Invoice, lAmount, strFiat);
                    LocalBroadcastManager.getInstance(PaymentRequestActivity.this).sendBroadcast(listenForBip70);
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
                return receivingBip70Invoice;
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                progressLayout.setVisibility(View.GONE);
            }
        }.execute();
    }

    private long getLongAmount(double amountPayable) {
        double value = Math.round(amountPayable * 100000000.0);
        return (Double.valueOf(value)).longValue();
    }

    private void showCheckMark() {
        setFinishOnTouchOutside(true); // now allow easy dismissal
        waitingLayout.setVisibility(View.GONE);
        receivedLayout.setVisibility(View.VISIBLE);
        AppUtil.setStatusBarColor(PaymentRequestActivity.this, R.color.bitcoindotcom_green);
        ivDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppUtil.setStatusBarColor(PaymentRequestActivity.this, R.color.gray);
                Intent intent = new Intent();
                PaymentRequestActivity.this.setResult(RESULT_OK, intent);
                PaymentRequestActivity.this.finish();
            }
        });
        setResult(RESULT_OK);
    }

    private void write2NFC(final String uri) {
        try {
            NfcAdapter nfc = NfcAdapter.getDefaultAdapter(PaymentRequestActivity.this);
            if (nfc != null && nfc.isNdefPushEnabled()) {
                nfc.setNdefPushMessageCallback(new NfcAdapter.CreateNdefMessageCallback() {
                    @Override
                    public NdefMessage createNdefMessage(NfcEvent event) {
                        NdefRecord uriRecord = NdefRecord.createUri(uri);
                        return new NdefMessage(new NdefRecord[]{uriRecord});
                    }
                }, PaymentRequestActivity.this);
            }
        } catch (Exception e) {
            // usually happens when activity is being closed while background task is executing
            Log.e(TAG, "Failed write2NFC: " + uri, e);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(PaymentInputFragment.ACTION_INTENT_RESET_AMOUNT));
    }
}
