package com.rhino.zxingdemo.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.google.zxing.ResultPoint;
import com.rhino.zxingdemo.R;

import java.util.Collection;
import java.util.HashSet;

/**
 * <p>This is a custom view for scanning qr code</p>
 *
 * @since Created by LuoLin on 2018/1/18.
 **/
public class ViewfinderView extends View {

    private static final String TAG = ViewfinderView.class.getName();

    private static final int DEFAULT_REFRESH_DELAY_TIME = 2;
    private static final int DEFAULT_SCAN_ANIM_SPEED = 2;
    private static final int DEFAULT_CORNER_COLOR = 0x88000000;
    private static final int DEFAULT_CORNER_WIDTH = 3;
    private static final int DEFAULT_CORNER_LENGTH = 40;
    private static final int DEFAULT_RECT_STROKE_COLOR = 0xFF000000;
    private static final int DEFAULT_RECT_STROKE_WIDTH = 1;
    private static final int DEFAULT_SCAN_LINE_COLOR = 0xFF000000;
    private static final int DEFAULT_SCAN_LINE_WIDTH = 2;
    private static final int DEFAULT_SCAN_RECT_OUTSIDE_COLOR = 0x88000000;
    private static final int DEFAULT_SCAN_RESULT_POINT_COLOR = 0xC0FFFF00;
    private static final int DEFAULT_SCAN_RECT_WIDTH = 420;
    private static final int DEFAULT_SCAN_RECT_HEIGHT = 420;
    private float mScanAnimSpeed = DEFAULT_SCAN_ANIM_SPEED;
    private int mCornerLength = DEFAULT_CORNER_LENGTH;
    private int mCornerWidth = DEFAULT_CORNER_WIDTH;
    private int mCornerColor = DEFAULT_CORNER_COLOR;
    private int mRectStrokeColor = DEFAULT_RECT_STROKE_COLOR;
    private int mRectStrokeWidth = DEFAULT_RECT_STROKE_WIDTH;
    private int mScanLineColor = DEFAULT_SCAN_LINE_COLOR;
    private int mScanLineWidth = DEFAULT_SCAN_LINE_WIDTH;
    private int mScanRectOutsideColor = DEFAULT_SCAN_RECT_OUTSIDE_COLOR;
    private int mScanRectWidth = DEFAULT_SCAN_RECT_WIDTH;
    private int mScanRectHeight = DEFAULT_SCAN_RECT_HEIGHT;
    private boolean mScanRectUpdated = false;
    private Rect mScanRect = new Rect();
    private RefreshRunnable mRefreshRunnable;
    private boolean mIsScanAnimStarted;
    private float mScanLineTop = 0;

    private int mViewWidth;
    private int mViewHeight;
    private Paint mPaint;

    private int mResultPointColor = DEFAULT_SCAN_RESULT_POINT_COLOR;
    private Collection<ResultPoint> mResultPointList = new HashSet<>(5);
    private Collection<ResultPoint> mLastResultPointList = new HashSet<>(5);

    public ViewfinderView(Context context) {
        super(context);
        init(context, null);
    }

    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ViewfinderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if (widthMode == MeasureSpec.EXACTLY) {
            mViewWidth = widthSize;
        } else {
            mViewWidth = getWidth();
        }
        if (heightMode == MeasureSpec.EXACTLY) {
            mViewHeight = heightSize;
        } else {
            mViewHeight = getHeight();
        }
        initView(mViewWidth, mViewHeight);
        setMeasuredDimension(mViewWidth, mViewHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        drawRectOutside(canvas);
        drawRectStroke(canvas);
        drawCorner(canvas);
        if (mIsScanAnimStarted) {
            drawScanResultPoint(canvas);
            drawScanLine(canvas);
        }
        canvas.restore();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopScanAnim();
        Log.d(TAG, "onDetachedFromWindow");
    }

    /**
     * Do something init.
     *
     * @param context Context
     * @param attrs   AttributeSet
     */
    private void init(Context context, AttributeSet attrs) {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (attrs == null) {
            return;
        }
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ViewfinderView);
        mScanRectWidth = typedArray.getDimensionPixelSize(R.styleable.ViewfinderView_sv_scan_width, DEFAULT_SCAN_RECT_WIDTH);
        mScanRectHeight = typedArray.getDimensionPixelSize(R.styleable.ViewfinderView_sv_scan_height, DEFAULT_SCAN_RECT_HEIGHT);
        mScanRectOutsideColor = typedArray.getColor(R.styleable.ViewfinderView_sv_scan_outside_color, DEFAULT_SCAN_RECT_OUTSIDE_COLOR);
        mRectStrokeWidth = typedArray.getDimensionPixelSize(R.styleable.ViewfinderView_sv_stroke_width, DEFAULT_RECT_STROKE_WIDTH);
        mRectStrokeColor = typedArray.getColor(R.styleable.ViewfinderView_sv_stroke_color, DEFAULT_RECT_STROKE_COLOR);
        mCornerWidth = typedArray.getDimensionPixelSize(R.styleable.ViewfinderView_sv_corner_width, DEFAULT_CORNER_WIDTH);
        mCornerLength = typedArray.getDimensionPixelSize(R.styleable.ViewfinderView_sv_corner_length, DEFAULT_CORNER_LENGTH);
        mCornerColor = typedArray.getColor(R.styleable.ViewfinderView_sv_corner_color, DEFAULT_CORNER_COLOR);
        mScanLineWidth = typedArray.getDimensionPixelSize(R.styleable.ViewfinderView_sv_anim_line_width, DEFAULT_SCAN_LINE_WIDTH);
        mScanLineColor = typedArray.getColor(R.styleable.ViewfinderView_sv_anim_line_color, DEFAULT_SCAN_LINE_COLOR);
        typedArray.recycle();
    }

    /**
     * Do something init.
     *
     * @param width  width
     * @param height height
     */
    private void initView(int width, int height) {
        if (mScanRectUpdated) {
            return;
        }
        if (0 >= width || 0 >= height) {
            return;
        }
        mScanRect.top = mViewHeight / 2 - mScanRectHeight / 2;
        mScanRect.left = mViewWidth / 2 - mScanRectWidth / 2;
        mScanRect.bottom = mScanRect.top + mScanRectHeight;
        mScanRect.right = mScanRect.left + mScanRectWidth;
    }

    /**
     * Draw the rect stroke.
     *
     * @param canvas Canvas
     */
    private void drawRectOutside(Canvas canvas) {
        canvas.save();
        mPaint.setColor(mScanRectOutsideColor);
        canvas.drawRect(0, 0, mViewWidth, mScanRect.top, mPaint);
        canvas.drawRect(0, mScanRect.top, mScanRect.left, mScanRect.bottom, mPaint);
        canvas.drawRect(mScanRect.right, mScanRect.top, mViewWidth, mScanRect.bottom, mPaint);
        canvas.drawRect(0, mScanRect.bottom, mViewWidth, mViewHeight, mPaint);
        canvas.restore();
    }

    /**
     * Draw the rect stroke.
     *
     * @param canvas Canvas
     */
    private void drawRectStroke(Canvas canvas) {
        canvas.save();
        mPaint.setColor(mRectStrokeColor);
        canvas.drawRect(mScanRect.left + mRectStrokeWidth, mScanRect.top, mScanRect.right - mRectStrokeWidth, mScanRect.top + mRectStrokeWidth, mPaint);
        canvas.drawRect(mScanRect.left, mScanRect.bottom - mRectStrokeWidth, mScanRect.right - mRectStrokeWidth, mScanRect.bottom, mPaint);
        canvas.drawRect(mScanRect.left, mScanRect.top, mScanRect.left + mRectStrokeWidth, mScanRect.bottom, mPaint);
        canvas.drawRect(mScanRect.right - mRectStrokeWidth, mScanRect.top, mScanRect.right, mScanRect.bottom, mPaint);
        canvas.restore();
    }

    /**
     * Draw the four corner.
     *
     * @param canvas Canvas
     */
    private void drawCorner(Canvas canvas) {
        canvas.save();
        mPaint.setColor(mCornerColor);
        canvas.drawRect(mScanRect.left + mCornerWidth, mScanRect.top, mScanRect.left + mCornerLength, mScanRect.top + mCornerWidth, mPaint);
        canvas.drawRect(mScanRect.left, mScanRect.top, mScanRect.left + mCornerWidth, mScanRect.top + mCornerLength, mPaint);

        canvas.drawRect(mScanRect.right - mCornerLength, mScanRect.top, mScanRect.right - mCornerWidth, mScanRect.top + mCornerWidth, mPaint);
        canvas.drawRect(mScanRect.right - mCornerWidth, mScanRect.top, mScanRect.right, mScanRect.top + mCornerLength, mPaint);

        canvas.drawRect(mScanRect.left + mCornerWidth, mScanRect.bottom - mCornerWidth, mScanRect.left + mCornerLength, mScanRect.bottom, mPaint);
        canvas.drawRect(mScanRect.left, mScanRect.bottom - mCornerLength, mScanRect.left + mCornerWidth, mScanRect.bottom, mPaint);

        canvas.drawRect(mScanRect.right - mCornerLength, mScanRect.bottom - mCornerWidth, mScanRect.right - mCornerWidth, mScanRect.bottom, mPaint);
        canvas.drawRect(mScanRect.right - mCornerWidth, mScanRect.bottom - mCornerLength, mScanRect.right, mScanRect.bottom, mPaint);
        canvas.restore();
    }

    /**
     * Draw scan result point.
     * @param canvas Canvas
     */
    private void drawScanResultPoint(Canvas canvas) {
        canvas.save();
        Collection<ResultPoint> currentPossible = mResultPointList;
        Collection<ResultPoint> currentLast = mLastResultPointList;
        if (currentPossible.isEmpty()) {
            mLastResultPointList = null;
        } else {
            mResultPointList = new HashSet<>(5);
            mLastResultPointList = currentPossible;
            mPaint.setAlpha(0xFF);
            mPaint.setColor(mResultPointColor);
            for (ResultPoint point : currentPossible) {
                canvas.drawCircle(mScanRect.left + point.getX(), mScanRect.top + point.getY(), 6.0f, mPaint);
            }
        }
        if (currentLast != null) {
            mPaint.setAlpha(0xFF / 2);
            mPaint.setColor(mResultPointColor);
            for (ResultPoint point : currentLast) {
                canvas.drawCircle(mScanRect.left + point.getX(), mScanRect.top + point.getY(), 3.0f, mPaint);
            }
        }
        canvas.restore();
    }


    /**
     * Draw scan line.
     *
     * @param canvas Canvas
     */
    private void drawScanLine(Canvas canvas) {
        canvas.save();
        mPaint.setColor(mScanLineColor);
        canvas.drawRect(mScanRect.left + mRectStrokeWidth, mScanRect.top + mScanLineTop, mScanRect.right - mRectStrokeWidth, mScanRect.top + mScanLineTop + mScanLineWidth, mPaint);
        canvas.restore();
    }

    /**
     * The refresh Runnable.
     */
    private class RefreshRunnable implements Runnable {
        public void run() {
            mScanLineTop += mScanAnimSpeed;
            if (mScanLineTop >= mScanRectHeight - mRectStrokeWidth - mScanLineWidth) {
                mScanLineTop = mRectStrokeWidth;
            }
            invalidate();
            postDelayed(this, DEFAULT_REFRESH_DELAY_TIME);
        }
    }

    /**
     * Return whether scan anim started.
     *
     * @return True started
     */
    public boolean isScanAnimStarted() {
        return mIsScanAnimStarted;
    }

    /**
     * Start the scan anim.
     */
    public void startScanAnim() {
        startScanAnim(0);
    }

    /**
     * Start the scan anim after some time.
     *
     * @param delayMillis The delay (in milliseconds) until the anim
     *                    will be started.
     */
    public void startScanAnim(int delayMillis) {
        if (mIsScanAnimStarted) {
            return;
        }
        mIsScanAnimStarted = true;
        if (null == mRefreshRunnable) {
            mRefreshRunnable = new RefreshRunnable();
        } else {
            removeCallbacks(mRefreshRunnable);
        }
        postDelayed(mRefreshRunnable, delayMillis);
    }

    /**
     * Stop the scan anim
     */
    public void stopScanAnim() {
        if (null != mRefreshRunnable) {
            mIsScanAnimStarted = false;
            removeCallbacks(mRefreshRunnable);
        }
    }

    /**
     * Set the scan anim speed.
     *
     * @param speed The scan anim speed.
     */
    public void setScanAnimSpeed(float speed) {
        this.mScanAnimSpeed = speed;
    }

    /**
     * Set the corner length.
     *
     * @param length The corner length.
     */
    public void setCornerLength(int length) {
        this.mCornerLength = length;
    }

    /**
     * Set the corner width.
     *
     * @param width The corner width.
     */
    public void setCornerWidth(int width) {
        this.mCornerWidth = width;
    }

    /**
     * Set the corner color.
     *
     * @param color The corner color.
     */
    public void setCornerColor(@ColorInt int color) {
        this.mCornerColor = color;
    }

    /**
     * Set the rect stroke color.
     *
     * @param color The rect stroke color.
     */
    public void setRectStrokeColor(@ColorInt int color) {
        this.mRectStrokeColor = color;
    }

    /**
     * Set the rect stroke width.
     *
     * @param width The rect stroke width.
     */
    public void setRectStrokeWidth(int width) {
        this.mRectStrokeWidth = width;
    }

    /**
     * Set the scan line color.
     *
     * @param color The scan line color.
     */
    public void setScanLineColor(@ColorInt int color) {
        this.mScanLineColor = color;
    }

    /**
     * Set the scan line width.
     *
     * @param width The scan line width.
     */
    public void setScanLineWidth(int width) {
        this.mScanLineWidth = width;
    }

    /**
     * Set the outside color.
     *
     * @param color the outside color.
     */
    public void setScanRectOutsideColor(@ColorInt int color) {
        this.mScanRectOutsideColor = color;
    }

    /**
     * Set the scan rect width.
     *
     * @param width the scan rect width.
     */
    public void setScanRectWidth(int width) {
        this.mScanRectWidth = mScanRectWidth;
    }

    /**
     * Set the scan rect height.
     *
     * @param height the scan rect height.
     */
    public void setScanRectHeight(int height) {
        this.mScanRectHeight = mScanRectHeight;
    }

    /**
     * Set the result point color.
     *
     * @param color the result point color.
     */
    public void setResultPointColor(@ColorInt int color) {
        this.mResultPointColor = color;
    }

    public void addPossibleResultPoint(@Nullable ResultPoint point) {
        if (null == point) {
            return;
        }
        Log.d(TAG, "point = " + point.toString());
        mResultPointList.add(point);
    }

    public void updateScanRect(@Nullable Rect rect) {
        if (null == rect || rect.isEmpty()) {
            return;
        }
        if (rect.equals(mScanRect)) {
            return;
        }
        mScanRectUpdated = true;
        mScanRect.top = rect.top;
        mScanRect.bottom = rect.bottom;
        mScanRect.left = rect.left;
        mScanRect.right = rect.right;
        mScanRectWidth = mScanRect.width();
        mScanRectHeight = mScanRect.height();
        invalidate();
    }

}
