/*
 * Copyright (c) 2014. Joseph Krug & TBG/Simplybit
 */

package com.tbg.bitpaypos.app;

/**
 * Created by joeykrug on 7/16/2014.
 */
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import android.util.Base64;

import javax.crypto.Cipher;

public class RSA  {

    public static final String RSAKeyFactory = "RSA";
    public static final String RSAKeyAlgorithm = "RSA/ECB/PKCS1Padding";
    public static final String UTF_8 = "UTF-8";
    public static final int BASE_64_FLAG = Base64.NO_WRAP;

    private PrivateKey privateKey;

    public RSA(String modulus, String d) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeySpecException
    {
        KeyFactory keyFactory = KeyFactory.getInstance(RSAKeyFactory);
        privateKey = keyFactory.generatePrivate(new RSAPrivateKeySpec(
                new BigInteger(1, Base64.decode(modulus, BASE_64_FLAG)),
                new BigInteger(1, Base64.decode(d, BASE_64_FLAG))));
    }

    public String decrypt(String cipherText) throws Exception
    {
        byte[] cipherBytes = Base64.decode(cipherText, BASE_64_FLAG);
        Cipher cipher = Cipher.getInstance(RSAKeyAlgorithm);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] plainData = cipher.doFinal(cipherBytes);
        return new String(plainData, UTF_8);
    }
}