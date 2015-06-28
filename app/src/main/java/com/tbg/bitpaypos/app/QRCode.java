package com.tbg.bitpaypos.app;

import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class QRCode extends AbstractQRCode {

    protected final String text;

    protected QRCode(String text) {
        this.text = text;
        qrWriter = new QRCodeWriter();
    }

    /**
     * Create a QR code from the given text. <br/>
     * <br/>
     * <p/>
     * There is a size limitation to how much you can put into a QR code. This
     * has been tested to work with up to a length of 2950 characters.<br/>
     * <br/>
     * <p/>
     * The QRCode will have the following defaults: <br/>
     * {size: 100x100}<br/>
     * {imageType:PNG} <br/>
     * <br/>
     * <p/>
     * Both size and imageType can be overridden: <br/>
     * Image type override is done by calling
     * {@link AbstractQRCode#to(net.glxn.qrgen.core.image.ImageType)} e.g.
     * QRCode.from("hello world").to(JPG) <br/>
     * Size override is done by calling {@link AbstractQRCode#withSize} e.g.
     * QRCode.from("hello world").to(JPG).withSize(125, 125) <br/>
     *
     * @param text the text to encode to a new QRCode, this may fail if the text
     *             is too large. <br/>
     * @return the QRCode object <br/>
     */
    public static QRCode from(String text) {
        return new QRCode(text);
    }

    /**
     * Creates a a QR Code from the given {@link VCard}.
     * <p/>
     * The QRCode will have the following defaults: <br/>
     * {size: 100x100}<br/>
     * {imageType:PNG} <br/>
     * <br/>
     *
     * @param vcard the vcard to encode as QRCode
     * @return the QRCode object
     */
    public static AbstractQRCode from(VCard vcard) {
        return new QRCode(vcard.toString());
    }

    @Override
    public File file() {
        File file;
        try {
            file = createTempFile();
            MatrixToImageWriter.writeToFile(createMatrix(text), imageType.toString(), file);
        } catch (Exception e) {
            throw new QRGenerationException("Failed to create QR image from text due to underlying exception", e);
        }

        return file;
    }

    @Override
    public File file(String name) {
        File file;
        try {
            file = createTempFile(name);
            MatrixToImageWriter.writeToFile(createMatrix(text), imageType.toString(), file);
        } catch (Exception e) {
            throw new QRGenerationException("Failed to create QR image from text due to underlying exception", e);
        }

        return file;
    }

    @Override
    protected void writeToStream(OutputStream stream) throws IOException,
            WriterException {
        MatrixToImageWriter.writeToStream(createMatrix(text),
                imageType.toString(), stream);
    }

}
