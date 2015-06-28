package com.tbg.bitpaypos.app;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import ch.boye.httpclientandroidlib.HttpResponse;

/**
 * @author bitpay
 * @date 11.11.2013
 */
public class Rates {

    private JSONArray rates;
    private BitPay bp;

    /**
     * @param Raw HTTP Response from BitPay api/rates call.
     * @param bp  - used to update self
     */
    public Rates(HttpResponse response, BitPay bp) {
        BufferedReader rd;
        try {
            rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuilder content = new StringBuilder();
            String line;

            while (null != (line = rd.readLine())) {
                content.append(line);
            }

            Object obj = JSONValue.parse(content.toString());
            JSONArray rates = (JSONArray) obj;
            this.rates = rates;
            this.bp = bp;

        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return Bitcoin Exchange rates in a JSONArray.
     */
    public JSONArray getRates() {
        return this.rates;
    }

    /**
     * Updates the rates from the BitPay api.
     */
    public void update() {
        this.rates = this.bp.getRates().getRates();
    }

    /**
     * Returns the Bitcoin exchange rate for the given currency code.
     * Ensure that the currency code is valid, and in ALL CAPS.
     *
     * @param 3 letter currency code in all caps.
     * @return String of the exchange rate.
     */
    public double getRate(String currencyCode) {
        double val = 0;
        for (Object rate : this.rates) {
            JSONObject obj = (JSONObject) rate;
            if (obj.get("code").equals(currencyCode)) {
                val = (Double) obj.get("rate");
            }
        }
        return val;
    }

}
