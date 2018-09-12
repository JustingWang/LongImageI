package com.hcz017.drawingview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;

public class DrawingView extends View {
    private static final String TAG = "DrawingView";
    private static final float TOUCH_TOLERANCE = 4;
    private Bitmap mBitmap;
    private Bitmap mOriginBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Paint mBitmapPaint;
    private Paint mPaint;
    private boolean mDrawMode;
    private float mX, mY;
    private float mProportion = 0;
    private SeekBar seekBar;

    private LinkedList<DrawPath> savePath;
    private DrawPath mLastDrawPath;
    private Matrix matrix;
    private float mPaintBarPenSize;
    private int mPaintBarPenColor;
    private int width, height;
    private Rect srcRect;
    private RectF dstRectF;
    private float lastY;
    private float slideLength;
    private Bitmap originalBitmap;


    private boolean isNeedSlide;

    public DrawingView(Context c) {
        this(c, null);
    }

    public DrawingView(Context c, AttributeSet attrs) {
        this(c, attrs, 0);
    }

    public DrawingView(Context c, AttributeSet attrs, int defStyle) {
        super(c, attrs, defStyle);
        init();
    }

    private void init() {
        Log.d(TAG, "init: ");
        mBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mDrawMode = false;
        savePath = new LinkedList<>();
        matrix = new Matrix();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int specSize = MeasureSpec.getSize(widthMeasureSpec);
        width = getPaddingLeft() + getPaddingRight() + specSize;
        specSize = MeasureSpec.getSize(heightMeasureSpec);
        height = getPaddingTop() + getPaddingBottom() + specSize;
        if (mBitmap == null) {
            mBitmap = resizeImage(mOriginBitmap, width);
            if (mBitmap.getHeight() > 1.5 * height) {
                //需要滑动
                setNeedSlide(true);
            } else {
                //不需要滑动
                setNeedSlide(false);
                srcRect.left = 0;
                srcRect.top = 0;
                srcRect.right = mBitmap.getWidth();
                srcRect.bottom = mBitmap.getHeight();
                if (mBitmap.getHeight() > height) {
                    mBitmap = resizeImageH(mBitmap, height - 20);
                } else {
                    float space = (height - mBitmap.getHeight());
                    dstRectF.left = 0;
                    dstRectF.top = space;
                    dstRectF.right = width;
                    dstRectF.bottom = height - space;
                }
            }
        }
        setMeasuredDimension(width, height);

    }
    public void setNeedSlide(boolean needSlide) {
        isNeedSlide = needSlide;
    }
    public Bitmap resizeImageH(Bitmap bitmap, int h) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = ((float) h) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleWidth);
        return Bitmap.createBitmap(bitmap, 0, 0, width,
                height, matrix, true);
    }
    public Bitmap resizeImage(Bitmap bitmap, int w) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = ((float) w) / width;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleWidth);
        return Bitmap.createBitmap(bitmap, 0, 0, width,
                height, matrix, true);
    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mBitmap == null) {
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        }
        mCanvas = new Canvas(mBitmap);
        mCanvas.drawColor(Color.TRANSPARENT);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawBitmap(mBitmap, (width - mBitmap.getWidth()) / 2,slideLength , mBitmapPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 如果你的界面有多个模式，你需要有个变量来判断当前是否可draw
        if (!mDrawMode) {
            return false;
        }
        lastY = event.getY();
        float x;
        float y;
        if (mProportion != 0) {
            x = (event.getX()) / mProportion;
            y = event.getY() / mProportion;
        } else {
            x = event.getX();
            y = event.getY();
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // This happens when we undo a path
                if (mLastDrawPath != null) {
                    mPaint.setColor(mPaintBarPenColor);
                    mPaint.setStrokeWidth(mPaintBarPenSize);
                }
                mPath = new Path();
                mPath.reset();
                mPath.moveTo(x, y+(Math.abs(slideLength)));
                mX = x;
                mY = y;
                mCanvas.drawPath(mPath, mPaint);
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(x - mX);
                float dy = Math.abs(y - mY);
                if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                    mPath.quadTo(mX, mY+(Math.abs(slideLength)), (x + mX) / 2, (y + mY) / 2+(Math.abs(slideLength)));
                    mX = x;
                    mY = y;
                }
                mCanvas.drawPath(mPath, mPaint);
                break;
            case MotionEvent.ACTION_UP:
                mPath.lineTo(mX, mY+(Math.abs(slideLength)));
                mCanvas.drawPath(mPath, mPaint);
                mLastDrawPath = new DrawPath(mPath, mPaint.getColor(), mPaint.getStrokeWidth());
                savePath.add(mLastDrawPath);
                mPath = null;
                break;
            default:
                break;
        }
        invalidate();
        return true;
    }

    public void initializePen() {
        mDrawMode = true;
        mPaint = null;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setFilterBitmap(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
    }

    @Override
    public void setBackgroundColor(int color) {
        mCanvas.drawColor(color);
        super.setBackgroundColor(color);
    }

    /**
     * This method should ONLY be called by clicking paint toolbar(outer class)
     */
    public void setPenSize(float size) {
        mPaintBarPenSize = size;
        mPaint.setStrokeWidth(size);
    }

    public float getPenSize() {
        return mPaint.getStrokeWidth();
    }

    /**
     * This method should ONLY be called by clicking paint toolbar(outer class)
     */
    public void setPenColor(@ColorInt int color) {
        mPaintBarPenColor = color;
        mPaint.setColor(color);
    }

    public
    @ColorInt
    int getPenColor() {
        return mPaint.getColor();
    }

    /**
     * @return 当前画布上的内容
     */
    public Bitmap getImageBitmap() {
        return mBitmap;
    }

    public void loadImage(Bitmap bitmap, SeekBar seekBar) {
        Log.d(TAG, "loadImage: ");
        this.seekBar=seekBar;
        mOriginBitmap = bitmap;
        mBitmap = mOriginBitmap.copy(Bitmap.Config.ARGB_8888, true);
        mCanvas = new Canvas(mBitmap);
        invalidate();
        mPath = new Path();
        seekBar.setMax(mBitmap.getHeight());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float moveY =mBitmap.getHeight()- progress;
                float distance = moveY - lastY;
                lastY = moveY;
                slideLength += distance;
                if (slideLength >= 0) {
                    slideLength = 0;
                }
                if (slideLength <= (-1) * (mBitmap.getHeight() - height)) {
                    slideLength = (-1) * (mBitmap.getHeight() - height);
                }
                postInvalidate();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public void undo() {
        Log.d(TAG, "undo: recall last path");
        if (savePath != null && savePath.size() > 0) {
            // 清空画布
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            loadImage(mOriginBitmap,seekBar);

            savePath.removeLast();

            // 将路径保存列表中的路径重绘在画布上 遍历绘制
            for (DrawPath dp : savePath) {
                mPaint.setColor(dp.getPaintColor());
                mPaint.setStrokeWidth(dp.getPaintWidth());
                mCanvas.drawPath(dp.path, mPaint);
            }
            invalidate();
        }
    }

    /**
     * 保存图片，其实我个人建议在其他类里面写保存方法，{@link #getImageBitmap()}就是内容
     *
     * @param filePath 路径名
     * @param filename 文件名
     * @param format   存储格式
     * @param quality  质量
     * @return 是否保存成功
     */
    public boolean saveImage(String filePath, String filename, Bitmap.CompressFormat format,
                             int quality) {
        if (quality > 100) {
            Log.d("saveImage", "quality cannot be greater that 100");
            return false;
        }
        File file;
        FileOutputStream out = null;
        try {
            switch (format) {
                case PNG:
                    file = new File(filePath, filename + ".png");
                    out = new FileOutputStream(file);
                    return mBitmap.compress(Bitmap.CompressFormat.PNG, quality, out);
                case JPEG:
                    file = new File(filePath, filename + ".jpg");
                    out = new FileOutputStream(file);
                    return mBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
                default:
                    file = new File(filePath, filename + ".png");
                    out = new FileOutputStream(file);
                    return mBitmap.compress(Bitmap.CompressFormat.PNG, quality, out);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 路径对象
     */
    private class DrawPath {
        Path path;
        int paintColor;
        float paintWidth;

        DrawPath(Path path, int paintColor, float paintWidth) {
            this.path = path;
            this.paintColor = paintColor;
            this.paintWidth = paintWidth;
        }

        int getPaintColor() {
            return paintColor;
        }

        float getPaintWidth() {
            return paintWidth;
        }
    }
}
