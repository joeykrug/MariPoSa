/*
 * Copyright (c) 2014. Joseph Krug & TBG/Simplybit
 */

package com.tbg.bitpaypos.app;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.NameValuePair;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.entity.StringEntity;
import ch.boye.httpclientandroidlib.impl.client.HttpClientBuilder;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;
import ch.boye.httpclientandroidlib.util.EntityUtils;
import org.apache.commons.codec.binary.Hex;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Created by joeykrug on 7/1/2014.
 */

public class CoinBase {

     private String apiKey = "API_KEY";

     private String apiSecret = "API_SECRET";


     public CoinBase(String API_KEY, String API_SECRET) {
         this.apiKey = API_KEY;
         this.apiSecret = API_SECRET;
     }


    public JSONObject postHttp(String url, String body) throws InvalidKeyException, NoSuchAlgorithmException, IOException {

        String nonce = String.valueOf(System.currentTimeMillis());
        String message = nonce + url + (body != null ? body : "");

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(apiSecret.getBytes(), "HmacSHA256"));
        String signature = new String(Hex.encodeHex(mac.doFinal(message.getBytes())));

        HttpRequestBase request;
        //if (body == null || body.length() == 0)
        //  request = new HttpGet(url);

        HttpPost post = new HttpPost(url);
        post.setEntity(new StringEntity(body));
        request = post;

        request.setHeader("Content-type", "application/json");
        request.setHeader("ACCESS_KEY", apiKey);
        request.setHeader("ACCESS_SIGNATURE", signature);
        request.setHeader("ACCESS_NONCE", nonce);



        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpResponse response = httpClient.execute(request);

        HttpEntity entity = response.getEntity();
        if (entity != null) {

            // get the response and make it a buffered reader from the input stream from the response entity
            BufferedReader rd = new BufferedReader(new InputStreamReader(entity.getContent()));

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
            return finalResult;
        }
        else {
            return null;
        }
    }

    public JSONObject getHttp(String url) throws InvalidKeyException, NoSuchAlgorithmException, IOException {
        String nonce = String.valueOf(System.currentTimeMillis());
        String message = nonce + url + "";

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(apiSecret.getBytes(), "HmacSHA256"));
        String signature = new String(Hex.encodeHex(mac.doFinal(message.getBytes())));

        HttpRequestBase request;
        request = new HttpGet(url);

        request.setHeader("Content-type", "application/json");
        request.setHeader("ACCESS_KEY", apiKey);
        request.setHeader("ACCESS_SIGNATURE", signature);
        request.setHeader("ACCESS_NONCE", nonce);

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpResponse response = httpClient.execute(request);

        HttpEntity entity = response.getEntity();

        if (entity != null) {

            // get the response and make it a buffered reader from the input stream from the response entity
            BufferedReader rd = new BufferedReader(new InputStreamReader(entity.getContent()));

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
            return finalResult;
        }

        else {
            return null;
        }
    }
}