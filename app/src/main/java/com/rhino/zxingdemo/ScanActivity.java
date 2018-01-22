package com.rhino.zxingdemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;
import com.rhino.zxingdemo.camera.CameraManager;
import com.rhino.zxingdemo.decoding.ScannerViewHandler;
import com.rhino.zxingdemo.view.ViewfinderView;

import java.io.IOException;

public class ScanActivity extends AppCompatActivity implements ScannerViewHandler.HandleDecodeListener, SurfaceHolder.Callback{

    private static final String TAG = ScanActivity.class.getName();

    private SurfaceHolder mSurfaceHolder;
    private ViewfinderView mViewfinderView;
    private ScannerViewHandler mScannerViewHandler;
    private boolean mSurfaceCreated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        SurfaceView mSurfaceView = findViewById(R.id.scan_surface_view);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mViewfinderView = findViewById(R.id.scan_qrcode_scan_view);

        mSurfaceCreated = false;
        CameraManager.init(this, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSurfaceCreated) {
            Log.d(TAG, "onResume initCamera");
            initCamera(mSurfaceHolder);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != mScannerViewHandler) {
            mScannerViewHandler.quitSynchronously();
            mScannerViewHandler = null;
        }
        CameraManager.get().closeDriver();
    }

    @Override
    public void handleDecode(Result obj, Bitmap barcode) {
        Log.d(TAG, "result: " + obj.toString());

        Toast.makeText(this, obj.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public ScannerViewHandler getHandler() {
        return mScannerViewHandler;
    }

    @Override
    public void restartPreviewAndDecode() {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!mSurfaceCreated) {
            Log.d(TAG, "surfaceCreated initCamera");
            mSurfaceCreated = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceCreated = false;
    }


    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            CameraManager.get().openDriver(surfaceHolder);
            CameraManager.get().setCameraDisplayOrientation(this);
            if (null == mScannerViewHandler) {
                mScannerViewHandler = new ScannerViewHandler(this, null, null, new ResultPointCallback() {
                    @Override
                    public void foundPossibleResultPoint(ResultPoint resultPoint) {
                        mViewfinderView.addPossibleResultPoint(resultPoint);
                    }
                });
            }
            mViewfinderView.updateScanRect(CameraManager.get().getFramingRect());
            mViewfinderView.startScanAnim(500);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            initCameraFailed();
        } catch (RuntimeException e) {
            e.printStackTrace();
            initCameraFailed();
        }
    }

    private void initCameraFailed() {
        Toast.makeText(this, "打开摄像头失败", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        finish();
    }


}
