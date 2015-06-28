package com.tbg.bitpaypos.app;

import android.content.Intent;
import android.util.Base64;
import android.util.Log;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLException;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.NameValuePair;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.impl.client.HttpClientBuilder;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;


/**
 * @author Chaz Ferguson
 * Ported + Error checking + Documentation added by Joseph Krug
 * + Bip72 url parsing added by Joseph
 * @date 6.4.14
 */
public class BitPay {

    private static final String BASE_URL = "https://bitpay.com/api/";

    private String apiKey;
    private HttpClient client;
    String auth;

    private String currency;

    /**
     * Constructor.
     *
     * @param apiKey   Generated at BitPay.com. Merchant account required.
     * @param currency default currency code
     */
    public BitPay(String apiKey, String currency) {
        this.apiKey = apiKey;
        this.currency = currency;
        this.auth = new String(Base64.encode((this.apiKey + ": ").getBytes(), Base64.NO_WRAP));
        // create an HttpClient
        client = HttpClientBuilder.create().build();
    }

    /**
     * Creates an invoice using the BitPay Payment Gateway API
     *
     * @param price - set in this.currency
     * @return Invoice
     */
    public Invoice createInvoice(double price) {

        String url = BASE_URL + "invoice";

        try {
            // make a new httpPost
            HttpPost post = new HttpPost(url);

            // header for the post includes apikey
            post.addHeader("Authorization", "Basic " + this.auth);
            // sets the entity for the post request, basically price + currency
            post.setEntity(new UrlEncodedFormEntity(this.getParams(price, this.currency), "UTF-8"));

            // execute the post & post it to the bitpay server using our client
            // then store whatever the server responds in response
            HttpResponse response = this.client.execute(post);

            // server responds with some junk, gotta encode as json so
            // call this to create invoice from the response
            return createInvoiceObjectFromResponse(response);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (SSLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Creates an invoice using the BitPay Payment Gateway API
     * This is same as method createInvoice but adds params in the post request
     *
     * @param price  - set in this.currency
     * @param params - optional invoice parameters
     * @return Invoice
     */
    public Invoice createInvoice(double price, InvoiceParams params) {


        String url = BASE_URL + "invoice";

        try {
            HttpPost post = new HttpPost(url);

            post.addHeader("Authorization", "Basic " + this.auth);
            post.setEntity(new UrlEncodedFormEntity(this.getParams(price, this.currency, params), "UTF-8"));

            HttpResponse response = this.client.execute(post);

            return createInvoiceObjectFromResponse(response);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Get an existing Invoice by it's ID. The ID is used in the url:
     * "http://bitpay.com/invoice?id=<ID>"
     *
     * @param invoiceId
     * @return Invoice
     */
    public Invoice getInvoice(String invoiceId) {
        String url = BASE_URL + "invoice/" + invoiceId;

        // create an httpget request
        HttpGet get = new HttpGet(url);

        get.addHeader("Authorization", "Basic " + this.auth);

        try {
            //has our httpclient execute a get request
            HttpResponse response = client.execute(get);

            // creates an invoice from this get request
            return createInvoiceObjectFromResponse(response);

        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    // gets the bip72 uri that every client uses
    public String getBitcoinUrl(String invoiceURL) {
        String url = invoiceURL;
        HttpGet get = new HttpGet(url);
        get.setHeader("Accept", "text/uri-list");
        try {
            HttpResponse response = client.execute(get);
            // get the response and make it a buffered reader from the input stream from the respons
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            // make a stringbuilder
            StringBuilder content = new StringBuilder();
            String line;

            // read from the buffered reader and feed it to the stringbuilder
            while (null != (line = rd.readLine())) {
                content.append(line);
            }

            // bitpay forgot to close reader
            rd.close();

           String reply = content.toString();
           int start = reply.indexOf("<a href=\"bitcoin");
           int end = reply.indexOf("\" class=\"pay\"");
           reply = reply.substring((start+9),end);
           return reply;

        }
          catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get the current Bitcoin Exchange rates in dozens of currencies based on several exchanges.
     *
     * @return Rates object.
     */
    public Rates getRates() {
        String url = BASE_URL + "rates";

        HttpGet get = new HttpGet(url);

        get.addHeader("Authorization", "Basic " + this.auth);

        try {
            HttpResponse response = client.execute(get);

            return new Rates(response, this);

        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    // puts params in a namevaluepair list to send to server
    private List<NameValuePair> getParams(double price,
                                          String currency) {
        List<NameValuePair> params = new ArrayList<NameValuePair>(2);
        params.add(new BasicNameValuePair("price", price + ""));
        params.add(new BasicNameValuePair("currency", currency));
        return params;
    }

    // same as above w/ addn. params
    private List<NameValuePair> getParams(double price,
                                          String currency, InvoiceParams optionalParams) {
        List<NameValuePair> params = new ArrayList<NameValuePair>(2);
        // get extra params from invoiceparams object and add them to list as well
        for (BasicNameValuePair param : optionalParams.getNameValuePairs()) {
            params.add(param);
        }
        params.add(new BasicNameValuePair("price", price + ""));
        params.add(new BasicNameValuePair("currency", currency));
        return params;
    }

    // creates an invoice object from the server's response
    private Invoice createInvoiceObjectFromResponse(HttpResponse response) throws JSONException, IOException {
        // get the response and make it a buffered reader from the input stream from the respons
        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

        // make a stringbuilder
        StringBuilder content = new StringBuilder();
        String line;

        // read from the buffered reader and feed it to the stringbuilder
        while (null != (line = rd.readLine())) {
            content.append(line);
        }

        // bitpay forgot to close reader
        rd.close();

        // make the content a string then parse it and create jsonfinal result
        Object obj = JSONValue.parse(content.toString());
        JSONObject finalResult = (JSONObject) obj;

        if (finalResult.get("error") != null) {
            System.out.println("Error: " + finalResult.get("error"));
        }
        // return a new invoice from this json result
        // since it's a json result we can call .get and get
        // one of the values of the invoice/result using a key, e.g. "price"
        return new Invoice(finalResult);
    }
}
