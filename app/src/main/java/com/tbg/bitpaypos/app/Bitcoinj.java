package com.tbg.bitpaypos.app;

import android.util.Log;


import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Base58;
import com.google.bitcoin.core.DumpedPrivateKey;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.RegTestParams;
import com.google.bitcoin.utils.BriefLogFormatter;

import java.io.File;

/**
 * Created by joeykrug2 on 10/8/14.
 */

// network param instance, wallet instance

public class Bitcoinj {
    private NetworkParameters params;
    private String filePrefix;

    public Bitcoinj() {
        BriefLogFormatter.init();

        params = MainNetParams.get();
        filePrefix = "forwarding-service";

                ECKey key = new ECKey();

                byte[] pub = key.getPubKey();
                byte[] pubKeyHash = key.getPubKeyHash();
                String btcAddress = new Address(params, pubKeyHash).toString();
                Log.d("Add", btcAddress);

                String prv;
                DumpedPrivateKey privateKey = key.getPrivateKeyEncoded(params);
                Log.d("prv", privateKey.toString());

    }
}
