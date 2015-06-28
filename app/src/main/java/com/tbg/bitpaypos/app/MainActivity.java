package com.tbg.bitpaypos.app;

import android.annotation.TargetApi;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.haibison.android.lockpattern.widget.LockPatternView;
import com.stripe.android.*;

import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.triangle.Session;
import io.triangle.TriangleException;

/**
 * @author Joseph Krug
 * @date 6.4.14
 */
public class MainActivity extends ActionBarActivity {

    public final static String EXTRA_MESSAGE = "com.tbg.bitpaypos.app.ID";
    public final static String EXTRA_MESSAGE2 = "com.tbg.bitpaypos.app.PRICE";
    private String apiKeyBitpay = null;
    private String apiKeyCoinbase = null;
    private String apiKeyGocoin = null;
    private String goCoinMerchantID = null;
    private String coinbaseSecret = null;
    private String learnBitcoin = "https://bitcoin.org/en/";
    private String publicAddress = null;

    public boolean foreground;

    public MainActivity thisClass = this;
    private NfcAdapter mNfcAdapter;

    private volatile String coinbaseOrderID = "";
    private volatile CoinBase coinBase;

    private volatile JSONObject result;


    // which pos processor var has 1 for bitpay, 2 for coinbase, 3 for gocoin
    private int whichPOS = 1;


    // Bluetooth stuff
    private static final String DEBUG_TAG = "Bluetooth";
    private static final int ENABLE_BLUETOOTH = 1;


    public static final int DATA_RECEIVED = 3;
    public static final int SOCKET_CONNECTED = 4;
    public static final int STATUS = 7;
    private static final int CONNECT = 2;

    private BluetoothAdapter btAdapter;
    private BtReceiver btReceiver;

    private BluetoothDevice remoteDevice = null;
    private BluetoothDevice btDevice = null;

    public static final UUID BT_APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static String bc04_address = "00:06:71:00:3e:aa";	// BC-04 address

    private ClientConnectThread clientConnectThread;
    private BluetoothDataCommThread bluetoothDataCommThread;

    private MediaPlayer player;

    private EditText editMsg, editID, editPrice;
    private TextView status;
    //TextView txtView1;
    private Switch btToggle;
    private Button pickDevice;
    private ListView listChat;
    private ListView list;
    private Button send;
    private TextView myBtDevName;

    boolean got_a_socket = false;

    char[] display_txt = new char[64];
    int d_index = 0;

    private HashMap<String, BluetoothDevice> discoveredDevices = new HashMap<String, BluetoothDevice>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        foreground = true;
        doQRCreation();

        editID = (EditText) findViewById(R.id.edit_message);
        editPrice = (EditText) findViewById(R.id.edit_price);


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            // open settings menu
            android.content.Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;

        }

        return super.onOptionsItemSelected(item);
    }

    public void doQRCreation() {
        File file = QRCode.from(learnBitcoin).file();

        android.graphics.
                Bitmap myQRImg = BitmapFactory.decodeFile(file.getAbsolutePath());
        myQRImg = myQRImg.createScaledBitmap(myQRImg, 350, 350, true);
        ImageView learnBtc = (ImageView) findViewById(R.id.learnAboutBtc);
        learnBtc.setImageBitmap(myQRImg);
        startNFC();
    }

    /**
     * Called when user clicks the submit button
     * Gets up-to-date api keys and which pos we're using
     * Also passes price/order id to display message activity
     */
    public void sendMessage(android.view.View view) {

        //fetch apikey textbox for bitpay
        EditText apiKey = (EditText) findViewById(R.id.api_key);

        //get sharedpreferences for the apikeys
        SharedPreferences apiKeys = this.getSharedPreferences("apiKeys", MODE_PRIVATE);


        // get the intent for this class
        android.content.Intent settingsIntent = getIntent();

        // if it happened to be settings and it was called via bitpay button...
        // update apikey value in saved preferences/settings
        if (settingsIntent.hasExtra("APIKEYB")) {
            SharedPreferences.Editor prefsEditorApiKeys = apiKeys.edit();
            apiKeyBitpay = settingsIntent.getStringExtra("APIKEYB");
            prefsEditorApiKeys.putString("bitpayAPIKey", apiKeyBitpay);
            whichPOS = 1;
            prefsEditorApiKeys.putInt("whichPOS", 1);
            prefsEditorApiKeys.commit();
        }

        // pos 2 is Coinbase
        // called if extra APIKEYC is pushed
        if (settingsIntent.hasExtra("APIKEYC")) {
            SharedPreferences.Editor prefsEditorApiKeys = apiKeys.edit();
            apiKeyCoinbase = settingsIntent.getStringExtra("APIKEYC");
            coinbaseSecret = settingsIntent.getStringExtra("COINBASESECRET");
            prefsEditorApiKeys.putString("coinbaseAPIKey", apiKeyCoinbase);
            prefsEditorApiKeys.putString("coinbaseSecret", coinbaseSecret);
            whichPOS = 2;
            prefsEditorApiKeys.putInt("whichPOS", 2);
            prefsEditorApiKeys.commit();
        }

        // pos 3 is gocoin
        // called if APIKEYG is pushed
        if (settingsIntent.hasExtra("APIKEYG") && settingsIntent.hasExtra("GOCOINMERCHANTID")) {
            SharedPreferences.Editor prefsEditorApiKeys = apiKeys.edit();
            apiKeyGocoin = settingsIntent.getStringExtra("APIKEYG");
            goCoinMerchantID = settingsIntent.getStringExtra("GOCOINMERCHANTID");
            prefsEditorApiKeys.putString("gocoinAPIKey", apiKeyGocoin);
            prefsEditorApiKeys.putString("goCoinMerchantID", goCoinMerchantID);
            whichPOS = 3;
            prefsEditorApiKeys.putInt("whichPOS", 3);
            prefsEditorApiKeys.commit();
        }

        // used if public own wallet is used
        if(settingsIntent.hasExtra("ADDRESS")) {
            SharedPreferences.Editor prefsEditorApiKeys = apiKeys.edit();
            publicAddress = settingsIntent.getStringExtra("ADDRESS");
            prefsEditorApiKeys.putString("publicAddress", publicAddress);

            // using public wallet
            whichPOS = 4;
            prefsEditorApiKeys.putInt("whichPOS", 4);
            prefsEditorApiKeys.commit();

        }

        // refetch preferences to get any updates
        apiKeys = this.getSharedPreferences("apiKeys", MODE_PRIVATE);

        // get which pos we're using
        // defaults to bitpay if nothing set yet
        whichPOS = apiKeys.getInt("whichPOS", 1);

        // get bitpay's apikey
        apiKeyBitpay = apiKeys.getString("bitpayAPIKey", null);

        // WOULD BE NICE TO UPDATE THIS SHIT TO HAVE API KEY DISPLAYED
        // IN SETTINGSACTIVITY, BUT THIS IS A PITA FOR SOME REASON

        // get coinbase's apikey
        apiKeyCoinbase = apiKeys.getString("coinbaseAPIKey", null);
        coinbaseSecret = apiKeys.getString("coinbaseSecret", null);

        // get gocoin's apikey and merchant id
        apiKeyGocoin = apiKeys.getString("gocoinAPIKey", null);
        goCoinMerchantID = apiKeys.getString("goCoinMerchantID", null);

        // get public address
        publicAddress = apiKeys.getString("publicAddress", null);


        // upon submit we go to the displayMessage/Invoice class
        android.content.Intent intent = new android.content.Intent(this, DisplayMessageActivity.class);

        // pass the orderID, apiKeys, pos type, and price to the next activity
        String idMessage = editID.getText().toString();
        String priceMessage = editPrice.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, idMessage);
        intent.putExtra(EXTRA_MESSAGE2, priceMessage);


        intent.putExtra("bitpayAPIKey", apiKeyBitpay);
        intent.putExtra("coinbaseAPIKey", apiKeyCoinbase);
        intent.putExtra("gocoinAPIKey", apiKeyGocoin);
        intent.putExtra("coinbaseSecret", coinbaseSecret);
        intent.putExtra("goCoinMerchantID", goCoinMerchantID);
        intent.putExtra("publicAddress", publicAddress);

        // sends pos type to next activity
        intent.putExtra("POS_TYPE", whichPOS);
        if(idMessage.equals("")) {
            idMessage = "0";
        }

        // if idmesage or pricemessage are empty restart this activity
        if (idMessage.equals("") || priceMessage.equals("")) {
            Context context = getApplicationContext();
            CharSequence text = "Please input a valid price and ID";
            Toast toast = Toast.makeText(context, text, 2);
            //toast.setGravity(Gravity.CENTER, 0,5);
            toast.show();
        }
        // android.content.Intent intentRetryNum = new Intent(this, MainActivity.class);
        // startActivity(intentRetryNum);
        // finish();


        // try to start the invoice creation activity
        // make sure the price is a double, if not e is thrown
        // and this activity is restarted
        try {
            if (idMessage.equals("") || priceMessage.equals("")) {
                throw new NumberFormatException();
            } else {
                Double.parseDouble(priceMessage);
                startActivity(intent);
            }
        } catch (NumberFormatException e) {
            Context context = getApplicationContext();
            CharSequence text = "Please input a valid price and ID";
            Toast toast = Toast.makeText(context, text, 2);
            //toast.setGravity(Gravity.CENTER, 0,5);
            toast.show();
            Log.d("Exception", "e");
            //android.content.Intent intentRetryNum = new Intent(this, MainActivity.class);
            //startActivity(intentRetryNum);
            //finish();
        }

    }


    /**
     * Start NFC behavior
     */
    public void startNFC() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    PackageManager pm = getPackageManager();
                    if (pm.hasSystemFeature(PackageManager.FEATURE_NFC) && Build.VERSION.SDK_INT >= 16 && foreground) {
                        mNfcAdapter = NfcAdapter.getDefaultAdapter(thisClass);
                        if (!mNfcAdapter.isEnabled())
                        {
                            Toast.makeText(getApplicationContext(), "Please activate NFC and press Back to return to the application!", Toast.LENGTH_LONG).show();
                            startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                        }
                        mNfcAdapter.enableForegroundNdefPush(thisClass, thisClass.createNdefMessage());
                        Log.d("NFC", "NFC successful");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("Error", "Error doing NFC");
                }
               //  Bitcoinj wah = new Bitcoinj();

            }
        }, 1000);
    }


    /**
     * called if using a credit card
     */
    public void usingCC(View view) {

        Intent intent = new Intent(this, ScanResultActivity.class);

        // upon submit we go to the displayMessage/Invoice class


        // pass the orderID, apiKeys, pos type, and price to the next activity
        String idMessage = editID.getText().toString();
        String priceMessage = editPrice.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, idMessage);
        intent.putExtra(EXTRA_MESSAGE2, priceMessage);

        // if idmesage or pricemessage are empty restart this activity
        if (idMessage.equals("") || priceMessage.equals("")) {
            Context context = getApplicationContext();
            CharSequence text = "Please input a valid price and ID";
            Toast toast = Toast.makeText(context, text, 2);
            //toast.setGravity(Gravity.CENTER, 0,5);
            toast.show();
        }
        // android.content.Intent intentRetryNum = new Intent(this, MainActivity.class);
        // startActivity(intentRetryNum);
        // finish();


        // try to start the invoice creation activity
        // make sure the price is a double, if not e is thrown
        // and this activity is restarted
        try {
            if (idMessage.equals("") || priceMessage.equals("")) {
                throw new NumberFormatException();
            } else {
                Double.parseDouble(priceMessage);
                startActivity(intent);
            }
        } catch (NumberFormatException e) {
            Context context = getApplicationContext();
            CharSequence text = "Please input a valid price and ID";
            Toast toast = Toast.makeText(context, text, 2);
            //toast.setGravity(Gravity.CENTER, 0,5);
            toast.show();
            Log.d("Exception", "e");
            //android.content.Intent intentRetryNum = new Intent(this, MainActivity.class);
            //startActivity(intentRetryNum);
            //finish();

        }
    }

    @Override
    public void onResume() {
        foreground = true;
        super.onResume();


        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter == null) {
            // no bluetooth available on device
            show_message("Couldn't detect bluetooth on your device");
            disable_UI();
        } else {
            // bluetooth is available on the device
            Log.v(DEBUG_TAG, "Bluetooth available on device");


            btReceiver = new BtReceiver();


            regBroadcasts();    // set broadcast receivers

        }

        android.content.Intent btIntent = getIntent();

        // connect to pos bluetooth device
        if(btIntent.hasExtra("Bluetooth<4")) {
            btDevice = (BluetoothDevice) btIntent.getParcelableExtra("Bluetooth<4");
            remoteDevice = btDevice;
            doConnectToDevice(btDevice);
        }

        else {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    tryToUseDevice();
                }
            }, 500);
        }

        if(btIntent.hasExtra("COINBASE_ORDER_ID")) {
            coinbaseOrderID = btIntent.getStringExtra("COINBASE_ORDER_ID");
        }
        // enable nfc foreground push
        if (mNfcAdapter != null)
            mNfcAdapter.enableForegroundNdefPush(thisClass, thisClass.createNdefMessage());
        // connection.resume();
    }

    /**
     * Creates nfc message from uri
     *
     * @return msg - nfc message (bitcoin uri)
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public NdefMessage createNdefMessage() {
        if (Build.VERSION.SDK_INT >= 16 && foreground) {
            NdefRecord nfcUriRecord = NdefRecord.createUri(learnBitcoin);
            NdefMessage msg = new NdefMessage(nfcUriRecord);
            Log.d("MSG", msg.toString());
            return msg;
        } else {
            return null;
        }
    }

    @Override
    public void onPause() {
        foreground = false;
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundNdefPush(this);
        }

        try {
            close_threads();
            remoteDevice = btDevice;
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Failed to start audio", e);
        }
        super.onPause();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_main);
        doQRCreation();
    }

    @Override
    public void onStop() {
        try {
            close_threads();
        }
        catch (Exception e) {
            Log.d("Exception:", "failed to close thread or receiver not registered yet");
        }
        foreground = false;
        super.onStop();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

    }

    public void show_message(String msg) {
        Toast.makeText(getBaseContext(), msg,
                Toast.LENGTH_SHORT).show();
    }

    private void disable_UI(){
        Button button;
        int[] buttonIds = { R.id.bt_toggle, R.id.bt_pick_device, R.id.send};
        for (int buttonId : buttonIds) {
            button = (Button) findViewById(buttonId);
            button.setEnabled(false);
        }
        editMsg.setEnabled(false);
    }

    /**
     * Register bluetooth serial broadcast events
     */
    public void regBroadcasts(){
        // register for state change broadcast events
        IntentFilter stateChangedFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        this.registerReceiver(btReceiver, stateChangedFilter);

        // register for local name changed events
        IntentFilter nameChangedFilter = new IntentFilter(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED);
        this.registerReceiver(btReceiver, nameChangedFilter);
    }

    /**
     * Close bluetooth threads
     */
    public void close_threads(){
        if (clientConnectThread != null) {
            clientConnectThread.stopConnecting();
        }

        if (bluetoothDataCommThread != null) {
            bluetoothDataCommThread.disconnect();
        }

        remoteDevice = null;
        clientConnectThread = null;
        bluetoothDataCommThread = null;
        got_a_socket = false;

        btAdapter.cancelDiscovery();

        try {
            this.unregisterReceiver(btReceiver);
        }
        catch (Exception e) {
            // receiver not registered
        }
        if (player != null) {
            player.stop();
            player.reset();
            player.release();
            player = null;
        }

    }


    /**
     * Bluetooth receiver
     */
    private class BtReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v(DEBUG_TAG, "Broadcast: Got some intent");
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                Log.v(DEBUG_TAG, "Broadcast: Got ACTION_STATE_CHANGED");
                int currentState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                //setUIForBTState(currentState);
            }
            else if (action.equals(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED)) {
                Log.v(DEBUG_TAG, "Broadcast: Got ACTION_LOCAL_NAME_CHANGED");

            }
        }
    }

    /**
     *  begin device discovery for bluetooth devices
     *  attempt to connect to PoS serial device
     */

    public void tryToUseDevice() {
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                Log.d("M", "made it here"+device.getName());

                if(device.getName().equals("BOLUTEK")) {
                    btDevice = device;
                    Log.d("M", "connecting to bolutek device");
                    doConnectToDevice(btDevice);
                }
                else {
                    // nothing
                }
            }
        }
    }

    /**
     * Connect to a bluetooth device
     * @param device - the bluetooth device we're connecting to
     */
    public void doConnectToDevice(BluetoothDevice device) {
        // halt the resource intensive bluetooth discovery
        btAdapter.cancelDiscovery();
        Log.v(DEBUG_TAG, "Starting connect thread");
        clientConnectThread = new ClientConnectThread(device, handler);
        clientConnectThread.start();
    }

    /**
     * Bluetooth handler to receive messages from point of sale
     * this receives message sent from client connect thread
     */
    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SOCKET_CONNECTED: {
                    bluetoothDataCommThread = (BluetoothDataCommThread) msg.obj;
                    got_a_socket = true;
                    //bluetoothDataCommThread.send("this is a message");
                    break;
                }
                case DATA_RECEIVED: {
                    String data = ((String) msg.obj);

                    if (data != null ){

                        for ( int i = 0; i < data.length(); i++ ) {
                            char ch = data.charAt(i);
                            display_txt[d_index++] = ch;
                            if ( ch == '\n' ) {
                                display_txt[--d_index] = '\0';
                                String out = String.copyValueOf(display_txt);
                                out.trim();
                                Log.v(DEBUG_TAG, out);
                                Log.d("Bluetooth Comms: ", out);

                                // fill in order info by parsing the message from point of sale
                                // for different pos different parsing options need to be included
                                fillInOrder(out);

                                // sendMsg("How are you doing?" + "\n");
                                d_index = 0;

                                // Clear display_txt array
                                for ( int j = 0; j < 64; j++)
                                    display_txt[j] = '\0';

                                //play_audio(0);
                                break;
                            }
                        }
                    }
                    else {
                        Log.v(DEBUG_TAG, "DATA_RECEIVED: NULL");
                    }

                    break;
                }
                case STATUS: {
                  //  status.setText((String)msg.obj);
                    break;
                }
                default:
                    break;
            }
        }
    };


    /**
     * Fill in order information from the serial-bluetooth adapter
     * @param orderInfo
     */
    public void fillInOrder(String orderInfo) {
       // last 2 positions are decimals
       String priceString = (orderInfo.substring(13, 22) + "." + orderInfo.substring(22, 24)).replaceFirst("^0*", "");
       editID.setText(orderInfo.substring(44, 50));
       editPrice.setText(priceString);
    }


    /**
     * Can be used to play a sound if message / order info received from point of sale
     * @param who
     */
    public void play_audio(int who){
        if (player != null) {
            player.reset();
            player.release();
        }
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            Log.v(DEBUG_TAG, "Play Audio");
            if ( who == 1 ) {
                player.setDataSource(getBaseContext(), Uri.parse("android.resource://com.tbg.bitpaypos.app/" + R.raw.send));
            } else {
                player.setDataSource(getBaseContext(), Uri.parse("android.resource://com.tbg.bitpaypos.app/"+R.raw.received));
            }
            player.prepare();
            player.start();
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Failed to start audio", e);
        }
    }

    /**
     * Send a message to point of sale
     * @param message
     */
    public void sendMsg(String message){
        if ( got_a_socket ) {
            if (btAdapter.isDiscovering()) btAdapter.cancelDiscovery();
            String deviceName = btAdapter.getName();
            if ( bluetoothDataCommThread != null && got_a_socket ) {
                bluetoothDataCommThread.send(message);	///////////////////
               // addChats(deviceName, myMsg);
                //play_audio(1);
            }
        } else {
            show_message("Message not sent.\nNot connected to a remote device");
        }
    }

    @Override
    protected void onDestroy() {
        try {
            close_threads();
        }
        catch (Exception e) {
            Log.d("Exception:", "failed to close thread");
        }
        super.onDestroy();
    }


    /**
     * This refunds the last order made
     * @param view
     */
    public void refundLastOrder(View view) {
        // fetch preferences to get any updates
        SharedPreferences apiKeys = this.getSharedPreferences("apiKeys", MODE_PRIVATE);
        // get coinbase's apikey
        apiKeyCoinbase = apiKeys.getString("coinbaseAPIKey", null);
        coinbaseSecret = apiKeys.getString("coinbaseSecret", null);

        // get which pos
        whichPOS = apiKeys.getInt("whichPOS", 1);

        // bitpay
        if(whichPOS==1) {
            Context context = getApplicationContext();
            CharSequence text = "Bitcoin payment protocol refunds aren't available with Bitpay yet, please ask them about it!";
            Toast toast = Toast.makeText(context, text, 2);
            //toast.setGravity(Gravity.CENTER, 0,5);
            toast.show();
        }

        if (whichPOS == 2 && !coinbaseOrderID.equals("")) {



            // create the exception handler to catch uncaught exception from coinbase class
            // e.g. null apikey
            Thread.UncaughtExceptionHandler handle = new Thread.UncaughtExceptionHandler() {
                public void uncaughtException(Thread th, Throwable ex) {
                    System.out.println("Uncaught exception: " + ex);
                    ex.printStackTrace();

                    Context context = getApplicationContext();
                    CharSequence text = "Network is not available, enable a wifi connection";
                    Toast toast = Toast.makeText(context, text, 2);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();

                    android.content.Intent intent = new Intent(thisClass, MainActivity.class);
                    startActivity(intent);
                }
            };

            // new thread needed for network activities
            Thread t = new Thread() {
                @Override
                public void run() {

                    try {
                        String mispaymentsID = "";

                        coinBase = new CoinBase(apiKeyCoinbase, coinbaseSecret);

                        // check for null api keys and redirect to settings if it's so!!!
                        // else create bitpay invoice and get its url
                        try {
                            Log.d("W", "coinbase created");
                            Log.d("W", apiKeyCoinbase);
                            Log.d("W", coinbaseSecret);
                            JSONObject refundCoinbase = new JSONObject();


                            refundCoinbase.put("instant_buy", "true");
                            refundCoinbase.put("refund_iso_code", "USD");


                            JSONObject json = new JSONObject();
                            json.put("order", refundCoinbase);

                            Log.d("REFUND", json.toString());

                            try {
                                JSONObject orderResult = coinBase.getHttp("https://coinbase.com/api/v1/orders/" + coinbaseOrderID);
                                Log.d("RESULT", orderResult.toJSONString());
                                Log.d("Result", orderResult.toString());
                                JSONObject order = (JSONObject) orderResult.get("order");
                                org.json.simple.JSONArray mispayments = (org.json.simple.JSONArray) order.get("mispayments");
                                JSONObject misPayObj = (JSONObject) mispayments.get(0);
                                mispaymentsID = (String) misPayObj.get("id");

                                Log.d("WOW", mispayments.toString() + "    " + mispaymentsID);

                                if (mispaymentsID != null && !mispaymentsID.equals("")) {
                                    refundCoinbase.put("mispayment_id", mispaymentsID);
                                }

                            } catch (NoSuchAlgorithmException e) {
                                e.printStackTrace();
                            } catch (InvalidKeyException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (NullPointerException e) {
                                // order not paid yet
                                Log.d("exceptionw", e.toString());
                                e.printStackTrace();
                            }


                            try {
                                // use coinbaseorderid after payment done otherwise mispamentid
                                result = coinBase.postHttp("https://coinbase.com/api/v1/orders/" + coinbaseOrderID + "/refund", json.toString());
                                JSONObject orderResult = coinBase.getHttp("https://coinbase.com/api/v1/orders/" + coinbaseOrderID);
                                Log.d("URL", "https://coinbase.com/api/v1/orders/" + coinbaseOrderID);
                            } catch (InvalidKeyException e) {
                                e.printStackTrace();
                            } catch (NoSuchAlgorithmException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            // JSONObject s = (JSONObject) result.get("order");
                            // s.get("status");
                            // Log.d("WHAT", (String) s.get("status"));
                            Log.d("REFUNDRESULT", result.toString());
                            if (result.toString().contains("No refund address is present")) {
                                Context context = getApplicationContext();
                                CharSequence text = "Customer wallet doesn't support bip70 (refund protocol) \n manually send a refund to the customer's address \n from your coinbase account";
                                Toast toast = Toast.makeText(context, text, 2);
                                //toast.setGravity(Gravity.CENTER, 0,5);
                                toast.show();

                            } else if (result.toString().contains("fee to be accepted")) {
                                Context context = getApplicationContext();
                                CharSequence text = "Amount is too small for Coinbase auto refund \n manually send a refund to the customer's address \n from your coinbase account";
                                Toast toast = Toast.makeText(context, text, 2);
                                //toast.setGravity(Gravity.CENTER, 0,5);
                                toast.show();

                            } else if (result.toString().contains("true")) {
                                Context context = getApplicationContext();
                                CharSequence text = "Payment Refunded";
                                Toast toast = Toast.makeText(context, text, 2);
                                //toast.setGravity(Gravity.CENTER, 0,5);
                                toast.show();
                            }
                            //   JSONObject order = (JSONObject) result.get("order");
                            //  JSONObject button = (JSONObject) order.get("button");


                        }
                        // if api key is wrong we get a null pointer
                        // go to settings screen in this case
                        catch (NullPointerException e) {
                            //   apiIsAGo = false;
                            android.content.Intent intentSettings = new Intent(thisClass, SettingsActivity.class);
                            intentSettings.putExtra("apifail?", true);
                            startActivity(intentSettings);

                            finish();
                            // Log.d("AA", "" + apiIsAGo + e);
                            e.printStackTrace();
                        }
                    } finally {
                        // other threads can now access this info
                    }


                }

            };
            t.setUncaughtExceptionHandler(handle);
            t.start();
        }

        // gocoin
        else if (whichPOS==3) {
            Context context = getApplicationContext();
            CharSequence text = "Refunds aren't available with GoCoin yet, please ask them about it!";
            Toast toast = Toast.makeText(context, text, 2);
            //toast.setGravity(Gravity.CENTER, 0,5);
            toast.show();
        }

        // personal wallet
        else {
            Context context = getApplicationContext();
            CharSequence text = "Please make a refund from your personal wallet";
            Toast toast = Toast.makeText(context, text, 2);
            //toast.setGravity(Gravity.CENTER, 0,5);
            toast.show();
        }

    }
}

// items like normal pos style + square pay
// quickbooks integration/CSV addon
// stripe doesn't accept google wallet due to no card present tx, need merchant acc for that and triangle higher lvl scanning
// pin number if you lose it you must delete and reinstall app (preventing randoms from changing your code)


// // test FillInOrder, test ble pay w/ new wallet setup, test // refund last order option on main screen..
// emv
// diff. ui layout for tablet & phone

// ios dev
// slidedeck
// logo
// add ble beam info

// yc app