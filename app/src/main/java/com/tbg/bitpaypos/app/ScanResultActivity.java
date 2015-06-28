package com.tbg.bitpaypos.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import io.triangle.Session;
import io.triangle.reader.PaymentCard;
import io.triangle.reader.ScanActivity;

import com.stripe.*;
import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Charge;
import com.tbg.bitpaypos.app.R;


import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class demonstrates how scanning payment cards via NFC is done via the Triangle APIs.
 */
public class ScanResultActivity extends Activity implements View.OnClickListener
{
    private LinearLayout root;
    private TextView caption;

    // Buttons
    private ImageButton facebookButton;
    private ImageButton twitterButton;
    private ImageButton shareButton;
    private ImageButton linkedInButton;
    private ImageButton googlePlusButton;

    private String ID;
    private String price;
    private Application thisApp = this.getApplication();


    private static final int SCAN_REQUEST_CODE = 100;

    /**
     * Tracks whether this activity has already requested a card scan.
     */
    private boolean hasRequestedScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.scan_result);

        android.content.Intent intent = getIntent();

        ID = intent.getStringExtra("com.tbg.bitpaypos.app.ID");
        price = intent.getStringExtra("com.tbg.bitpaypos.app.PRICE");

        this.root = (LinearLayout) this.findViewById(R.id.main_LinearLayout_root);
        this.caption = (TextView) this.findViewById(R.id.header_textView_caption);


        final Application thisApp = this.getApplication();

        // Link the text to our website in the caption
        Linkify.addLinks(this.caption, Linkify.WEB_URLS);

        // Initialize the Triangle API if it has not been initialized yet
        final Session triangleSession = Session.getInstance();

        if (!triangleSession.isInitialized())
        {
            // TODO: You need to obtain keys from http://www.triangle.io to be able to run the application
            final String applicationId = "qkarsGMcxeSUUiz";
            final String accessKey = "k65tl75IWP";
            final String secretKey = "OrjlWStYp1DJYBGKWszVytUXiY05kHIoQeLPFlvEOzW0FA8uMAbD91gkwm3YSoEI" ;

            // need keys in case they have not added them here
            if (applicationId.equals("TODO"))
            {
                Toast.makeText(this, "You need to obtain keys from triangle.io before running the sample application", Toast.LENGTH_LONG).show();
            }

            // Since the initialization performs network IO, we should execute it in a background thread
            AsyncTask<Void, Void, Void> triangleInitializationTask = new AsyncTask<Void, Void, Void>()
            {
                Exception exception;

                @Override
                protected Void doInBackground(Void... voids)
                {
                    try
                    {
                        triangleSession.initialize(
                                applicationId, // Application ID
                                accessKey,      // Access Key
                                secretKey, // Secret Key
                                thisApp);
                    }
                    catch (Exception exception)
                    {
                        this.exception = exception;
                    }

                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid)
                {
                    super.onPostExecute(aVoid);

                    if (this.exception != null)
                    {
                        // TODO: Do error handling if initialization was not successful
                        Toast.makeText(thisApp, this.exception.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            };

            // Finally execute the task
            triangleInitializationTask.execute();
        }
    }


    @Override
    protected void onResume()
    {
        super.onResume();

        // Ensure that the device's NFC sensor is on
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        boolean askingToEnableNfc = false;

        if (nfcAdapter != null && !nfcAdapter.isEnabled())
        {
            askingToEnableNfc = true;

            // Alert the user that NFC is off
            new AlertDialog.Builder(this)
                    .setTitle("NFC Sensor Turned Off")
                    .setMessage("In order to use this application, the NFC sensor must be turned on. Do you wish to turn it on?")
                    .setPositiveButton("Go to Settings", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            // Send the user to the settings page and hope they turn it on
                            if (android.os.Build.VERSION.SDK_INT >= 16)
                            {
                                startActivity(new Intent(android.provider.Settings.ACTION_NFC_SETTINGS));
                            }
                            else
                            {
                                startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                            }
                        }
                    })
                    .setNegativeButton("Do Nothing", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            // Do nothing
                        }
                    })
                    .show();
        }

        if (!askingToEnableNfc && !this.hasRequestedScan)
        {
            // If no cards have been scanned so far, then automatically kick off a scan
            // this.scanCard();
        }
    }

    private void onScanResult(PaymentCard cardInformation, List<String> errors)
    {
        // NOTE: The errors list would contain any errors the scanning may have yielded

        if (cardInformation != null)
        {
            // Remove any previous cards
            this.root.removeAllViews();

            CardView cardView = new CardView(this.root, cardInformation, this);
            LinearLayout.LayoutParams cardViewLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardViewLayoutParams.gravity = Gravity.CENTER_HORIZONTAL;
            this.root.addView(cardView, 0, cardViewLayoutParams);
        }

        RSA rsaClass = null;
        try {
            rsaClass = new RSA("rQIvuFDkyqRw78yASZY7kCMpQnWJLIhsFfBHxSZAK5ckBvH9SMATfQFnss9JWuwuqGkNXC/oZSqczi+1c6cHELyQM7liO6JSGYXMJtr8pS/h+vYzho2rbDm9MUeLSKy9MaWtmNKo9HSgLSl85Ju41QOEmUjyybeKJ/AWm8YcFE47jwxyivLvbLc9idapRMlnWMWPEb1J5bFUh0/OrcuJ2OQf3rqexSJu6dgE7FNV+c1l5SF4CZGQEmbNH5+isYD2VmbXDTxcycVrBZ62JmNkeD9vvCrF/vgAcaHs5Gx5+G4MiNqYtSWeiAbfruzCtHy5BY91pndi5gEGZ3YVTf1daw==","WIED1HBdemTA+YtOHVbjRYsXMk5aTBPF5zsyG+LDdQkufvcQMUVBMvOjDtAHoKGuBK0pDn3bjtVLvhad5noNnTw5MJynagZRpYjStRXVpNNn8TA9j5mtlgG7jRgiYp0rc9hjhAhQMi3vOus8Xt5ioWXZUWkF+rcPz/p8NlgUSTYcqY5kz5u3n2yft3noqtJe/hclruru70BSU9nQZA8+0PzsNoSMp33j3xRT3GpRXVTbMSVTeWiKcpUeZs/XsQovVE2IkMWOF9QZuVt+nxbgLtEZ92A/PiaPyWeFf2K6Uw8ttROB9wqMknCQxgLcN2TAKRAB2DjJOvN/N9mcdlqMMQ==");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }

        if(rsaClass!=null) {
            try {
                String ccNum = rsaClass.decrypt(cardInformation.getEncryptedAccountNumber());
                Log.d("NUM", ccNum);
                int month = (cardInformation.getExpiryDate().getMonth() + 1);
                Log.d("NUM", cardInformation.getExpiryDate().toString());
                int year = (cardInformation.getExpiryDate().getYear() + 1900);

                // To do google wallet you need do get encrypted tracks
                // Log.d("What", rsaClass.decrypt(cardInformation.getEncryptedTrack1()));

                beginCharge(ccNum, month, year, "");

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }



    public void onScanButtonClick(final View view)
    {
        this.scanCard();
    }

    private void scanCard()
    {
        Intent scanIntent = new Intent(this, io.triangle.reader.ScanActivity.class);

        // We want the scanning to continue until a successful scan occurs or
        // the user explicitly cancels
        scanIntent.putExtra(ScanActivity.INTENT_EXTRA_RETRY_ON_ERROR, true);

        // Kick off the scan activity
        this.startActivityForResult(scanIntent, SCAN_REQUEST_CODE);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == SCAN_REQUEST_CODE)
        {
            // Track that this activity has already asked for a scan
            this.hasRequestedScan = true;

            if (resultCode == RESULT_OK)
            {
                PaymentCard scannedCard = data.getParcelableExtra(ScanActivity.INTENT_EXTRA_PAYMENT_CARD);
                List<String> errors = data.getStringArrayListExtra(ScanActivity.INTENT_EXTRA_SCAN_ERRORS);

                // Handle the scan result
                this.onScanResult(scannedCard, errors);
            }
            else if (resultCode == ScanActivity.RESULT_NO_NFC)
            {
                // This device does not have an NFC sensor
                new AlertDialog.Builder(this)
                        .setTitle("Device has no NFC Sensor")
                        .setMessage("In order to scan a payment card, you must have a device with an NFC sensor.")
                        .setPositiveButton("OK", null)
                        .create()
                        .show();
            }
            else if (resultCode == ScanActivity.RESULT_CANCELED)
            {
                // The scanning was cancelled by the user
            }
        }
        else
        {
            // Let the parent handle this, we don't know what it is
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onClick(View view)
    {
        // The only buttons that call this method are the share buttons
        // Perform the actual recommendation
       // this.recommend();
    }


    public void submitPayment(View view) {

        try {
            EditText ccNum = (EditText) findViewById(R.id.editCardNum);
            String ccNumS = ccNum.getText().toString();

            EditText ccCVC = (EditText) findViewById(R.id.CVC);
            String ccCVCS = ccCVC.getText().toString();

            EditText ccMonth = (EditText) findViewById(R.id.cardMonth);
            String ccMonthS = ccMonth.getText().toString();
            int cardExpMonth = Integer.parseInt(ccMonthS);

            EditText ccYear = (EditText) findViewById(R.id.cardYear);
            String ccYearS = ccYear.getText().toString();
            int cardExpYear = Integer.parseInt(ccYearS);

            beginCharge(ccNumS, cardExpMonth, cardExpYear, ccCVCS);
        }
        catch (Exception e) {
            // tell user they didn't input somehting
            e.printStackTrace();
        }


    }


    public void beginCharge(String ccNumS, int cardExpMonth, int cardExpYear, String ccCVCS) {


        Card card = new Card(ccNumS, cardExpMonth, cardExpYear, ccCVCS);
        // Log.d("CVC", card.getCVC());
        Log.d("NUM", "created charge");
        Log.d("NUM", ccNumS+cardExpMonth+cardExpYear);
        if (!card.validateCard()) {
        //     something went wrong, whoops! add some error messages for this messup
       }

        // do the tx
        else {
            try {
                Stripe stripe = new Stripe("pk_live_4PNdyGocmLzT1SmorlZyGd69");
                stripe.createToken(card, new TokenCallback() {
                    @Override
                    public void onError(Exception error) {
                        // else we have an error
                        Log.d("HUH", error.toString());

                    }

                    @Override
                    public void onSuccess(final Token token) {
                        // do charge with token
                        Log.d("NUM", "got a token");
                        Thread t = new Thread() {
                            @Override
                            public void run() {
                                chargeClient(price, ID, "USD", token);
                            }
                        };
                        t.start();
                    }
                });
            } catch (AuthenticationException e) {
                e.printStackTrace();
            }
        }
    }


    public void chargeClient(String price, String orderID, String currency, Token token) {
        // set secret key
        com.stripe.Stripe.apiKey = "sk_live_4PNdG4c5NVMQW75IomzMliL6";

        Map<String, Object> chargeParams = new HashMap<String, Object>();

        chargeParams.put("amount", (int) (Double.parseDouble(price)*100));
        chargeParams.put("currency", currency);
        // pass token or id?
        chargeParams.put("card", token.getId());
        // chargeParams.put("description", orderID);
        Log.d("NUM", "charge params put");


        try {
            Charge.create(chargeParams, "sk_live_4PNdG4c5NVMQW75IomzMliL6");
            Log.d("Charge", "Successful");
            android.content.Intent intent = new Intent(this, DisplayConfirmationActivity.class);
            startActivity(intent);
            finish();
        } catch (AuthenticationException e) {
            e.printStackTrace();
        } catch (InvalidRequestException e) {
            e.printStackTrace();
        } catch (APIConnectionException e) {
            e.printStackTrace();
        } catch (CardException e) {
            e.printStackTrace();
        } catch (APIException e) {
            e.printStackTrace();
        }
    }
}
