package com.iza.servitech_pro.ui.firma;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class SignatureView extends View {

    public interface OnSignatureChangedListener {
        void onChanged(boolean hasSignature);
    }

    private final Path path = new Path();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float lastX, lastY;
    private boolean empty = true;
    private OnSignatureChangedListener listener;

    public SignatureView(Context context) {
        super(context);
        init();
    }

    public SignatureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SignatureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setOnSignatureChangedListener(OnSignatureChangedListener l) {
        this.listener = l;
    }

    public boolean isEmpty() {
        return empty;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                path.moveTo(x, y);
                path.lineTo(x + 0.1f, y + 0.1f); // un toque también deja punto
                lastX = x;
                lastY = y;
                if (empty) {
                    empty = false;
                    if (listener != null) listener.onChanged(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                path.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2);
                lastX = x;
                lastY = y;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                path.lineTo(lastX, lastY);
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        invalidate();
        return true;
    }

    public void clear() {
        path.reset();
        empty = true;
        if (listener != null) listener.onChanged(false);
        invalidate();
    }

    /** Firma con fondo blanco, lista para guardar o enviar. */
    public Bitmap getSignatureBitmap() {
        if (empty || getWidth() == 0 || getHeight() == 0) return null;
        Bitmap bmp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);
        canvas.drawPath(path, paint);
        return bmp;
    }
}