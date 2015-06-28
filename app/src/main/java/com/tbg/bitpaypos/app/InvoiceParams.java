package com.tbg.bitpaypos.app;

import java.util.ArrayList;

import ch.boye.httpclientandroidlib.message.BasicNameValuePair;

/**
 * @author Chaz Ferguson
 * @date 11.11.2013
 */
public class InvoiceParams {

    private String posData;
    private String notificationURL;
    private String transactionSpeed;
    private boolean fullNotifications;
    private String notificationEmail;
    private String redirectURL;
    private String orderId;
    private String itemDesc;
    private String itemCode;
    private boolean physical;

    private String buyerName;
    private String buyerAddress1;
    private String buyerAddress2;
    private String buyerCity;
    private String buyerState;
    private String buyerZip;
    private String buyerCountry;
    private String buyerEmail;
    private String buyerPhone;

    public InvoiceParams() {
        this.physical = false;
        this.fullNotifications = false;
    }

    /**
     * A passthru variable provided by the merchant and designed to be used by
     * the merchant to correlate the invoice with an order or other object in
     * their system.
     * <p/>
     * This passthru variable can be a JSON-encoded string, for example
     * <p/>
     * posData: ‘ { “ref” : 711454, “affiliate” : “spring112” } ‘
     */
    public String getPosData() {
        return posData;
    }

    /**
     * A passthru variable provided by the merchant and designed to be used by
     * the merchant to correlate the invoice with an order or other object in
     * their system.
     * <p/>
     * This passthru variable can be a JSON-encoded string, for example
     * <p/>
     * posData: ‘ { “ref” : 711454, “affiliate” : “spring112” } ‘
     */
    public void setPosData(String posData) {
        this.posData = posData;
    }

    /**
     * A URL to send status update messages to your server (this must be an
     * https URL, unencrypted http URLs or any other type of URL is not
     * supported).
     * <p/>
     * Bitpay.com will send a POST request with a JSON encoding of the invoice
     * to this URL when the invoice status changes
     */
    public String getNotificationURL() {
        return notificationURL;
    }

    /**
     * A URL to send status update messages to your server (this must be an
     * https URL, unencrypted http URLs or any other type of URL is not
     * supported).
     * <p/>
     * Bitpay.com will send a POST request with a JSON encoding of the invoice
     * to this URL when the invoice status changes
     */
    public void setNotificationURL(String notificationURL) {
        this.notificationURL = notificationURL;
    }

    /**
     * default value: set in your https://bitpay.com/order-settings
     * <p/>
     * “high” : An invoice is considered to be "confirmed" immediately upon
     * receipt of payment.
     * <p/>
     * “medium” : An invoice is considered to be "confirmed" after 1 block
     * confirmation (~10 minutes).
     * <p/>
     * “low” : An invoice is considered to be "confirmed" after 6 block
     * confirmations (~1 hour).
     * <p/>
     * NOTE: Orders are posted to
     */
    public String getTransactionSpeed() {
        return transactionSpeed;
    }

    /**
     * default value: set in your https://bitpay.com/order-settings
     * <p/>
     * “high” : An invoice is considered to be "confirmed" immediately upon
     * receipt of payment.
     * <p/>
     * “medium” : An invoice is considered to be "confirmed" after 1 block
     * confirmation (~10 minutes).
     * <p/>
     * “low” : An invoice is considered to be "confirmed" after 6 block
     * confirmations (~1 hour).
     * <p/>
     * NOTE: Orders are posted to
     */
    public void setTransactionSpeed(String transactionSpeed) {
        this.transactionSpeed = transactionSpeed;
    }

    /**
     * Default: false
     * <p/>
     * true: Notifications will be sent on every status change.
     * <p/>
     * false: Notifications are only sent when an invoice is confirmed
     * (according the the transactionSpeed setting).
     */
    public boolean isFullNotifications() {
        return fullNotifications;
    }

    /**
     * Default: false
     * <p/>
     * true: Notifications will be sent on every status change.
     * <p/>
     * false: Notifications are only sent when an invoice is confirmed
     * (according the the transactionSpeed setting).
     */
    public void setFullNotifications(boolean fullNotifications) {
        this.fullNotifications = fullNotifications;
    }

    /**
     * Bitpay.com will send an email to this email address when the invoice
     * status changes.
     */
    public String getNotificationEmail() {
        return notificationEmail;
    }

    /**
     * Bitpay.com will send an email to this email address when the invoice
     * status changes.
     *
     * @param notificationEmail
     */
    public void setNotificationEmail(String notificationEmail) {
        this.notificationEmail = notificationEmail;
    }

    /**
     * This is the URL for a return link that is displayed on the receipt, to
     * return the shopper back to your website after a successful purchase. This
     * could be a page specific to the order, or to their account.
     */
    public String getRedirectURL() {
        return redirectURL;
    }

    /**
     * This is the URL for a return link that is displayed on the receipt, to
     * return the shopper back to your website after a successful purchase. This
     * could be a page specific to the order, or to their account.
     */
    public void setRedirectURL(String redirectURL) {
        this.redirectURL = redirectURL;
    }

    /**
     * Used to display your public order number to the buyer on the BitPay
     * invoice. In the merchant Account Summary page, this value is used to
     * identify the ledger entry.
     */
    public String getOrderId() {
        return orderId;
    }

    /**
     * Used to display your public order number to the buyer on the BitPay
     * invoice. In the merchant Account Summary page, this value is used to
     * identify the ledger entry.
     */
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    /**
     * Used to display an item description to the buyer
     *
     * @return
     */
    public String getItemDesc() {
        return itemDesc;
    }

    /**
     * Used to display an item description to the buyer
     */
    public void setItemDesc(String itemDesc) {
        this.itemDesc = itemDesc;
    }

    /**
     * Used to display an item SKU code or part number to the buyer
     */
    public String getItemCode() {
        return itemCode;
    }

    /**
     * Used to display an item SKU code or part number to the buyer
     */
    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    /**
     * default value: false
     * <p/>
     * true : Indicates a physical item will be shipped (or picked up)
     * <p/>
     * false : Indicates that nothing is to be shipped for this order
     */
    public boolean isPhysical() {
        return physical;
    }

    /**
     * default value: false
     * <p/>
     * true : Indicates a physical item will be shipped (or picked up)
     * <p/>
     * false : Indicates that nothing is to be shipped for this order
     */
    public void setPhysical(boolean physical) {
        this.physical = physical;
    }

    /**
     * used for display purposes only and will be shown on the invoice if
     * provided.
     */
    public String getBuyerName() {
        return buyerName;
    }

    /**
     * used for display purposes only and will be shown on the invoice if
     * provided.
     */
    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    /**
     * used for display purposes only and will be shown on the invoice if
     * provided.
     */
    public String getBuyerAddress1() {
        return buyerAddress1;
    }

    /**
     * used for display purposes only and will be shown on the invoice if
     * provided.
     */
    public void setBuyerAddress1(String buyerAddress1) {
        this.buyerAddress1 = buyerAddress1;
    }

    /**
     * used for display purposes only and will be shown on the invoice if
     * provided.
     */
    public String getBuyerAddress2() {
        return buyerAddress2;
    }

    /**
     * used for display purposes only and will be shown on the invoice if
     * provided.
     */
    public void setBuyerAddress2(String buyerAddress2) {
        this.buyerAddress2 = buyerAddress2;
    }

    /**
     * used for display purposes only and will be shown on the invoice if
     * provided.
     */
    public String getBuyerCity() {
        return buyerCity;
    }

    /**
     * used for display purposes only and will be shown on the invoice if
     * provided.
     */
    public void setBuyerCity(String buyerCity) {
        this.buyerCity = buyerCity;
    }

    /**
     * used for display purposes only and will be shown on the invoice if
     * provided.
     */
    public String getBuyerState() {
        return buyerState;
    }

    /**
     * used for display purposes only and will be shown on the invoice if
     * provided.
     */
    public void setBuyerState(String buyerState) {
        this.buyerState = buyerState;
    }

    /**
     * used for display purposes only and will be shown on the invoice if
     * provided.
     */
    public String getBuyerZip() {
        return buyerZip;
    }

    /**
     * used for display purposes only and will be shown on the invoice if
     * provided.
     */
    public void setBuyerZip(String buyerZip) {
        this.buyerZip = buyerZip;
    }

    /**
     * used for display purposes only and will be shown on the invoice if
     * provided.
     */
    public String getBuyerCountry() {
        return buyerCountry;
    }

    /**
     * used for display purposes only and will be shown on the invoice if
     * provided.
     */
    public void setBuyerCountry(String buyerCountry) {
        this.buyerCountry = buyerCountry;
    }

    /**
     * used for display purposes only and will be shown on the invoice if
     * provided.
     */
    public String getBuyerEmail() {
        return buyerEmail;
    }

    /**
     * used for display purposes only and will be shown on the invoice if
     * provided.
     */
    public void setBuyerEmail(String buyerEmail) {
        this.buyerEmail = buyerEmail;
    }

    /**
     * used for display purposes only and will be shown on the invoice if
     * provided.
     */
    public String getBuyerPhone() {
        return buyerPhone;
    }

    /**
     * used for display purposes only and will be shown on the invoice if
     * provided.
     */
    public void setBuyerPhone(String buyerPhone) {
        this.buyerPhone = buyerPhone;
    }

    /**
     * Get all the set invoice parameters
     *
     * @return ArrayList of BasicNameValuePairs.
     */
    public ArrayList<BasicNameValuePair> getNameValuePairs() {
        ArrayList<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();

        params.add(new BasicNameValuePair("physical", String.valueOf(this.physical)));
        params.add(new BasicNameValuePair("fullNotifications", String.valueOf(this.fullNotifications)));

        if (this.notificationURL != null) {
            params.add(new BasicNameValuePair("notificationURL", this.notificationURL));
        }
        if (this.transactionSpeed != null) {
            params.add(new BasicNameValuePair("transactionSpeed", this.transactionSpeed));
        }
        if (this.posData != null) {
            params.add(new BasicNameValuePair("posData", this.posData));
        }
        if (this.notificationEmail != null) {
            params.add(new BasicNameValuePair("notificationEmail", this.notificationEmail));
        }
        if (this.redirectURL != null) {
            params.add(new BasicNameValuePair("redirectURL", this.redirectURL));
        }
        if (this.orderId != null) {
            params.add(new BasicNameValuePair("orderID", this.orderId));
        }
        if (this.itemDesc != null) {
            params.add(new BasicNameValuePair("itemDesc", this.itemDesc));
        }
        if (this.itemCode != null) {
            params.add(new BasicNameValuePair("itemCode", this.itemCode));
        }
        if (this.buyerName != null) {
            params.add(new BasicNameValuePair("buyerName", this.buyerName));
        }
        if (this.buyerAddress1 != null) {
            params.add(new BasicNameValuePair("buyerAddress1", this.buyerAddress1));
        }
        if (this.buyerAddress2 != null) {
            params.add(new BasicNameValuePair("buyerAddress2", this.buyerAddress2));
        }
        if (this.buyerCity != null) {
            params.add(new BasicNameValuePair("buyerCity", this.buyerCity));
        }
        if (this.buyerState != null) {
            params.add(new BasicNameValuePair("buyerState", this.buyerState));
        }
        if (this.buyerZip != null) {
            params.add(new BasicNameValuePair("buyerZip", this.buyerZip));
        }
        if (this.buyerCountry != null) {
            params.add(new BasicNameValuePair("buyerCountry", this.buyerCountry));
        }
        if (this.buyerEmail != null) {
            params.add(new BasicNameValuePair("buyerEmail", this.buyerEmail));
        }
        if (this.buyerPhone != null) {
            params.add(new BasicNameValuePair("buyerPhone", this.buyerPhone));
        }
        return params;
    }

}
