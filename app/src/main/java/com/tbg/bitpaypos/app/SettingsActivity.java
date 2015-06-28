package com.tbg.bitpaypos.app;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import com.haibison.android.lockpattern.LockPatternActivity;
import com.haibison.android.lockpattern.util.Settings;
import com.haibison.android.lockpattern.util.SimpleWeakEncryption;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Set;


public class SettingsActivity extends ActionBarActivity {

    private Switch btToggle;
    private Button pickDevice;
    private TextView status;
    private BluetoothAdapter btAdapter;
    private static String DEBUG_TAG = "Debug:";
    private static final int ENABLE_BLUETOOTH = 1;
    private BluetoothDevice remoteDevice = null;
    private static final int CONNECT = 2;
    private BluetoothDevice btDevice = null;
    private ClientConnectThread clientConnectThread;
    private static final int PIN_ALERT = 10;
    private static final int SET_PIN = 9;
    private HashMap<String, BluetoothDevice> discoveredDevices = new HashMap<String, BluetoothDevice>();
    private SettingsActivity thisClass = this;
    private boolean pinEntered = false;
    private boolean usingPin = false;
    private boolean disablePin = false;
    private boolean creatingNewPin = false;

    // flag used to create a lock pattern
    private static final int REQ_CREATE_PATTERN = 3;
    private static final int REQ_ENTER_PATTERN = 4;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getIntent().hasExtra("apifail?")) {
            Context context = getApplicationContext();
            CharSequence text = "      Api key is incorrect, please input again, \n  hint: try copy and pasting to ensure accuracy";
            Toast toast = Toast.makeText(context, text, 5);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }

        Settings.Security.setAutoSavePattern(this, true);

        Settings.Security.setEncrypterClass(this, LPEncrypter.class);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        btToggle = (Switch) findViewById(R.id.bt_toggle);
        pickDevice = (Button) findViewById(R.id.bt_pick_device);
        status = (TextView) findViewById(R.id.status);


        // so make pin appear originally, if success set a var true,
        // else allow user to go back and / change pin w/ old pin if they want but changing apikeys doesn't work
        // before doing this check if a pin is set at all



        SharedPreferences apiKeys = this.getSharedPreferences("pin", MODE_PRIVATE);
        SharedPreferences.Editor prefsEditorApiKeys = apiKeys.edit();
        usingPin = apiKeys.getBoolean("usingPin", false);


        if(usingPin) {
            // enter existing pin
            Intent intent = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null,
                    this, LockPatternActivity.class);
           // intent.putExtra(LockPatternActivity.EXTRA_PATTERN, savedPattern);
            startActivityForResult(intent, REQ_ENTER_PATTERN);
        }

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter == null) {
            // no bluetooth available on device
            show_message("Couldn't detect bluetooth on your device");
            disable_UI();
        } else {
            // bluetooth is available on the device
            Log.d("Debug:", "Bluetooth available on device");

            // change listener for toggle switch
            btToggle.setOnCheckedChangeListener(btToggleListener);

            // check current state
            int currentState = btAdapter.getState();
            setUIForBTState(currentState);
        }
    }

    private void setUIForBTState(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_ON:
                btToggle.setChecked(true);
                btToggle.setEnabled(true);

                //editMsg.setEnabled(true);
                //send.setEnabled(true);
                pickDevice.setEnabled(true);
                Log.v(DEBUG_TAG, "BT state now on");
                break;
            case BluetoothAdapter.STATE_OFF:
                btToggle.setChecked(false);
                btToggle.setEnabled(true);
                pickDevice.setEnabled(false);
               // send.setEnabled(false);
               // editMsg.setEnabled(false);
                Log.v(DEBUG_TAG, "BT state now off");
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                Log.v(DEBUG_TAG, "BT state turning off");
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                Log.v(DEBUG_TAG, "BT state turning on");
                break;
        }
    }

    /**
     * If bluetooth toggle is switched to on then enable bluetooth
     */
    public CompoundButton.OnCheckedChangeListener btToggleListener = new CompoundButton.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Log.v(DEBUG_TAG, "doToggleBT() called");
            if ( isChecked == false ) {
                Log.v(DEBUG_TAG, "Disabling bluetooth");

                pickDevice.setEnabled(false);

                if ( btAdapter.isDiscovering() ){
                    btAdapter.cancelDiscovery();
                }

                if (!btAdapter.disable()) {
                    Log.v(DEBUG_TAG, "Disable adapter failed");
                }


            } else {
                if ( !btAdapter.isEnabled()) {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, ENABLE_BLUETOOTH);
                }
            }
        }
    };


    /**
     * Preparing for bluetooth after enabling it and
     * connecting to a peripheral (aka PoS) along with sending
     * the bluetoothdevice back to the main activity
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // we got a result where we requested to enable bluetooth
        if (requestCode == ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                // Bluetooth has been enabled, initialize the UI.
                Log.v(DEBUG_TAG, "Bluetooth Enabled");
                setUIForBTState(BluetoothAdapter.STATE_ON);
            }
            else if (resultCode == RESULT_CANCELED) {
                btToggle.setChecked(false);
                btToggle.setEnabled(true);
            }
            else {
                Log.v(DEBUG_TAG, "Enable Bluetooth adapter failed");
            }
        }
        else if (requestCode == CONNECT && resultCode == RESULT_OK) {
            btDevice = data.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            android.content.Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("Bluetooth<4", btDevice);
            startActivity(intent);
            finish();
        }
        else if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                String contents = data.getStringExtra("SCAN_RESULT");
                contents = contents.substring(contents.indexOf("b")+8, contents.length());
                EditText address = (EditText) findViewById(R.id.publicAddress);
                address.setText(contents);
            }
            if (resultCode == RESULT_CANCELED){
                //do nothing
            }
        }
        else if (requestCode == REQ_CREATE_PATTERN) {
                    if (resultCode == RESULT_OK) {
                        char[] pattern = data.getCharArrayExtra(
                                LockPatternActivity.EXTRA_PATTERN);
                        pinEntered = true;
                        usingPin = true;
                        SharedPreferences apiKeys = this.getSharedPreferences("pin", MODE_PRIVATE);
                        SharedPreferences.Editor prefsEditorApiKeys = apiKeys.edit();

                        prefsEditorApiKeys.putBoolean("usingPin", true);
                        prefsEditorApiKeys.commit();
                        Log.d("PATTERN", pattern.toString());
                    }
          }// REQ_CREATE_PATTERN

        else if(requestCode == REQ_ENTER_PATTERN) {
        /*
         * NOTE that there are 4 possible result codes!!!
         */
            switch (resultCode) {
                case RESULT_OK:
                    // The user passed
                    pinEntered = true;

                    // if success w/ existing pin and using existing pin + creating a new one
                    // then open new lock pattern creator
                    if(usingPin && creatingNewPin) {
                        Intent intent = new Intent(LockPatternActivity.ACTION_CREATE_PATTERN, null,
                                this, LockPatternActivity.class);
                        startActivityForResult(intent, REQ_CREATE_PATTERN);
                    }
                    if(disablePin) {
                        pinEntered = false;
                        usingPin = false;
                        SharedPreferences apiKeys = this.getSharedPreferences("pin", MODE_PRIVATE);
                        SharedPreferences.Editor prefsEditorApiKeys = apiKeys.edit();
                        prefsEditorApiKeys.putBoolean("usingPin", false);
                        prefsEditorApiKeys.commit();
                    }
                    break;
                case RESULT_CANCELED:
                    // The user cancelled the task
                    break;
                case LockPatternActivity.RESULT_FAILED:
                    // The user failed to enter the pattern
                    break;
                case LockPatternActivity.RESULT_FORGOT_PATTERN:
                    // The user forgot the pattern and invoked your recovery Activity.
                    break;
            }

        /*
         * In any case, there's always a key EXTRA_RETRY_COUNT, which holds
         * the number of tries that the user did.
         */
            try {
                int retryCount = data.getIntExtra(
                        LockPatternActivity.EXTRA_RETRY_COUNT, 0);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

        }// REQ_ENTER_PATTERN

    }




    private void disable_UI(){
        Button button;
        int[] buttonIds = { R.id.bt_toggle, R.id.bt_pick_device, R.id.send};
        for (int buttonId : buttonIds) {
            button = (Button) findViewById(buttonId);
            button.setEnabled(false);
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings, menu);
        menu.add("Setup Lock Pattern");
        menu.add("Disable Lock Pattern");
        return true;
    }

    public void show_message(String msg) {
        Toast.makeText(getBaseContext(), msg,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        try {
            if(item.getTitle()!=null) {
                if (item.getTitle().equals("Setup Lock Pattern") && !usingPin) {
                    Intent intent = new Intent(LockPatternActivity.ACTION_CREATE_PATTERN, null,
                            this, LockPatternActivity.class);
                    startActivityForResult(intent, REQ_CREATE_PATTERN);
                } else if (item.getTitle().equals("Setup Lock Pattern") && usingPin) {
                    // create new pin bool true
                    creatingNewPin = true;
                    // enter existing pin first

                    SharedPreferences apiKeys = this.getSharedPreferences("pin", MODE_PRIVATE);

                    Intent intent = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null,
                            this, LockPatternActivity.class);
                    //intent.putExtra(LockPatternActivity.EXTRA_PATTERN, savedPattern);
                    startActivityForResult(intent, REQ_ENTER_PATTERN);
                } else if (item.getTitle().equals("Disable Lock Pattern") && usingPin) {
                    Intent intent = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null,
                            this, LockPatternActivity.class);
                    //intent.putExtra(LockPatternActivity.EXTRA_PATTERN, savedPattern);
                    startActivityForResult(intent, REQ_ENTER_PATTERN);
                    disablePin = true;
                }
            }

        }
        catch (Exception e) {
            // null pointer due to no title on the item
            Log.d("Error", e.toString());
            e.printStackTrace();
        }
        if(id==R.id.home) {
            this.onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    public void pickDevice(View v){
        remoteDevice = null;
        discoveredDevices.clear();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                discoveredDevices.put(device.getName(), device);
            }
        }

       Intent showDevicesIntent = new Intent(this, pickDevice.class);
       showDevicesIntent.putExtra("hashmapDevices", discoveredDevices);
       startActivityForResult(showDevicesIntent, CONNECT);
    }

    /**
     * Called if using bitpay
     * Tells the Main activity we're using bitpay
     * and passes it the apikey for bitpay to store
     *
     */
    public void usingBitPay(android.view.View view) {


        //fetch the apikey string

        EditText apiKey = (EditText) findViewById(R.id.api_key);
        String apiKeyS = apiKey.getText().toString();
        if(apiKeyS.equals("")) {
            Context context = getApplicationContext();
            CharSequence text = "Please input your api key";
            Toast toast = Toast.makeText(context, text, 2);
            toast.setGravity(Gravity.CENTER, 0,5);
            toast.show();

        }

        else if(pinEntered || !usingPin) {
                // go to main class with this apikey string so main class can save it
                android.content.Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("APIKEYB", apiKeyS);
                startActivity(intent);
                finish();
            }
        else {
            Context context = getApplicationContext();
            CharSequence text = "Please enter your pin";
            Toast toast = Toast.makeText(context, text, 2);
            toast.setGravity(Gravity.CENTER, 0,5);
            toast.show();

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // enter existing pin
                    Intent intent = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null,
                            thisClass, LockPatternActivity.class);
                    // intent.putExtra(LockPatternActivity.EXTRA_PATTERN, savedPattern);
                    startActivityForResult(intent, REQ_ENTER_PATTERN);
                }
            }, 1000);

        }
    }

    /**
     * Called if using gocoin
     * Tells the Main activity we're using gocoin
     * and passes it the apikey for gocoin to store
     *
     */
    public void usingGocoin(android.view.View view) {

        // fetch apikey string
        EditText apiKey = (EditText) findViewById(R.id.api_key_gocoin_text);
        String apiKeyS = apiKey.getText().toString();
        EditText merchantID = (EditText) findViewById(R.id.gocoinMerchantID);
        String merchantIDS = merchantID.getText().toString();

        if(apiKeyS.equals("") || merchantIDS.equals("")) {
            Context context = getApplicationContext();
            CharSequence text = "Please input your api key";
            Toast toast = Toast.makeText(context, text, 2);
            toast.setGravity(Gravity.CENTER, 0, 5);
            toast.show();
        }

        else if(pinEntered || !usingPin) {
            // go to main class with this apikey string so main class can save it
            android.content.Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("APIKEYG", apiKeyS);
            intent.putExtra("GOCOINMERCHANTID", merchantIDS);
            startActivity(intent);
            finish();
        }
        else {
            Context context = getApplicationContext();
            CharSequence text = "Please enter your pin";
            Toast toast = Toast.makeText(context, text, 2);
            toast.setGravity(Gravity.CENTER, 0,5);
            toast.show();

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // enter existing pin
                    Intent intent = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null,
                            thisClass, LockPatternActivity.class);
                    // intent.putExtra(LockPatternActivity.EXTRA_PATTERN, savedPattern);
                    startActivityForResult(intent, REQ_ENTER_PATTERN);
                }
            }, 1000);
        }
    }


    /**
     * Called if using coinbase
     * Tells the Main activity we're using coibase
     * and passes it the apikey for coinbase to store
     *
     */
    public void usingCoinbase(android.view.View view) {
        // fetch apikey
        EditText apiKey = (EditText) findViewById(R.id.api_key_coinbase_text);
        String apiKeyS = apiKey.getText().toString();
        EditText apiSecret = (EditText) findViewById(R.id.api_secret_coinbase_text);
        String apiSecretS = apiSecret.getText().toString();

        if(apiKeyS.equals("") || apiSecretS.equals("")) {
            Context context = getApplicationContext();
            CharSequence text = "Please input your api key";
            Toast toast = Toast.makeText(context, text, 2);
            toast.setGravity(Gravity.CENTER, 0, 5);
            toast.show();
        }

        else if(pinEntered || !usingPin) {
            // go to main class with this apikey string so main class can save it
            android.content.Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("APIKEYC", apiKeyS);
            intent.putExtra("COINBASESECRET", apiSecretS);
            startActivity(intent);
            finish();
        }
        else {
            Context context = getApplicationContext();
            CharSequence text = "Please enter your pin";
            Toast toast = Toast.makeText(context, text, 2);
            toast.setGravity(Gravity.CENTER, 0,5);
            toast.show();

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // enter existing pin
                    Intent intent = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null,
                            thisClass, LockPatternActivity.class);
                    // intent.putExtra(LockPatternActivity.EXTRA_PATTERN, savedPattern);
                    startActivityForResult(intent, REQ_ENTER_PATTERN);
                }
            }, 1000); }

    }

    /**
     * Called if using own personal wallet
     * Tells main activity this
     */
    public void usingPersonal(android.view.View view) {
        // fetch public address
        EditText address = (EditText) findViewById(R.id.publicAddress);
        String addressS = address.getText().toString();
        if(addressS.equals("")) {
            Context context = getApplicationContext();
            CharSequence text = "Please input your address";
            Toast toast = Toast.makeText(context, text, 2);
            toast.setGravity(Gravity.CENTER, 0, 5);
            toast.show();
        }

        else if(pinEntered || !usingPin) {
            // go to main class with this apikey string so main class can save it
            android.content.Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("ADDRESS", addressS);
            startActivity(intent);
            finish();
        }
        else {
            Context context = getApplicationContext();
            CharSequence text = "Please enter your pin";
            Toast toast = Toast.makeText(context, text, 2);
            toast.setGravity(Gravity.CENTER, 0,5);
            toast.show();
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // enter existing pin
                    Intent intent = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null,
                            thisClass, LockPatternActivity.class);
                    // intent.putExtra(LockPatternActivity.EXTRA_PATTERN, savedPattern);
                    startActivityForResult(intent, REQ_ENTER_PATTERN);
                }
            }, 1000);
        }
    }

    @Override
    public void onStop() {
        clientConnectThread = null;
        pinEntered = false;
        creatingNewPin = false;
        super.onStop();
    }



    public View getView(int arg0, View arg1, ViewGroup arg2) {
        // TODO Auto-generated method stub
        LayoutInflater inflater = (LayoutInflater) SettingsActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

       // ChatBox chatBox = chatBoxList.get(arg0);

        TextView BTDeviceName;
        TextView BT_Chat;

        if (1!=2){
            arg1 = inflater.inflate(R.layout.listview_right, arg2,false);
            BTDeviceName = (TextView)arg1.findViewById(R.id.RtextView1);
            BT_Chat = (TextView)arg1.findViewById(R.id.RtextView2);
        } else {
            arg1 = inflater.inflate(R.layout.listview_left, arg2,false);
            BTDeviceName = (TextView)arg1.findViewById(R.id.LtextView1);
            BT_Chat = (TextView)arg1.findViewById(R.id.LtextView2);
        }

        BTDeviceName.setText("");
        BT_Chat.setText("");

        return arg1;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_settings);
    }

    public void scanQR (android.view.View view) {
        try {
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE"); // "PRODUCT_MODE for bar codes
            startActivityForResult(intent, 0);
        } catch (Exception e) {
            Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
            Intent marketIntent = new Intent(Intent.ACTION_VIEW,marketUri);
            startActivity(marketIntent);
        }

    }



}