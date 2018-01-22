package com.rhino.zxingdemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.rhino.zxingdemo.encoding.EncodeHandler;

public class ShareActivity extends AppCompatActivity {


    ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        mImageView = findViewById(R.id.img);

        Bitmap logoBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        Bitmap qrCodeBitmap = EncodeHandler.createQRBitmapWithIcon("www.baidu.com",
                logoBitmap, 0xFF000000, false);

        mImageView.setImageBitmap(qrCodeBitmap);
    }



}
