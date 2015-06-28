package com.tbg.bitpaypos.app;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gocoin.api.GoCoin;
import com.gocoin.api.JSON;
import com.gocoin.api.pojo.Token;
import com.gocoin.api.services.InvoiceService;
import com.m1pay.nfsound.NFSoundActivity;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import ch.boye.httpclientandroidlib.impl.client.HttpClientBuilder;


@TargetApi(Build.VERSION_CODES.L)
public class DisplayMessageActivity extends ActionBarActivity implements SensorEventListener, NfcAdapter.CreateNdefMessageCallback {


    // accelerometer info
    private float mLastX, mLastY, mLastZ;
    private boolean mInitialized;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private final float NOISE = (float) 2.0;


    private volatile String invoiceURL = "";
    private volatile double price = 0.0;

    private volatile String btcPrice;
    private volatile String btcAddress;

    // coinbase button id
    private volatile String coinBaseID;
    // this is order id assigned by coinbase
    // merchant id on receipt goes on custom id in coinbase
    private volatile String coinbaseOrderID = "";

    // initization of order id params which takes order id message
    // declarations for bitpay
    private volatile InvoiceParams orderIDParam = null;
    private volatile Invoice invoice = null;
    private volatile BitPay bitpay = null;

    // gocoin declarations
    private volatile com.gocoin.api.pojo.Invoice goCoinInvoiceSetup = new com.gocoin.api.pojo.Invoice();
    private volatile InvoiceService goCoinInvoiceService;
    private volatile Token goCoinToken;
    private String serviceOneCharUuid;

    // used to check whether payment is confirmed or not
    private volatile boolean notConfirmed = true;

    private volatile NfcAdapter mNfcAdapter;

    private volatile TextView textView;

    // which pos we are using
    private int whichPOS = 1;

    private volatile CoinBase coinBase;
    private volatile JSONObject result;

    private volatile boolean foreground = true;

    // locks used so can wait until invoice is created
    // before checking for confirmation / transmitting nfc message
    private volatile Lock lock = new ReentrantLock();
    private volatile Lock nfcLock = new ReentrantLock();
    private volatile Lock refundLock = new ReentrantLock();

    private DisplayMessageActivity thisClass = this;

    // used for if exception thrown when checking transaction confirmation
    private Thread.UncaughtExceptionHandler h;

    private volatile boolean apiIsAGo = true;

    // used for storing bitcoin uri
    private volatile String url = "";


    private NFSoundActivity sound = null;
    private volatile boolean soundPlaying = false;

    // vars for wallet balance
    private volatile long balance;
    private volatile long unconfirmed_balance;
    private double initialWalletBalanceBtc;

    String bitpayApiKey;
    String coinbaseApiKey;
    String gocoinAPIKey;
    String gocoinMerchantID;
    String coinbaseSecret;
    String publicAddress;

    // used for ble wallet communications
    BluetoothUtility ble;
    private static final String SERVICE_UUID_1 = "00001802-0000-1000-8000-00805f9b34fb";


    /**
     * Get price and api keys from main screen
     * Decide which PoS we're using and call its methods
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_message);
        android.content.Intent intent = getIntent();


        // this Gets the message order id and price from business
        String idMessage = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        String priceMessage = intent.getStringExtra(MainActivity.EXTRA_MESSAGE2);
        price = Double.parseDouble(priceMessage);



        // this gets whichever POS we're using
        // defaults to 1 (BitPay)
        // check for null api keys and redirect to settings if it's so
        whichPOS = intent.getIntExtra("POS_TYPE", 1);
        bitpayApiKey = intent.getStringExtra("bitpayAPIKey");
        coinbaseApiKey = intent.getStringExtra("coinbaseAPIKey");
        gocoinAPIKey = intent.getStringExtra("gocoinAPIKey");
        gocoinMerchantID = intent.getStringExtra("goCoinMerchantID");
        coinbaseSecret = intent.getStringExtra("coinbaseSecret");
        publicAddress = intent.getStringExtra("publicAddress");


        new Thread() {
            @Override
            public void run() {
                // m accelerometer stuff
                // used to detect movement and play sound
                mInitialized = false;
                mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                mSensorManager.registerListener(thisClass, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }.run();


        // if whichPOS  == 1 then bitpay else if 2 call coinbase else if 3 call gocoin methods
        // if 4 then call personal address methods
        if (whichPOS == 1 && isNetworkAvailable()) {
            bitPayPOS(idMessage, bitpayApiKey);
        } else if (whichPOS == 2 && isNetworkAvailable()) {
            coinBasePOS(idMessage, coinbaseApiKey, coinbaseSecret, priceMessage);
        } else if (whichPOS == 3 && isNetworkAvailable()) {
            goCoinPOS(idMessage, gocoinAPIKey, priceMessage, gocoinMerchantID);
        } else if (whichPOS == 4 && isNetworkAvailable()) {
            btcAddress = publicAddress;
            new Thread() {
                @Override
                public void run() {
                    checkTxChain();
                    initialWalletBalanceBtc = ((double)(unconfirmed_balance)/10000000);
                }
            }.start();
            Log.d("WHAT", "!"+initialWalletBalanceBtc);
            ownWallet(idMessage, priceMessage, publicAddress);
        }
        else {
            Log.d("Error", "Network is not available");
            Context context = getApplicationContext();
            CharSequence text = "Network is not available, enable a wifi connection";
            Toast toast = Toast.makeText(context, text, 2);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();

            // go to main screen
            android.content.Intent intentMain = new Intent(thisClass, MainActivity.class);
            startActivity(intentMain);
            finish();
        }
    }


    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not needed
    }

    /** check if BLE Supported device */
    public static boolean isBLESupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * Used to play near field sound when sensor tapped
     * @param event - accelerometer event
     */
    public void onSensorChanged(SensorEvent event){
        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.

        String X = "";
        String Y = "";
        String Z = "";

        // 0 is x, 1 is y, 2 is z
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        if(!mInitialized) {
            mLastX = x;
            mLastY = y;
            mLastZ = z;
            X = "0.0";
            Y = "0.0";
            Z = "0.0";
            mInitialized = true;

        }
        else {
            float deltaX = Math.abs(mLastX - x);
            float deltaY = Math.abs(mLastY - y);
            float deltaZ = Math.abs(mLastZ - z);

            if (deltaX < NOISE) deltaX = (float)0.0;
            if (deltaY < NOISE) deltaY = (float)0.0;
            if (deltaZ < NOISE) deltaZ = (float)0.0;

            mLastX = x;
            mLastY = y;
            mLastZ = z;


            X = Float.toString(deltaX);
            Y = Float.toString(deltaY);
            Z = Float.toString(deltaZ);

           // Log.d("DIFF", X+"  "+Y+"   "+Z);

            if (deltaZ > 1.5) {
                try {
                    if (sound != null && !url.equals("") && foreground && apiIsAGo && notConfirmed && !soundPlaying) {
                        soundPlaying = true;
                        sound.sendString();
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if(sound!=null) {
                                    sound.sendString();
                                }
                                Handler handlerBool = new Handler();
                                handlerBool.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        soundPlaying = false;
                                    }
                                }, 1400); // 5000ms delay

                            }
                        }, 1400); // 5000ms delay

                    }
                }
                catch (Exception e) {
                    Log.d("Error", e.toString());
                }
            }

        }
    }




    /**
     * Creates a bitpay instance + invoice
     * @param idMessage - order id
     * @param bitpayApiKey
     */
    public void bitPayPOS(final String idMessage, final String bitpayApiKey) {

        // Creates an invoice using params passed from mainactivity
        orderIDParam = new InvoiceParams();
        orderIDParam.setOrderId(idMessage);

        // create the exception handler to catch uncaught exception from bitpay class
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
                // have to use lock b/c need to check for invoice in while loop later
                lock.lock();
                refundLock.lock();
                nfcLock.lock();
                try {


                    // check for null api keys and redirect to settings if it's so!!!
                    // else create bitpay invoice and get its url
                    try {
                        bitpay = new BitPay(bitpayApiKey, "USD");
                        invoice = bitpay.createInvoice(price, orderIDParam);
                        invoiceURL = invoice.getUrl();
                        url = bitpay.getBitcoinUrl(invoiceURL);

                       // Log.d("URLBitpay", url);
                        // fetch address for QR Creation text

                        String bitcoinURI = url.substring(url.indexOf("b"), url.indexOf("&")+1);

                        int start = url.indexOf("<a href=\"bitcoin");
                        int end = url.indexOf("?");
                        btcPrice = url.substring((url.indexOf("?")+8), url.indexOf("&"));
                        btcAddress = url.substring((start+9),end);


                    }
                    // if api key is wrong we get a null pointer
                    // go to settings screen in this case
                    catch (NullPointerException e) {
                        apiIsAGo = false;
                        android.content.Intent intentSettings = new Intent(thisClass, SettingsActivity.class);
                        intentSettings.putExtra("apifail?", true);
                        startActivity(intentSettings);
                        finish();
                      //  Log.d("AA", "" + apiIsAGo);
                    }
                } finally {
                    // other threads can now access this info
                    lock.unlock();
                    nfcLock.unlock();
                    refundLock.unlock();
                }

                // provided API key is correct + network is working
                // we create the qr code
                DisplayMessageActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (apiIsAGo) {
                            doQRCreation(btcAddress, btcPrice);
                        }
                    }
                });
            }

        };
        t.setUncaughtExceptionHandler(handle);
        t.start();
        // check to see if invoice is paid
        // checking done on new thread to save rsc + can't be done on ui thread
        checkBitpayInvoice();
    }

    /**
     * Creates a transaction invoice when using a personal / business wallet service
     * @param idMessage
     * @param priceString
     * @param publicAddress
     */
    public void ownWallet(final String idMessage, final String priceString, final String publicAddress) {

        // create the exception handler to catch uncaught exception
        // e.g. null result from api call
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
                // have to use lock b/c need to check for invoice in while loop later
                lock.lock();
                nfcLock.lock();
                try {
                    // check for null api keys and redirect to settings if it's so!!!
                    // else create bitpay invoice and get its url
                    try {
                        // need to get price w/ coindesk
                        btcAddress = publicAddress;
                        String priceUSD = fetchBitcoinPrice();
                        double priceBtc = (Double.parseDouble(priceString)/Double.parseDouble(priceUSD));
                        btcPrice = String.format("%.8f", priceBtc);
                        url = "bitcoin:"+publicAddress+"?amount="+btcPrice;
                       // Log.d("URLBitpay", url);
                    }

                    // go to settings screen in case of failed api call to fetch price
                    catch (NullPointerException e) {
                        apiIsAGo = false;
                        android.content.Intent intentSettings = new Intent(thisClass, SettingsActivity.class);
                        intentSettings.putExtra("apifail?", true);
                        startActivity(intentSettings);
                        finish();
                       // Log.d("AA", "" + apiIsAGo);
                    }

                } finally {
                    // other threads can now access this info
                    lock.unlock();
                    nfcLock.unlock();
                }

                // provided API key is correct + network is working
                // we create the qr code
                DisplayMessageActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (apiIsAGo) {
                            doQRCreation(btcAddress, btcPrice);
                        }
                    }
                });
            }

        };
        t.setUncaughtExceptionHandler(handle);
        t.start();
        // check to see if invoice is paid
        // checking done on new thread to save rsc + can't be done on ui thread
        checkWalletPaid();
    }


    /**
     * Fetches current bitcoin/usd price to calculate prices for people not using a btc payment
     * processing service - this uses Chain Api
     *
     * @return String of the price
     */
    public String fetchBitcoinPrice() {


        ch.boye.httpclientandroidlib.client.methods.HttpRequestBase request;
        request = new ch.boye.httpclientandroidlib.client.methods.HttpGet("https://api.coindesk.com/v1/bpi/currentprice/USD.json");

        // request.setHeader("Content-type", "application/json");

        ch.boye.httpclientandroidlib.client.HttpClient httpClient = HttpClientBuilder.create().build();

        ch.boye.httpclientandroidlib.HttpResponse response = null;

        try {
            response = httpClient.execute(request);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ch.boye.httpclientandroidlib.HttpEntity entity = response.getEntity();

        if (entity != null) {

            // get the response and make it a buffered reader from the input stream from the response entity
            BufferedReader rd = null;
            try {
                rd = new BufferedReader(new InputStreamReader(entity.getContent()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            // make a stringbuilder
            StringBuilder content = new StringBuilder();
            String line;

            // read from the buffered reader and feed it to the stringbuilder
            try {
                while (null != (line = rd.readLine())) {
                    content.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // bitpay forgot to close reader
            try {
                rd.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // make the content a string then parse it and create jsonfinal result
            Object obj = JSONValue.parse(content.toString());
            JSONObject finalResult = (JSONObject) obj;
            Log.d("Result", finalResult.toJSONString());
            JSONObject bpi = (JSONObject) finalResult.get("bpi");
            JSONObject usd = (JSONObject) bpi.get("USD");
            String usdPrice = usd.get("rate_float").toString();
            return usdPrice;
        }

        else {
            return null;
        }
    }

    /**
     * Uses chain api to check if a payment was made to the bitcoin address
     * records the address's unconfirmed balance
     */
    public void checkTxChain() {

        ch.boye.httpclientandroidlib.client.methods.HttpRequestBase request;
        request = new ch.boye.httpclientandroidlib.client.methods.HttpGet("https://api.chain.com/v1/bitcoin/addresses/"+btcAddress);

        String encoding = new String(Base64.encode(("38602a33683645763a2165f8223807f6:c990967a2e822424726467d40aa400c5".getBytes()), Base64.NO_WRAP));

        request.addHeader("Authorization", "Basic " + encoding);

        // request.setHeader("Content-type", "application/json");

        ch.boye.httpclientandroidlib.client.HttpClient httpClient = HttpClientBuilder.create().build();

        ch.boye.httpclientandroidlib.HttpResponse response = null;

        try {
               response = httpClient.execute(request);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ch.boye.httpclientandroidlib.HttpEntity entity = response.getEntity();

        if (entity != null) {

            // get the response and make it a buffered reader from the input stream from the response entity
            BufferedReader rd = null;
            try {
                rd = new BufferedReader(new InputStreamReader(entity.getContent()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            // make a stringbuilder
            StringBuilder content = new StringBuilder();
            String line;

            // read from the buffered reader and feed it to the stringbuilder
            try {
                while (null != (line = rd.readLine())) {
                    content.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // bitpay forgot to close reader
            try {
                rd.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // make the content a string then parse it and create jsonfinal result
            Object obj = JSONValue.parse(content.toString());
            JSONObject finalResult = (JSONObject) obj;
           // Log.d("Result", finalResult.toJSONString());
            balance = (Long) finalResult.get("balance");
            unconfirmed_balance = (Long) finalResult.get("unconfirmed_received");
        }
    }

    /**
     * Creates a coinbase instance + invoice
     * @param idMessage - order id
     * @param idMessage - order id
     * @param coinbaseApiKey
     * @param coinbaseSecret
     * @param priceMessage
     */
    public void coinBasePOS(final String idMessage, final String coinbaseApiKey, final String coinbaseSecret, final String priceMessage) {


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
                // have to use lock b/c need to check for invoice in while loop later
                lock.lock();
                refundLock.lock();
                nfcLock.lock();
                try {


                    // check for null api keys and redirect to settings if it's so!!!
                    // else create coinbase invoice and get its url
                    try {
                        coinBase = new CoinBase(coinbaseApiKey, coinbaseSecret);
                        //Log.d("W", "coinbase created");
                        //Log.d("W", coinbaseApiKey);
                        //Log.d("W", coinbaseSecret);
                        JSONObject invoiceCoinbase = new JSONObject();


                        invoiceCoinbase.put("price_currency_iso", "USD");
                        invoiceCoinbase.put("price_string", priceMessage);
                        invoiceCoinbase.put("type", "buy_now");
                        invoiceCoinbase.put("name", idMessage);
                        invoiceCoinbase.put("custom", idMessage);

                        JSONObject json = new JSONObject();
                        json.put("button", invoiceCoinbase);
                      //  Log.d("INVOICE", json.toString());
                        try {
                          result = coinBase.postHttp("https://coinbase.com/api/v1/orders", json.toString());
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
                     //   Log.d("RESULT", result.toString());
                        JSONObject order = (JSONObject) result.get("order");
                        JSONObject button = (JSONObject) order.get("button");

                     //   Log.d("BUTTON", button.toString());
                        coinBaseID = (String) button.get("id");
                     //    Log.d("COINBASEID", coinBaseID);
                     //    Log.d("ORDER", order.toString());
                        JSONObject totalBtc = (JSONObject) order.get("total_btc");
                        String btcPricePrelim = totalBtc.get("cents").toString();
                        double price = (Double.parseDouble(btcPricePrelim)/100000000);
                        btcPrice = String.format("%.8f", price);
                     //   Log.d("PRICE", btcPrice);
                        btcAddress = (String) order.get("receive_address");
                        url = ("bitcoin:"+btcAddress+"?amount="+btcPrice+"&amp;r=https%3A%2F%2Fcoinbase.com%2Fcheckouts%2F"+coinBaseID);
                        coinbaseOrderID = (String) order.get("id");
                       Log.d("WWWW", coinbaseOrderID+btcPrice);

                    }
                    // if api key is wrong we get a null pointer
                    // go to settings screen in this case
                    catch (NullPointerException e) {
                        apiIsAGo = false;
                        android.content.Intent intentSettings = new Intent(thisClass, SettingsActivity.class);
                        intentSettings.putExtra("apifail?", true);
                        startActivity(intentSettings);

                        finish();
                       // Log.d("AA", "" + apiIsAGo+e);
                        e.printStackTrace();
                    }
                } finally {
                    // other threads can now access this info
                    lock.unlock();
                    nfcLock.unlock();
                    refundLock.unlock();
                }

                // provided API key is correct + network is working
                // we create the qr code
                DisplayMessageActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (apiIsAGo) {
                            doQRCreation(btcAddress, btcPrice);
                            // check to see if invoice is paid
                        }
                    }
                });
            }

        };
        t.setUncaughtExceptionHandler(handle);
        t.start();

            // checking done on new thread to save rsc + can't be done on ui thread
            if (coinbaseOrderID != null) {
                checkCoinbaseInvoice();
            }

            else {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkCoinbaseInvoice();
                    }
                }, 7500);
            }
        }



   /**
     * Creates a goCoin instance + invoice
     * @param idMessage - order id
     * @param goCoinAPIKey
     * @param priceMessage - price
     * @param gocoinMerchantID
     */
    public void goCoinPOS(final String idMessage, final String goCoinAPIKey, final String priceMessage, final String gocoinMerchantID) {

        // create the exception handler to catch uncaught exception from gocoin class
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
                // have to use lock b/c need to check for invoice in while loop later
                lock.lock();
                nfcLock.lock();

                    // check for null api keys and redirect to settings if it's so!!!
                    // else create gocoin invoice and get its url
                    try {
                        goCoinInvoiceService = GoCoin.getInvoiceService();

                        // invoice setup is the invoice we're creating which we pass into the service to create
                        // a real invoice

                        // this is what currency we input the price in
                        goCoinInvoiceSetup.setBasePriceCurrency("USD");
                        // this is the currency we get paid in (btc)
                        goCoinInvoiceSetup.setPriceCurrency("BTC");

                        // set the usd price to whatever we input
                        goCoinInvoiceSetup.setBasePrice(priceMessage);

                        // set order id
                        goCoinInvoiceSetup.setOrderId(idMessage);

                        goCoinToken = new Token(goCoinAPIKey,"","");
                        // create an invoice using token, merchantid, and our invoice params we setup above
                        goCoinInvoiceSetup = goCoinInvoiceService.createInvoice(goCoinToken, gocoinMerchantID, goCoinInvoiceSetup);

                        // retrieve the invoice and get it's url + btc price info below
                        goCoinInvoiceSetup = goCoinInvoiceService.getInvoice(goCoinToken, goCoinInvoiceSetup.getId());

                        btcPrice = goCoinInvoiceSetup.getPrice();

                        // makes a bitcoin url/uri
                        url = ("bitcoin:"+goCoinInvoiceSetup.getPaymentAddress()+"?amount="+goCoinInvoiceSetup.getPrice()+"&amp;r=https%3A%2F%2Fgateway.gocoin.com%2Finvoices%2F"+goCoinInvoiceSetup.getId());


                    }
                    // if api key is wrong we get a null pointer
                    // go to settings screen in this case
                    catch (NullPointerException e) {
                        apiIsAGo = false;
                        android.content.Intent intentSettings = new Intent(thisClass, SettingsActivity.class);
                        intentSettings.putExtra("apifail?", true);
                        startActivity(intentSettings);
                        finish();
                        Log.d("AA", "" + apiIsAGo);
                    }
                finally {
                    // other threads can now access this info
                    lock.unlock();
                    nfcLock.unlock();
                }

                // provided API key is correct + network is working
                // we create the qr code and do sound
                DisplayMessageActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (apiIsAGo) {
                            btcAddress = goCoinInvoiceSetup.getPaymentAddress();
                            doQRCreation(btcAddress, btcPrice);
                        }
                    }
                });
            }

        };
        t.setUncaughtExceptionHandler(handle);
        t.start();

        // check to see if invoice is paid
        // checking done on new thread to save rsc + can't be done on ui thread
        checkGoCoinInvoice();


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.display_message, menu);
        Log.d("K", "options created");
        return true;
    }

    /**
     * Start NFC behavior
     */
    public void startNFC() {

        nfcLock.lock();
        try {
            PackageManager pm = getPackageManager();
            if (pm.hasSystemFeature(PackageManager.FEATURE_NFC) && !url.equals("") && Build.VERSION.SDK_INT>=16 && foreground) {
                mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
                if (mNfcAdapter != null && mNfcAdapter.isEnabled()) {
                    mNfcAdapter.setNdefPushMessageCallback(this, this);
                   // mNfcAdapter.enableForegroundNdefPush(thisClass, this.createNdefMessage());
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.d("Error", "Error doing NFC");
            Context context = getApplicationContext();
            CharSequence text = "Enable NFC + Android beam (not S beam)";
            Toast toast = Toast.makeText(context, text, 2);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
        finally {
            nfcLock.unlock();
        }

    }

    /**
     * Creates nfc message from uri
     *
     * @return msg - nfc message (bitcoin uri)
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        if(Build.VERSION.SDK_INT >=16 && !url.equals("") && foreground) {
            NdefRecord nfcUriRecord = NdefRecord.createUri(url);
            NdefMessage msg = new NdefMessage(nfcUriRecord);
            tapScreen();
            return msg;
        }
        else {
            return null;
        }
    }

    /**
     *  Tap screen automatically to bypass "tap for nfc screen"
     */
    public void tapScreen() {
        DisplayMessageActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Process p = Runtime.getRuntime().exec("su");
                            DataOutputStream outs = new DataOutputStream(p.getOutputStream());

                            // get screen center programmatically
                            Display display = getWindowManager().getDefaultDisplay();
                            Point size = new Point();
                            display.getSize(size);
                            int width = size.x;
                            int height = size.y;
                            int widthHalf = width/2;
                            int heightHalf = height/2;
                            // String cmd="input tap 368 669";
                            String cmd = "input tap "+widthHalf+" "+heightHalf;
                            outs.writeBytes(cmd+"\n");

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, 250);
            }
        });
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        // Log.d("K", "options selected");
        if (id == R.id.action_settings) {
            android.content.Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        if(id==R.id.home) {
            this.onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Creates the qr from bitcoin URI
     * Also begins NFC after QR created (also guarantees activity is in foreground)
     * Doing NFC this way makes sure invoice is created before setting message
     * @param btcAddress
     */
    private void doQRCreation(String btcAddress, String btcPricing) {
        // check if twice incase in between we change orientation
        try {
            if (foreground && isNetworkAvailable()) {
                // display order info on the screen
                TextView orderInfo = (TextView) findViewById(R.id.order_info);
                orderInfo.setText("$" + Double.toString(price) + "\n" + "\n" + "Status: Unpaid" + "\n \n" + "Address: " + btcAddress);

                // create a qr code from the url
                File file = QRCode.from(url).file();
                ImageView image = (ImageView) findViewById(R.id.qrImg);
                android.graphics.
                        Bitmap myQRImg = BitmapFactory.decodeFile(file.getAbsolutePath());
                myQRImg = myQRImg.createScaledBitmap(myQRImg, 500, 500, true);
                image.setImageBitmap(myQRImg);
            }
        }
        catch (Exception e) {
            Log.d("Error", "Exception while creating QR"+e);
            e.printStackTrace();
        }

        // provided we're in the foreground start sound (a few initial plays - the rest occur via accelerometer) & nfc
        if(foreground) {
            startNFC();

            String soundUrl = "b:"+btcAddress+"?"+btcPricing+"&";
            Log.d("Soundurl", soundUrl);
            // start sound too
            if(!url.equals("") && foreground && apiIsAGo && notConfirmed && !soundPlaying) {
                soundPlaying = true;

                sound = new NFSoundActivity(soundUrl, (AudioManager) this.getSystemService(Context.AUDIO_SERVICE));

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        if(sound!=null) {
                            sound.sendString();
                        }

                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                if(sound!=null) {
                                    sound.sendString();
                                }

                                Handler handlerBool = new Handler();
                                handlerBool.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        soundPlaying = false;
                                    }
                                }, 1400); // 5000ms delay

                            }

                        }, 3750); // 5000ms delay

                    }

                }, 1400); // 5000ms delay

            }
        }
    }


    /**
     * Goes to confirmation screen
     */
    private void confirmed() {
        android.content.Intent intent = new Intent(this, DisplayConfirmationActivity.class);
        intent.putExtra("COINBASE_ORDER_ID", coinbaseOrderID);
        startActivity(intent);
    }

    /**
     * returns payment invoice from bitpay
     * @return invoice
     */
    private Invoice getInvoice() {
        if (whichPOS == 1) {
            return bitpay.getInvoice(invoice.getId());
        } else {
            return null;
        }
    }

    /**
     * Check if the bitpay invoice is paid
     */
    public void checkBitpayInvoice() {
        // catch any uncaught exceptions thrown by getting invoice status
        // should be caught by bitpay class
        h = new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread th, Throwable ex) {
                System.out.println("Uncaught exception: " + ex);

                Context context = getApplicationContext();
                CharSequence text = "Network is not available, enable a wifi connection";
                Toast toast = Toast.makeText(context, text, 2);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();

                android.content.Intent intent = new Intent(thisClass, MainActivity.class);
                startActivity(intent);
                finish();
            }
        };

            Thread c = new Thread() {
                @Override
                public void run() {
                    // lock thread so it only accesses invoice after it's created
                    lock.lock();
                    try {
                        if(apiIsAGo) {
                            while (notConfirmed) {
                                Thread.sleep(800);
                                // get invoice and check its payment status
                                String status = getInvoice().getStatus();
                                if (status.equals("paid") || status.equals("confirmed")) {
                                    notConfirmed = false;
                                    // go to confirmed screen
                                    confirmed();
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        // do nothing
                        // pause is intendec
                    }
                    finally {
                        lock.unlock();
                    }
                }

            };
            c.setUncaughtExceptionHandler(h);
            c.start();
        }

    /**
     * Checking if coinbase invoice is paid
     */
    public void checkCoinbaseInvoice() {
        // catch any uncaught exceptions thrown by getting invoice status
        // should be caught by coinbase class
        // this id is one assigned by coinbase
        // Log.d("ID", ""+id);
        h = new Thread.UncaughtExceptionHandler() {
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
                finish();
            }
        };

        Thread c = new Thread() {
            @Override
            public void run() {
                // lock thread so it only accesses invoice after it's created
                lock.lock();
                JSONObject result = null;
                String status = null;
                try {
                    if(apiIsAGo) {
                        while (notConfirmed) {
                            Thread.sleep(800);
                            try {
                                // get the coinbase order
                             result = coinBase.getHttp("https://coinbase.com/api/v1/orders/"+coinbaseOrderID);
                                Log.d("URL", "https://coinbase.com/api/v1/orders/"+coinbaseOrderID);
                            } catch (InvalidKeyException e) {
                                e.printStackTrace();
                            } catch (NoSuchAlgorithmException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            //Log.d("Result", result.toString());
                            if(result!= null) {
                                try {
                                    // get the status of the order
                                    JSONObject orderBlock = (JSONObject) result.get("order");
                                    status = (String) orderBlock.get("status");
                                    //Log.d("status", status);
                                    //Log.d("WHUT", result.toString());
                                }
                                catch (Exception e) {
                                    Log.d("exception", "this means invoice not paid yet"+e);
                                    e.printStackTrace();
                                }
                                if (status!=null && status.equals("completed")) {
                                    notConfirmed = false;
                                    // go to confirmed screen
                                    confirmed();
                                }
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    // do nothing
                    // pause is intendec
                }
                finally {
                    lock.unlock();
                }
            }

        };
        c.setUncaughtExceptionHandler(h);
        c.start();
    }

    /**
     * Check if the gocoin invoice is paid
     */
    public void checkGoCoinInvoice() {
        // catch any uncaught exceptions thrown by getting invoice status
        // should be caught by gocoin class
        h = new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread th, Throwable ex) {
                System.out.println("Uncaught exception: " + ex);

                Context context = getApplicationContext();
                CharSequence text = "Network is not available, enable a wifi connection";
                Toast toast = Toast.makeText(context, text, 2);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();

                android.content.Intent intent = new Intent(thisClass, MainActivity.class);
                startActivity(intent);
                finish();
            }
        };

        Thread c = new Thread() {
            @Override
            public void run() {
                // lock thread so it only accesses invoice after it's created
                lock.lock();
                try {
                    if(apiIsAGo) {
                        while (notConfirmed) {
                            Thread.sleep(800);
                            // get status for gocoin invoice
                            String status = goCoinInvoiceService.getInvoice(goCoinToken, goCoinInvoiceSetup.getId()).getStatus();
                            Log.d("status", status);
                            if (status.equals("paid") || status.equals("confirmed") || status.equals("processing")) {
                                notConfirmed = false;
                                // go to confirmed screen
                                confirmed();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    // do nothing
                    // pause is intended
                }
                finally {
                    lock.unlock();
                }
            }

        };
        c.setUncaughtExceptionHandler(h);
        c.start();
    }

    /**
     * Check if the wallet is paid
     */
    public void checkWalletPaid() {
        // catch any uncaught exceptions thrown by getting invoice status
        // should be caught by gocoin class
        h = new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread th, Throwable ex) {
                System.out.println("Uncaught exception: " + ex);

                Context context = getApplicationContext();
                CharSequence text = "Network is not available, enable a wifi connection";
                Toast toast = Toast.makeText(context, text, 2);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();

                android.content.Intent intent = new Intent(thisClass, MainActivity.class);
                startActivity(intent);
                finish();
            }
        };

        Thread c = new Thread() {
            @Override
            public void run() {
                // lock thread so it only accesses invoice after it's created
                lock.lock();
                try {
                    if(apiIsAGo) {
                        while (notConfirmed) {
                            Thread.sleep(800);
                            // get status for wallet payment
                            checkTxChain();
                            double unconfirmedBtc = ((double)(unconfirmed_balance)/10000000);
                            View content = findViewById(android.R.id.content);


                            content.setOnTouchListener(new View.OnTouchListener() {
                                @Override
                                public boolean onTouch(View view, MotionEvent motionEvent) {
                                    return true;
                                }
                            });



                            double btcPaymentPrice = Double.parseDouble(btcPrice);
                            //Log.d("BTC", ""+unconfirmedBtc+"   "+initialWalletBalanceBtc);

                            // initial wallet balance is the initial unconfirmed balance
                            // see if payment received
                            if ((unconfirmedBtc>=btcPaymentPrice && (unconfirmedBtc-initialWalletBalanceBtc)>=btcPaymentPrice)) {
                                notConfirmed = false;
                                // go to confirmed screen
                                confirmed();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    // do nothing
                    // pause is intended
                }
                finally {
                    lock.unlock();
                }
            }

        };
        c.setUncaughtExceptionHandler(h);
        c.start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        notConfirmed = false;
    }




    @Override
    public void onResume() {
        super.onResume();
        apiIsAGo = true;
        notConfirmed = true;
        foreground = true;

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        if(whichPOS==1 && isNetworkAvailable()) {
            checkBitpayInvoice();
        }

        else if(whichPOS==2 && isNetworkAvailable()) {
            if(coinbaseOrderID==null) {
                // do nothing
            }
            else {
                checkCoinbaseInvoice();
            }
        }

        else if(whichPOS==3 && isNetworkAvailable()) {
            checkGoCoinInvoice();
        }

        else if(whichPOS==4 && isNetworkAvailable()) {
            // check personal wallet invoice :D
            checkWalletPaid();
        }

        else {
                Context context = getApplicationContext();
                CharSequence text = "Network is not available, enable a wifi connection";
                Toast toast = Toast.makeText(context, text, 2);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();

                // go to main screen
                android.content.Intent intent = new Intent(thisClass, MainActivity.class);
                startActivity(intent);
                finish();
        }

        // resume nfc push
        if(!url.equals("") && url!=null && foreground && apiIsAGo && notConfirmed) {
           // mNfcAdapter.enableForegroundNdefPush(thisClass, thisClass.createNdefMessage());
            mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
            mNfcAdapter.setNdefPushMessageCallback(this, this);

            soundPlaying = true;
            String soundUrl = "b:"+btcAddress+"?"+btcPrice+"&";
            sound = new NFSoundActivity(soundUrl, (AudioManager)this.getSystemService(Context.AUDIO_SERVICE));
            Handler handlerBool = new Handler();
            handlerBool.postDelayed(new Runnable() {
                @Override
                public void run() {
                    soundPlaying = false;
                }
            }, 1400); // 5000ms delay

           // connection.resume();
        }
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        }

        // if ble supported then send data over ble
        // can remove nexus 5 line w/ android l official release
        try {
         if(isBLESupported(this)) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ble = new BluetoothUtility(thisClass);

                    ble.setAdvertiseCallback(advertiseCallback);
                    ble.setGattServerCallback(gattServerCallback);

                    addServiceToGattServer();

                    ble.startAdvertise();
                    Log.d("BLE", "started advertising");

                }
            }, 3500);
         }
      }
        catch (NullPointerException e) {
            Log.d("Error", e.toString());
            e.printStackTrace();

            // if fail due to android low level error try one more time
            try {
                if(isBLESupported(this)) {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ble = new BluetoothUtility(thisClass);

                            ble.setAdvertiseCallback(advertiseCallback);
                            ble.setGattServerCallback(gattServerCallback);

                            addServiceToGattServer();

                            ble.startAdvertise();
                            Log.d("BLE", "started advertising");

                        }
                    }, 3500);
                }
            }
            catch (NullPointerException ex) {
                Log.d("Error", ex.toString());
                ex.printStackTrace();
            }

        }
    }

    /**
     * Add the initial base service to the gatt server (required to show up as advertisement)
     */
    private void addServiceToGattServer() {
        serviceOneCharUuid = UUID.randomUUID().toString();


        BluetoothGattService firstService = new BluetoothGattService(
                UUID.fromString(SERVICE_UUID_1),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        // alert level char.
        BluetoothGattCharacteristic firstServiceChar = new BluetoothGattCharacteristic(
                UUID.fromString(serviceOneCharUuid),
                BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ |
                        BluetoothGattCharacteristic.PERMISSION_WRITE);
        firstService.addCharacteristic(firstServiceChar);
        ble.addService(firstService);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onPause() {

        foreground = false;
        sound = null;
        soundPlaying = false;
        //if (connection!=null) {
          //  connection.suspend();
        //}
        mSensorManager.unregisterListener(this);
        if (mNfcAdapter != null) {
            //mNfcAdapter.disableForegroundNdefPush(thisClass);
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        notConfirmed = false;
        apiIsAGo = false;
        foreground = false;
        soundPlaying = false;
        sound = null;
        try {
            ble.cleanUp();
        }
        catch (Exception e) {
            // wasn't created
        }
        super.onStop();
    }

    @Override
    public void onDestroy()
    {
        try {
            ble.cleanUp();
        }
        catch (Exception e) {
            // was already cleaned up in on stop
        }
        super.onDestroy();
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_display_message);
        doQRCreation(btcAddress, btcPrice);
    }

    /**
     * Used to process response from NFC, though there shouldn't really be one
     * except maybe payment sent
     * @param intent
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    void processIntent(Intent intent) {
        textView = (TextView) findViewById(R.id.nfcResponse);
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // textView.setText(new String(msg.getRecords()[0].getPayload()));
    }

    /**
     * Check if a internet connection is available
     * @return true if network is available
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void refundOrder(View view) {
        // bitpay
        if(whichPOS==1) {
            Context context = getApplicationContext();
            CharSequence text = "Bitcoin payment protocol refunds aren't available with Bitpay yet, please ask them about it!";
            Toast toast = Toast.makeText(context, text, 2);
            //toast.setGravity(Gravity.CENTER, 0,5);
            toast.show();
        }

        // coinbase
        else if (whichPOS==2) {
            // Creates an invoice using params passed from mainactivity

            // create the exception handler to catch uncaught exception from bitpay class
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
                    // have to use lock b/c need to check for invoice in while loop later

                    refundLock.lock();

                    try {
                        String mispaymentsID = "";

                        // check for null api keys and redirect to settings if it's so!!!
                        // else create bitpay invoice and get its url
                        try {
                            Log.d("W", "coinbase created");
                            Log.d("W", coinbaseApiKey);
                            Log.d("W", coinbaseSecret);
                            JSONObject refundCoinbase = new JSONObject();


                            refundCoinbase.put("instant_buy", "true");
                            refundCoinbase.put("refund_iso_code", "USD");


                            JSONObject json = new JSONObject();
                            json.put("order", refundCoinbase);

                            Log.d("REFUND", json.toString());

                            try {
                                JSONObject orderResult = coinBase.getHttp("https://coinbase.com/api/v1/orders/"+coinbaseOrderID);
                                Log.d("RESULT", orderResult.toJSONString());
                                Log.d("Result", orderResult.toString());
                                JSONObject order = (JSONObject) orderResult.get("order");
                                org.json.simple.JSONArray mispayments = (org.json.simple.JSONArray) order.get("mispayments");
                                JSONObject misPayObj = (JSONObject) mispayments.get(0);
                                mispaymentsID = (String) misPayObj.get("id");

                                Log.d("WOW", mispayments.toString() + "    " + mispaymentsID);

                                if(mispaymentsID!=null && !mispaymentsID.equals("")) {
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
                                result = coinBase.postHttp("https://coinbase.com/api/v1/orders/"+coinbaseOrderID+"/refund", json.toString());

                               JSONObject orderResult = coinBase.getHttp("https://coinbase.com/api/v1/orders/"+coinbaseOrderID);
                                Log.d("URL", "https://coinbase.com/api/v1/orders/"+coinbaseOrderID);
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
                            if(result.toString().contains("No refund address is present")) {
                                Context context = getApplicationContext();
                                CharSequence text = "Customer wallet doesn't support bip70 (refund protocol) \n manually send a refund to the customer's address \n from your coinbase account";
                                Toast toast = Toast.makeText(context, text, 2);
                                //toast.setGravity(Gravity.CENTER, 0,5);
                                toast.show();

                            }
                            else if(result.toString().contains("fee to be accepted")) {
                                Context context = getApplicationContext();
                                CharSequence text = "Amount is too small for Coinbase auto refund \n manually send a refund to the customer's address \n from your coinbase account";
                                Toast toast = Toast.makeText(context, text, 2);
                                //toast.setGravity(Gravity.CENTER, 0,5);
                                toast.show();

                            }
                            else if(result.toString().contains("true")) {
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
                            apiIsAGo = false;
                            android.content.Intent intentSettings = new Intent(thisClass, SettingsActivity.class);
                            intentSettings.putExtra("apifail?", true);
                            startActivity(intentSettings);

                            finish();
                            Log.d("AA", "" + apiIsAGo+e);
                            e.printStackTrace();
                        }
                    } finally {
                        // other threads can now access this info
                       refundLock.unlock();
                    }


                }

            };
            t.setUncaughtExceptionHandler(handle);
            t.start();
            // checkCoinbaseInvoice();
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

    /**
     * Called on a successful (or failed) bluetooth LE advertising
     */
    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onSuccess(AdvertiseSettings advertiseSettings) {
            String successMsg = "Advertisement command attempt successful";
            Log.d("BLE", successMsg);
        }

        @Override
        public void onFailure(int i) {
            String failMsg = "Advertisement command attempt failed: " + i;
            Log.e("BLE", failMsg);
        }
    };

    /**
     * Once advertising is started, a gatt server is created with this callback
     */
    public BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            Log.d("BLE", "onConnectionStateChange status=" + status + "->" + newState);
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
        }

        // if char read request then set 3 char values, then send the value as a response to the bluetooth device
        // that requested it
        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.d("BLE", "onCharacteristicReadRequest requestId=" + requestId + " offset=" + offset);

           try {
              String bitcoinURI = url.substring(url.indexOf("b"), url.indexOf("&")+1);
               Log.d("BLE", bitcoinURI);

               // first 19 chars
               String partOne = bitcoinURI.substring(bitcoinURI.indexOf("b"), bitcoinURI.indexOf("b")+19);
               // next 19 chars
               String partTwo = bitcoinURI.substring(bitcoinURI.indexOf("b")+19, bitcoinURI.indexOf("b")+38);
               // last set of chars (20)
               String partThree = bitcoinURI.substring(bitcoinURI.indexOf("b")+38, bitcoinURI.indexOf("&")+1);

               // Log.d("WOAH", partOne+partTwo+partThree);

               if (characteristic.getUuid().equals(UUID.fromString("230f04b4-42ff-4ce9-94cb-ed0dc8231957"))) {
                   characteristic.setValue(partOne);
                   ble.getGattServer().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                           characteristic.getValue());
               }
               if (characteristic.getUuid().equals(UUID.fromString("230f04b4-42ff-4ce9-94cb-ed0dc8231958"))) {
                   characteristic.setValue(partTwo);
                   ble.getGattServer().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                           characteristic.getValue());
               }
               if (characteristic.getUuid().equals(UUID.fromString("230f04b4-42ff-4ce9-94cb-ed0dc8231959"))) {
                   characteristic.setValue(partThree);
                   ble.getGattServer().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                           characteristic.getValue());
               }

           }
           catch (Exception e) {
               // uri not created yet
           }
        }

        /**
         * Writes a value to a ble characteristic
         * @param device
         * @param requestId
         * @param characteristic
         * @param preparedWrite
         * @param responseNeeded
         * @param offset
         * @param value
         */
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            Log.d("BLE", "onCharacteristicWriteRequest requestId=" + requestId + " preparedWrite="
                    + Boolean.toString(preparedWrite) + " responseNeeded="
                    + Boolean.toString(responseNeeded) + " offset=" + offset);
        }
    };

}
