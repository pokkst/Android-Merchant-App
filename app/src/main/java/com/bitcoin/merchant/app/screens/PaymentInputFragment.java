package com.bitcoin.merchant.app.screens;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bitcoin.merchant.app.MainActivity;
import com.bitcoin.merchant.app.R;
import com.bitcoin.merchant.app.currency.CountryCurrency;
import com.bitcoin.merchant.app.currency.CurrencyExchange;
import com.bitcoin.merchant.app.util.AppUtil;
import com.bitcoin.merchant.app.util.MonetaryUtil;
import com.bitcoin.merchant.app.util.SnackCustom;
import com.bitcoin.merchant.app.util.ToastCustom;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Currency;
import java.util.Locale;

public class PaymentInputFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "PaymentInputFragment";
    public static final String DEFAULT_CURRENCY_BCH = "BCH";
    public static final int RECEIVE_RESULT = 1122;
    public static final String ACTION_INTENT_RESET_AMOUNT = "RESET_AMOUNT";
    private static final double bitcoinLimit = 21000000.0;
    public static String AMOUNT_PAYABLE_FIAT = "AMOUNT_PAYABLE_FIAT";
    public static String AMOUNT_PAYABLE_BTC = "AMOUNT_PAYABLE_BTC";
    public static String INVOICE_HAS_API_KEY = "INVOICE_HAS_API_KEY";
    public double amountPayableFiat = 0.0;
    public double amountPayableBch = 0.0;
    private int allowedDecimalPlaces = 2;
    private View rootView = null;
    private TextView tvCurrencySymbol = null;
    private TextView tvAmount = null;
    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            if (ACTION_INTENT_RESET_AMOUNT.equals(intent.getAction())) {
                if (tvAmount != null) {
                    tvAmount.setText("0");
                }
            }
        }
    };
    private Button buttonDecimal = null;
    private TextView tvBch = null;
    private NumberFormat nf = null;
    private DecimalFormat df = null;
    private DecimalFormatSymbols dfs = null;
    private String strDecimal = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_payment, container, false);
        nf = NumberFormat.getInstance(Locale.getDefault());
        dfs = new DecimalFormatSymbols();
        df = new DecimalFormat("#.########", dfs);
        tvCurrencySymbol = rootView.findViewById(R.id.tv_currency_symbol);
        tvAmount = rootView.findViewById(R.id.tv_fiat_amount);
        Button ivCharge = rootView.findViewById(R.id.iv_charge);
        ivCharge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chargeClicked();
            }
        });
        tvBch = rootView.findViewById(R.id.tv_bch);
        initalizeButtons();
        updateAmounts();
        initDecimalButton();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_INTENT_RESET_AMOUNT);
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(receiver, filter);
        tvCurrencySymbol.setText(getCurrencySymbol());
        return rootView;
    }

    @Override
    public void onDestroyView() {
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(receiver);
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAmounts();
        initDecimalButton();
        tvCurrencySymbol.setText(getCurrencySymbol());
    }

    private void initDecimalButton() {
        strDecimal = Character.toString(MonetaryUtil.getInstance().getDecimalFormatSymbols().getDecimalSeparator());
        try {
            Currency instance = Currency.getInstance(AppUtil.getCurrency(getContext()));
            allowedDecimalPlaces = instance.getDefaultFractionDigits();
            boolean enabled = allowedDecimalPlaces > 0;
            View buttonView = rootView.findViewById(R.id.buttonDecimal);
            buttonView.setEnabled(enabled);
            buttonDecimal.setText(enabled ? strDecimal : "");
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
    }

    private String getCurrency() {
        return AppUtil.getCurrency(getActivity());
    }

    private String getCurrencySymbol() {
        String currency = AppUtil.getCurrency(getContext());
        String country = AppUtil.getCountry(getContext());
        String locale = AppUtil.getLocale(getContext());
        CountryCurrency cc = CurrencyExchange.getInstance(getContext()).getCountryCurrency(currency, country, locale);
        if (cc != null) {
            if (cc.currencyRate.symbol != null) {
                return cc.currencyRate.symbol;
            } else {
                /* The currency symbols displayed on the payment request screen are not able to be grabbed from the Currency class, or from CountryCurrency.
                   A prime example of this is country AOA. NumberFormat gives Kz for its symbol, yet you can't grab it from any object.
                   Here we format a fake balance (0) then remove all digits, periods, commas, etc. to get the symbol.

                   It's kinda hacky but it works.
                 */
                NumberFormat formatter = NumberFormat.getCurrencyInstance(cc.countryLocales.getLocale());
                Currency instance = Currency.getInstance(currency);
                formatter.setCurrency(instance);
                formatter.setMaximumFractionDigits(instance.getDefaultFractionDigits());
                String formatted = formatter.format(0);
                return formatted.replaceAll("[\\d., ]", "");
            }
        }
        return "";
    }


    private void initalizeButtons() {
        rootView.findViewById(R.id.button0).setOnClickListener(this);
        rootView.findViewById(R.id.button1).setOnClickListener(this);
        rootView.findViewById(R.id.button2).setOnClickListener(this);
        rootView.findViewById(R.id.button3).setOnClickListener(this);
        rootView.findViewById(R.id.button4).setOnClickListener(this);
        rootView.findViewById(R.id.button5).setOnClickListener(this);
        rootView.findViewById(R.id.button6).setOnClickListener(this);
        rootView.findViewById(R.id.button7).setOnClickListener(this);
        rootView.findViewById(R.id.button8).setOnClickListener(this);
        rootView.findViewById(R.id.button9).setOnClickListener(this);
        buttonDecimal = rootView.findViewById(R.id.buttonDecimal);
        buttonDecimal.setOnClickListener(this);
        Button buttonDeleteBack = rootView.findViewById(R.id.buttonDeleteBack);
        buttonDeleteBack.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Button viewButton = (Button) v;
        if (v.getId() != R.id.buttonDeleteBack && v.getId() != R.id.buttonDecimal)
            padClicked(viewButton.getText().toString());
        else if(v.getId() == R.id.buttonDeleteBack)
            padClicked(null);
        else if(v.getId() == R.id.buttonDecimal)
            padClicked(strDecimal);

        updateAmounts();
    }

    private boolean validateAmount() {
        try {
            Double value = Double.valueOf(nf.parse(tvAmount.getText().toString()).doubleValue());
            return !value.isInfinite() && !value.isNaN() && value > 0.0;
        } catch (Exception e) {
            return false;
        }
    }

    public void chargeClicked() {
        String paymentAddress = AppUtil.getReceivingAddress(getContext());

        if (!AppUtil.getInstance(getActivity()).hasValidReceiver()) {
            if(!paymentAddress.matches("^[a-z]*$") && !(paymentAddress.length() == 40)) {
                SnackCustom.make(getActivity(), getView(), getActivity().getText(R.string.no_valid_receiver), getActivity().getResources().getString(R.string.prompt_ok), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getActivity(), SettingsActivity.class);
                        startActivityForResult(intent, MainActivity.SETTINGS_ACTIVITY);
                    }
                });

                return;
            }
        }
        if (validateAmount()) {
            updateAmounts();
            boolean hasAPIKey = false;
            /*
            Check to see if they have a pay.bitcoin.com API key set
             */
            if(paymentAddress.matches("^[a-z]*$") && paymentAddress.length() == 40)
                hasAPIKey = true;

            Intent intent = new Intent(getActivity(), PaymentRequestActivity.class);
            intent.putExtra(AMOUNT_PAYABLE_FIAT, amountPayableFiat);
            intent.putExtra(AMOUNT_PAYABLE_BTC, amountPayableBch);
            intent.putExtra(INVOICE_HAS_API_KEY, hasAPIKey);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivityForResult(intent, RECEIVE_RESULT);
        } else {
            SnackCustom.make(getActivity(), getView(), getActivity().getText(R.string.invalid_amount), getActivity().getResources().getString(R.string.prompt_ok), null);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //reset amount after receive
        if (requestCode == RECEIVE_RESULT && resultCode == Activity.RESULT_OK) {
            tvAmount.setText("0");
        }
    }

    public void padClicked(String pad) {
        // Back clicked
        if (pad == null) {
            String e1 = tvAmount.getText().toString();
            if (e1.length() > 1) {
                tvAmount.setText(e1.substring(0, e1.length() - 1));
            } else {
                tvAmount.setText("0");
            }
            return;
        }
        String amountText = tvAmount.getText().toString();
        if (amountText.length() == 1 && amountText.charAt(0) == '0' && !pad.equals(strDecimal)) {
            tvAmount.setText(pad);
            return;
        }
        //initial input
        double amount = 0.0;
        try {
            amount = nf.parse(amountText).doubleValue();
        } catch (ParseException pe) {
            amount = 0.0;
        }
        if (amount == 0.0 && pad.equals(strDecimal)) {
            tvAmount.setText("0" + strDecimal);
            return;
        }
        //Don't allow multiple decimal separators
        if (amountText.contains(strDecimal) && pad.equals(strDecimal)) {
            return;
        }
        //Don't allow multiple leading 0's
        if (amountText.equals("0") && pad.equals("0")) {
            return;
        }
        //Get decimal places
        if (amountText.contains(strDecimal)) {
            int decimalPlaces = 0;
            amountText += pad;
            String[] result = amountText.split(strDecimal);
            if (result.length >= 2)
                decimalPlaces = result[1].length();
            if (decimalPlaces > allowedDecimalPlaces) return;
        }
        // Append tapped #
        tvAmount.append(pad);
        //Check that we don't exceed bitcoin limit
        checkBitcoinLimit();
    }

    private void checkBitcoinLimit() {
        double currentValue = 0.0;
        try {
            currentValue = nf.parse(tvAmount.getText().toString()).doubleValue();
        } catch (ParseException e) {
            Log.e(TAG, "", e);
        }
        double bchValue = 0.0;
        try {
            bchValue = toBch(currentValue);
        } catch (ParseException e) {
            Log.e(TAG, "", e);
        }
        if (bchValue > bitcoinLimit) {
            Double currencyPrice = CurrencyExchange.getInstance(getActivity()).getCurrencyPrice(getCurrency());
            tvAmount.setText(MonetaryUtil.getInstance().getFiatDecimalFormat().format(bitcoinLimit * currencyPrice));
            ToastCustom.makeText(getActivity(), getResources().getString(R.string.invalid_amount), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }
    }

    private void updateAmounts() {
        if (tvAmount == null) return;
        try {
            double amount = nf.parse(tvAmount.getText().toString()).doubleValue();
            double bch = toBch(amount);
            amountPayableFiat = amount;
            amountPayableBch = bch;
        } catch (ParseException e) {
            amountPayableFiat = 0.0;
            amountPayableBch = 0.0;
            Log.e(TAG, "", e);
        }
        if (amountPayableFiat == 0.0) {
            tvBch.setText("Enter an amount");
        } else {
            tvBch.setText(df.format(amountPayableBch) + " BCH");
        }
    }

    private double toBch(double amount) throws ParseException {
        Double currencyPrice = CurrencyExchange.getInstance(getActivity()).getCurrencyPrice(getCurrency());
        MonetaryUtil util = MonetaryUtil.getInstance();
        return (currencyPrice == 0.0d) ? 0.0d : nf.parse(util.getBchDecimalFormat().format(amount / currencyPrice)).doubleValue();
    }
}
