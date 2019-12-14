package com.bitcoin.merchant.app.screens.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputFilter;
import android.widget.EditText;
import android.widget.TextView;

import com.bitcoin.merchant.app.R;
import com.bitcoin.merchant.app.util.PrefsUtil;

public class MerchantNameEditorDialog {
    private final Activity ctx;

    public MerchantNameEditorDialog(Activity ctx) {
        this.ctx = ctx;
    }

    public boolean show(final TextView namePref) {
        final EditText etName = new EditText(ctx);
        etName.setSingleLine(true);
        int maxLength = 70;
        InputFilter[] fArray = new InputFilter[1];
        fArray[0] = new InputFilter.LengthFilter(maxLength);
        etName.setFilters(fArray);
        etName.setText(PrefsUtil.getInstance(ctx).getValue(PrefsUtil.MERCHANT_KEY_MERCHANT_NAME, ""));
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx)
                .setTitle(R.string.settings_merchant_name)
                .setView(etName)
                .setCancelable(false)
                .setPositiveButton(R.string.prompt_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String name = etName.getText().toString();
                        if (name.length() > 0) {
                            PrefsUtil.getInstance(ctx).setValue(PrefsUtil.MERCHANT_KEY_MERCHANT_NAME, name);
                            namePref.setText(name);
                        }
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.prompt_ko, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });
        if (! ctx.isFinishing()) {
            builder.show();
        }
        return true;
    }
}
