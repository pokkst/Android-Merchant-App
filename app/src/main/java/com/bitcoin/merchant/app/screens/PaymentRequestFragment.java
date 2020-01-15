package com.bitcoin.merchant.app.screens;

import android.annotation.SuppressLint;
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
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bitcoin.merchant.app.MainActivity;
import com.bitcoin.merchant.app.R;
import com.bitcoin.merchant.app.application.CashRegisterApplication;
import com.bitcoin.merchant.app.model.PaymentReceived;
import com.bitcoin.merchant.app.network.ExpectedPayments;
import com.bitcoin.merchant.app.network.paybitcoincom.Bip70InvoiceJson;
import com.bitcoin.merchant.app.network.paybitcoincom.RequestHelper;
import com.bitcoin.merchant.app.screens.dialogs.PaymentTooHighDialog;
import com.bitcoin.merchant.app.screens.dialogs.PaymentTooLowDialog;
import com.bitcoin.merchant.app.screens.features.ToolbarAwareFragment;
import com.bitcoin.merchant.app.util.AmountUtil;
import com.bitcoin.merchant.app.util.AppUtil;
import com.bitcoin.merchant.app.util.PrefsUtil;
import com.bitcoin.merchant.app.util.ToastCustom;
import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.bitcoin.merchant.app.MainActivity.TAG;

public class PaymentRequestFragment extends ToolbarAwareFragment {
    private LinearLayout waitingLayout;
    private LinearLayout receivedLayout;
    private TextView tvFiatAmount;
    private TextView tvBtcAmount;
    private TextView tvExpiryTimer;
    private ImageView ivReceivingQr;
    private LinearLayout progressLayout;
    private Button ivCancel;
    private Button ivDone;
    private String receivingBip70Invoice;
    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            if (MainActivity.ACTION_INTENT_EXPECTED_PAYMENT_RECEIVED.equals(intent.getAction())) {
                PaymentReceived p = new PaymentReceived(intent);
                if (receivingBip70Invoice == null)
                    return;
                else {
                    if (!receivingBip70Invoice.equalsIgnoreCase(p.addr)) {
                        // different address: might be a previous one, keep the payment request
                        return;
                    }
                }
                onPaymentReceived(p);
            }
        }
    };
    private boolean hasAPIKey = false;
    private String qrCodeUri;

    private void onPaymentReceived(PaymentReceived p) {
        if (p.isUnderpayment()) {
            Runnable closingAction = new Runnable() {
                @Override
                public void run() {
                    showCheckMark();
                }
            };
            new PaymentTooLowDialog(activity).showUnderpayment(p.bchReceived, p.bchExpected, closingAction);
        } else if (p.isOverpayment()) {
            showCheckMark();
            new PaymentTooHighDialog(activity).showOverpayment();
        } else {
            // expected amount
            showCheckMark();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_request_payment, container, false);
        initViews(v);
        setToolbarVisible(false);
        //Register receiver (Listen for incoming tx)
        IntentFilter filter = new IntentFilter();
        filter.addAction(MainActivity.ACTION_INTENT_EXPECTED_PAYMENT_RECEIVED);
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(activity.getApplicationContext());
        broadcastManager.registerReceiver(receiver, filter);
        Bundle args = getArguments();
        hasAPIKey = args.getBoolean(PaymentInputFragment.INVOICE_HAS_API_KEY, false);
        double amountFiat = args.getDouble(PaymentInputFragment.AMOUNT_PAYABLE_FIAT, 0.0);
        AmountUtil f = new AmountUtil(activity);
        double amountBch = args.getDouble(PaymentInputFragment.AMOUNT_PAYABLE_BTC, 0.0);
        tvFiatAmount.setText(f.formatFiat(amountFiat));
        tvBtcAmount.setText(f.formatBch(amountBch));
        getBIP70Invoice(getApp(), amountBch, tvFiatAmount.getText().toString(), hasAPIKey);
        // Attempt to reconnect in case we were disconnected from Internet
        broadcastManager.sendBroadcast(new Intent(MainActivity.ACTION_INTENT_RECONNECT));
        // Query mempool, in case the previous TX was not received by the socket listeners
        // It will be unnecessary and can be removed after the switch to BIP-70 in the next big release.
        LocalBroadcastManager.getInstance(activity).sendBroadcast(new Intent(MainActivity.ACTION_QUERY_MISSING_TX_IN_MEMPOOL));
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(receiver);
    }

    private void initViews(View v) {
        tvFiatAmount = v.findViewById(R.id.tv_fiat_amount);
        tvBtcAmount = v.findViewById(R.id.tv_btc_amount);
        tvExpiryTimer = v.findViewById(R.id.bip70_timer_tv);
        ivReceivingQr = v.findViewById(R.id.qr);
        progressLayout = v.findViewById(R.id.progressLayout);
        waitingLayout = v.findViewById(R.id.layout_waiting);
        receivedLayout = v.findViewById(R.id.layout_complete);
        ivCancel = v.findViewById(R.id.iv_cancel);
        ivDone = v.findViewById(R.id.iv_done);
        showGeneratingQrCodeProgress(true);
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

    private void showGeneratingQrCodeProgress(boolean enabled) {
        this.progressLayout.setVisibility(enabled ? View.VISIBLE : View.GONE);
        this.ivReceivingQr.setVisibility(enabled ? View.GONE : View.VISIBLE);
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
        activity.onBackPressed();
        ExpectedPayments.getInstance().removePayment(receivingBip70Invoice);
        LocalBroadcastManager.getInstance(activity).sendBroadcast(new Intent(MainActivity.ACTION_QUERY_MISSING_TX_THEN_ALL_UTXO));
        LocalBroadcastManager.getInstance(activity).sendBroadcast(new Intent(MainActivity.ACTION_STOP_LISTENING_FOR_BIP70));
    }

    private void copyQrCodeToClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText(qrCodeUri, qrCodeUri);
                clipboard.setPrimaryClip(clip);
            }
            Log.i(TAG, "Copied to clipboard: " + qrCodeUri);
        } catch (Exception e) {
            Log.i(TAG, "Failed to copy to clipboard: " + qrCodeUri);
        }
    }

    private void displayQRCode(String invoiceId, final String startDate, final String expirationDate) {
        if (!invoiceId.equals("")) {
            qrCodeUri = "bitcoincash:?r=https://pay.bitcoin.com/i/" + invoiceId;
            generateQRCode(qrCodeUri);

            Handler mainThread = new Handler(activity.getMainLooper());

            Runnable myRunnable = new Runnable() {
                @Override
                public void run() {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'");
                    Date expirationTime = null;
                    Date creationTime = null;
                    try {
                        expirationTime = format.parse(expirationDate);
                        creationTime = format.parse(startDate);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    long timeLimit = expirationTime.getTime() - creationTime.getTime();
                    CountDownTimer mCountDownTimer = new CountDownTimer(timeLimit, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            long secondsLeft = millisUntilFinished / 1000L;
                            tvExpiryTimer.setText(String.format(getResources().getConfiguration().locale, "%02d:%02d", secondsLeft / 60, secondsLeft % 60));

                            if (secondsLeft <= 0) {
                                cancelPayment();
                            }
                        }

                        @Override
                        public void onFinish() {

                        }
                    }.start();
                }
            };
            mainThread.post(myRunnable);

            write2NFC(qrCodeUri);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void generateQRCode(final String uri) {
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                showGeneratingQrCodeProgress(true);
            }

            @Override
            protected Bitmap doInBackground(Void... params) {
                Bitmap bitmap = null;
                int qrCodeDimension = 260;
                QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(uri, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension);
                try {
                    bitmap = qrCodeEncoder.encodeAsBitmap();
                } catch (WriterException e) {
                    Log.e(TAG, "", e);
                }
                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                showGeneratingQrCodeProgress(false);
                ivReceivingQr.setImageBitmap(bitmap);
            }
        }.execute();
    }

    @SuppressLint("StaticFieldLeak")
    private void getBIP70Invoice(final CashRegisterApplication app, final double amountBch, final String strFiat, final boolean hasAPIKey) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressLayout.setVisibility(View.VISIBLE);
            }

            @Override
            protected String doInBackground(Void... params) {
                try {
                    Bip70InvoiceJson invoice = new Bip70InvoiceJson();
                    invoice.fiat = "USD";
                    invoice.memo = "Payment to " + PrefsUtil.getInstance(getApp()).getValue(PrefsUtil.MERCHANT_KEY_MERCHANT_NAME, "Bitcoin.com Pay merchant");
                    long lAmount = getLongAmount(amountBch);
                    invoice.amount = lAmount;
                    if (hasAPIKey) {
                        invoice.apiKey = AppUtil.getReceivingAddress(app);
                    } else {
                        String tempAddress;
                        if (AppUtil.isValidXPub(app)) {
                            try {
                                tempAddress = app.getWallet().generateAddressFromXPub();
                                Log.i(TAG, "BCH-address(xPub) to receive: " + tempAddress);
                            } catch (Exception e) {
                                tempAddress = null;
                                e.printStackTrace();
                            }
                        } else {
                            tempAddress = AppUtil.getReceivingAddress(app);
                        }
                        if (StringUtils.isEmpty(tempAddress)) {
                            ToastCustom.makeText(app, getText(R.string.unable_to_generate_address), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                            return null;
                        }

                        invoice.address = tempAddress;
                    }

                    Gson gsonHelper = new Gson();
                    Log.i(TAG, gsonHelper.toJson(invoice));
                    //TODO move new RequestHelper() to an object in AppUtil (or something) so we don't need to make a new one each time
                    String response = new RequestHelper().createInvoice(gsonHelper.toJson(invoice));
                    JSONObject jsonObject = new JSONObject(response);
                    String startDate = jsonObject.getString("time");
                    String expirationDate = jsonObject.getString("expires");
                    receivingBip70Invoice = jsonObject.getString("paymentId");
                    displayQRCode(receivingBip70Invoice, startDate, expirationDate);
                    Intent listenForBip70 = new Intent(MainActivity.ACTION_START_LISTENING_FOR_BIP70);
                    listenForBip70.putExtra("invoice_id", receivingBip70Invoice);
                    ExpectedPayments.getInstance().addExpectedPayment(receivingBip70Invoice, lAmount, strFiat);
                    LocalBroadcastManager.getInstance(app).sendBroadcast(listenForBip70);
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
        waitingLayout.setVisibility(View.GONE);
        receivedLayout.setVisibility(View.VISIBLE);
        AppUtil.setStatusBarColor(activity, R.color.bitcoindotcom_green);
        ivDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppUtil.setStatusBarColor(activity, R.color.gray);
                activity.onBackPressed();
            }
        });
    }

    private void write2NFC(final String uri) {
        try {
            NfcAdapter nfc = NfcAdapter.getDefaultAdapter(activity);
            if (nfc != null && nfc.isNdefPushEnabled()) {
                nfc.setNdefPushMessageCallback(new NfcAdapter.CreateNdefMessageCallback() {
                    @Override
                    public NdefMessage createNdefMessage(NfcEvent event) {
                        NdefRecord uriRecord = NdefRecord.createUri(uri);
                        return new NdefMessage(new NdefRecord[]{uriRecord});
                    }
                }, activity);
            }
        } catch (Exception e) {
            // usually happens when activity is being closed while background task is executing
            Log.e(TAG, "Failed write2NFC: " + uri, e);
        }
    }

    @Override
    public boolean isBackAllowed() {
        LocalBroadcastManager.getInstance(activity).sendBroadcast(new Intent(PaymentInputFragment.ACTION_INTENT_RESET_AMOUNT));
        return true;
    }
}
