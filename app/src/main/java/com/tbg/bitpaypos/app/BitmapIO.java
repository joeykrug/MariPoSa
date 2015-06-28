package com.tbg.bitpaypos.app;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class BitmapIO {


    public static boolean write(Bitmap image, String type, OutputStream stream) {
        if (type.equals(ImageType.PNG)) {
            return image.compress(CompressFormat.PNG, 80, stream);
        } else if (type.equals(ImageType.JPG)) {
            return image.compress(CompressFormat.JPEG, 80, stream);
        }

        // Default is PNG and for GIFs also
        return image.compress(CompressFormat.PNG, 80, stream);
    }

    public static boolean write(Bitmap image, String type, File file) throws IOException {
        OutputStream stream = null;
        try {
            stream = new FileOutputStream(file);
            return write(image, type, stream);

        } catch (IOException ioe) {
            Log.d("IO", ""+ioe);
        } finally {
            if (stream != null) {
                stream.flush();
                stream.close();
            }
        }

        return false;
    }
}
