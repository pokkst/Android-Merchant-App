package info.blockchain.merchant;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import info.blockchain.merchant.service.WebSocketHandler;
import info.blockchain.merchant.service.WebSocketListener;
import info.blockchain.merchant.tabsswipe.TabsPagerAdapter;
import info.blockchain.merchant.util.AppUtil;
import info.blockchain.merchant.util.PrefsUtil;

public class MainActivity extends AppCompatActivity implements NfcAdapter.CreateNdefMessageCallback, NfcAdapter.OnNdefPushCompleteCallback, WebSocketListener {

    private static int SETTINGS_ACTIVITY 	= 1;
    private static int PIN_ACTIVITY 		= 2;
    private static int RESET_PIN_ACTIVITY 	= 3;
    private static int ABOUT_ACTIVITY 	= 4;

    private WebSocketHandler webSocketHandler = null;

    public static final String ACTION_INTENT_SUBSCRIBE_TO_ADDRESS = "info.blockchain.merchant.MainActivity.SUBSCRIBE_TO_ADDRESS";
    public static final String ACTION_INTENT_INCOMING_TX = "info.blockchain.merchant.MainActivity.ACTION_INTENT_INCOMING_TX";
    public static final String ACTION_INTENT_RECONNECT = "info.blockchain.merchant.MainActivity.ACTION_INTENT_RECONNECT";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_main);
	    
        Toolbar toolbar = (Toolbar)this.findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.app_name));
        setSupportActionBar(toolbar);

        initTableLayout();

        // no PIN ?, then create one
		String pin = PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.MERCHANT_KEY_PIN, "");
        if(pin.equals("")) {
        	doPIN();
        }
		else	{

			//
			// test for v1
			//
			if(AppUtil.getInstance(MainActivity.this).isLegacy())	{
				String strCurrency = PrefsUtil.getInstance(MainActivity.this).getValue("currency", "");
				if(strCurrency.equals("ZZZ"))	{
					PrefsUtil.getInstance(MainActivity.this).setValue(PrefsUtil.MERCHANT_KEY_CURRENCY, "USD");
				}
				PrefsUtil.getInstance(MainActivity.this).removeValue("ocurrency");
			}

		}

        //Start websockets
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_INTENT_SUBSCRIBE_TO_ADDRESS);
        filter.addAction(ACTION_INTENT_RECONNECT);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, filter);

        webSocketHandler = new WebSocketHandler();
        webSocketHandler.addListener(this);
        webSocketHandler.start();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
	}

	@Override
	public void onNdefPushComplete(NfcEvent event) {

		if(Build.VERSION.SDK_INT < 16){
			return;
		}

        final String eventString = "onNdefPushComplete\n" + event.toString();
        runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), eventString, Toast.LENGTH_SHORT).show();
			}
		});

	}

	@Override
	public NdefMessage createNdefMessage(NfcEvent event) {

		if(Build.VERSION.SDK_INT < 16){
			return null;
		}

		NdefRecord rtdUriRecord = NdefRecord.createUri("market://details?id=info.blockchain.merchant");
		NdefMessage ndefMessageout = new NdefMessage(rtdUriRecord);
		return ndefMessageout;
	}

	@Override
    protected void onDestroy() {
        //Stop websockets
        webSocketHandler.stop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(receiver);

        super.onDestroy();
    }

    private void initTableLayout(){

        String[] tabs = new String[]{getResources().getString(R.string.tab_payment),getResources().getString(R.string.tab_transactions)};

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        PagerAdapter mAdapter = new TabsPagerAdapter(getSupportFragmentManager(), tabs);
		viewPager.setAdapter(mAdapter);

        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setTabTextColors(getResources().getColor(R.color.white_50), getResources().getColor(R.color.white));

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setTabsFromPagerAdapter(mAdapter);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    	case R.id.action_settings:
	    		doSettings(false);
	    		return true;
	    	case R.id.action_newpin:
				String pin = PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.MERCHANT_KEY_PIN, "");
	            if(pin.equals("")) {
		    		doPIN();
	            }
	            else {
		    		resetPIN();
	            }
	    		return true;
	    	case R.id.action_about:
	    		doAbout();
	    		return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if(requestCode == SETTINGS_ACTIVITY && resultCode == RESULT_OK) {
			;
		}
		else if(requestCode == PIN_ACTIVITY && resultCode == RESULT_OK) {
			String strOtherCurrency = PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.MERCHANT_KEY_PIN, "");
    		Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
    		intent.putExtra("ocurrency", strOtherCurrency);
    		startActivityForResult(intent, SETTINGS_ACTIVITY);
		}
		else if(requestCode == RESET_PIN_ACTIVITY && resultCode == RESULT_OK) {
			doPIN();
		}
		else {
			;
		}
		
	}

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean ret = super.dispatchTouchEvent(event);

        View view = this.getCurrentFocus();

        if (view instanceof EditText) {
            View w = this.getCurrentFocus();
            int scrcoords[] = new int[2];
            w.getLocationOnScreen(scrcoords);
            float x = event.getRawX() + w.getLeft() - scrcoords[0];
            float y = event.getRawY() + w.getTop() - scrcoords[1];
            
            if (event.getAction() == MotionEvent.ACTION_UP && (x < w.getLeft() || x >= w.getRight() || y < w.getTop() || y > w.getBottom()) ) { 
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getWindow().getCurrentFocus().getWindowToken(), 0);
            }
        }

        return ret;
    }

    private void doSettings(final boolean create)	{
    	if(create)	{
			String strOtherCurrency = PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.MERCHANT_KEY_PIN, "");
    		Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
    		intent.putExtra("ocurrency", strOtherCurrency);
    		startActivityForResult(intent, SETTINGS_ACTIVITY);
    	}
    	else	{
    		enterPIN();
    	}
    }

    private void doPIN()	{
		Intent intent = new Intent(MainActivity.this, PinActivity.class);
		intent.putExtra("create", true);
		startActivityForResult(intent, PIN_ACTIVITY);
    }

    private void enterPIN()	{
		Intent intent = new Intent(MainActivity.this, PinActivity.class);
		intent.putExtra("create", false);
		startActivityForResult(intent, PIN_ACTIVITY);
    }

    private void resetPIN()	{
		Intent intent = new Intent(MainActivity.this, PinActivity.class);
		intent.putExtra("create", false);
		startActivityForResult(intent, RESET_PIN_ACTIVITY);
    }

    private void doAbout()	{
    	Intent intent = new Intent(MainActivity.this, AboutActivity.class);
		startActivityForResult(intent, ABOUT_ACTIVITY);
    }

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {

            if (ACTION_INTENT_SUBSCRIBE_TO_ADDRESS.equals(intent.getAction())) {
                webSocketHandler.subscribeToAddress(intent.getStringExtra("address"));
            }

            //Connection re-established
            if (ACTION_INTENT_RECONNECT.equals(intent.getAction())) {
                if(webSocketHandler != null && !webSocketHandler.isConnected()){
                    webSocketHandler.start();
				}
            }
        }
    };

    @Override
    public void onIncomingPayment(long paymentAmount) {

        //New incoming payment - broadcast message
        Intent intent = new Intent(MainActivity.ACTION_INTENT_INCOMING_TX);
        intent.putExtra("payment_amount",paymentAmount);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
