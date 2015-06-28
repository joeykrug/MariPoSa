package com.tbg.bitpaypos.app;

import com.haibison.android.lockpattern.collect.Lists;
import com.haibison.android.lockpattern.util.IEncrypter;
import com.haibison.android.lockpattern.widget.LockPatternView.Cell;

import java.util.List;

import android.content.Context;

public class LPEncrypter implements IEncrypter {

    @Override
    public char[] encrypt(Context context, List<Cell> pattern) {
        /*
         * Simple encryption of passcode
         */

        StringBuilder result = new StringBuilder();
        for (Cell cell : pattern)
            result.append(Integer.toString(cell.getId() + 47)).append('-');

        return result.substring(0, result.length() - 1).toCharArray();
    }// encrypt()

    @Override
    public List<Cell> decrypt(Context context, char[] encryptedPattern) {
        List<Cell> result = Lists.newArrayList();
        String[] ids = new String(encryptedPattern).split("[^0-9]");
        for (String id : ids)
            result.add(Cell.of(Integer.parseInt(id) - 47));

        return result;
    }// decrypt()

}