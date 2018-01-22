package com.rhino.zxingdemo.encoding;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.support.annotation.ColorInt;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.Hashtable;


public class EncodeHandler {

    private static final int QR_WIDTH = 500;
    private static final int QR_HEIGHT = 500;

    private static final int IC_WIDTH = 40;
    private static final int IC_HEIGHT = 40;

    public static Bitmap createQRBitmap(String content, @ColorInt int color, boolean hasMargin) {
        Bitmap bitmap = null;
        try {
            QRCodeWriter writer = new QRCodeWriter();
            if (content == null || "".equals(content) || content.length() < 1) {
                return null;
            }
            Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT, hints);
            int qrWidth = QR_WIDTH;
            int qrHeight = QR_HEIGHT;
            if (!hasMargin) {
                bitMatrix = deleteWhite(bitMatrix);
                qrWidth = bitMatrix.getWidth();
                qrHeight = bitMatrix.getHeight();
            }
            int[] pixels = new int[qrWidth * qrHeight];
            for (int y = 0; y < qrHeight; y++) {
                for (int x = 0; x < qrWidth; x++) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * qrWidth + x] = color;
                    } else {
                        pixels[y * qrWidth + x] = 0xffffffff;
                    }
                }
            }

            bitmap = Bitmap.createBitmap(qrWidth, qrHeight, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, qrWidth, 0, 0, qrWidth, qrHeight);
        } catch (WriterException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    public static Bitmap createQRBitmapWithIcon(String content, Bitmap bitmap, @ColorInt int color, boolean hasMargin) {
        if (content == null || content.isEmpty() || bitmap == null) {
            return null;
        }
        Matrix iconM = new Matrix();
        float scaleX = (float) 2 * IC_WIDTH / bitmap.getWidth();
        float scaleY = (float) 2 * IC_HEIGHT / bitmap.getHeight();
        iconM.setScale(scaleX, scaleY);
        Bitmap mNewIcon = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), iconM, false);
        Bitmap newBitmap = null;
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT, hints);
            int qrWidth = QR_WIDTH;
            int qrHeight = QR_HEIGHT;
            if (!hasMargin) {
                bitMatrix = deleteWhite(bitMatrix);
                qrWidth = bitMatrix.getWidth();
                qrHeight = bitMatrix.getHeight();
            }
            int halfW = qrWidth / 2;
            int halfH = qrHeight / 2;
            int[] pixels = new int[qrWidth * qrHeight];
            for (int y = 0; y < qrHeight; y++) {
                for (int x = 0; x < qrWidth; x++) {
                    if (x > halfW - IC_WIDTH && x < halfW + IC_WIDTH && y > halfH - IC_HEIGHT && y < halfH + IC_HEIGHT) {
                        pixels[y * qrWidth + x] = mNewIcon.getPixel(x - halfW + IC_WIDTH, y - halfH + IC_HEIGHT);
                    } else {
                        pixels[y * qrWidth + x] = bitMatrix.get(x, y) ? color : 0xffffffff;
                    }
                }
            }
            newBitmap = Bitmap.createBitmap(qrWidth, qrHeight, Bitmap.Config.ARGB_8888);
            newBitmap.setPixels(pixels, 0, qrWidth, 0, 0, qrWidth, qrHeight);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return newBitmap;
    }

    private static BitMatrix deleteWhite(BitMatrix matrix) {
        int[] rec = matrix.getEnclosingRectangle();
        int resWidth = rec[2] + 1;
        int resHeight = rec[3] + 1;

        BitMatrix resMatrix = new BitMatrix(resWidth, resHeight);
        resMatrix.clear();
        for (int i = 0; i < resWidth; i++) {
            for (int j = 0; j < resHeight; j++) {
                if (matrix.get(i + rec[0], j + rec[1]))
                    resMatrix.set(i, j);
            }
        }
        return resMatrix;
    }
}
