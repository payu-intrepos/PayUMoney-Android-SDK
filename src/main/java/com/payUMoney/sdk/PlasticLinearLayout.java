package com.payUMoney.sdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class PlasticLinearLayout extends LinearLayout {
    private static final double SHINE_HEIGHT = 0.85;
    private final Paint shinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    //	private Bitmap tornImage;
    //	private Drawable mMainBkg;
    private Bitmap mBackground;

//	private static int MAX_DOWN = 5;
//	private static int MAX_SIDE = 5;

    public PlasticLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        //		mMainBkg = getResources().getDrawable(R.drawable.plastic_background);
        // The subtle gradient draws behind everything.
        // setBackgroundResource(R.drawable.plastic_background);
    }

    public PlasticLinearLayout(Context context) {
        super(context);
        //		mMainBkg = getResources().getDrawable(R.drawable.plastic_background);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (mBackground != null) {
            mBackground.recycle();
            System.gc();
            mBackground = null;
        }
        //		createShinePath();
        //		mMainBkg.setBounds(0, 0, w, h);
        try {
            mBackground = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mBackground);
            //		mMainBkg.draw(canvas);

            canvas.drawColor(getResources().getColor(R.color.clouds));

            int height = (int) (SHINE_HEIGHT * h);

            Path shinePath = new Path();
            shinePath.moveTo(0, 0);
            shinePath.lineTo(w, 0);
            shinePath.lineTo(w, height);
            shinePath.close();
            shinePaint.setShader(new LinearGradient(0, 0, 0, height, 0x70ffffff, 0x10ffffff, TileMode.CLAMP));

            // tornImage should be drawn again only if width has changed
//		if(w != oldw || tornImage == null) {
//			createTornPaper();
//		}

            canvas.drawPath(shinePath, shinePaint);
        } catch (OutOfMemoryError ex) {
            ex.printStackTrace();
        }

        //		canvas.drawBitmap(tornImage, 0, 0, null);

        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {

        // canvas.drawColor(Color.GRAY);
        // Draw the shine behind the children.
        // canvas.drawPath(shinePath, shinePaint);
        if (mBackground == null) {
            canvas.drawColor(getResources().getColor(R.color.clouds));
        } else {
            canvas.drawBitmap(mBackground, 0, 0, null);
        }

        // Draw the children.
//		canvas.save();
//		canvas.translate(0, MAX_DOWN);
        super.dispatchDraw(canvas);
//		canvas.restore();
    }

//	private void createTornPaper() {
//		// implement torn paper effect
//
//		int x = 0, y, prevX = 0, prevY = 0;
//		int width = getWidth();
//		// int height = (int) (0.85 * getHeight());
//
//		Bitmap paper = Bitmap.createBitmap(width, MAX_DOWN, Bitmap.Config.ARGB_8888);
//		Canvas canvas = new Canvas(paper);
//		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//		paint.setStrokeWidth(1);
//		paint.setColor(Color.GRAY);
//		paint.setShadowLayer(3, 1, 1, Color.BLACK);
//		// paint.setPathEffect(new DashPathEffect(new float[] { 5, 6 }, 0));
//
//		while(x < width) {
//			x += (Math.random() * MAX_SIDE) + 5;
//			y = (int) (Math.random() * MAX_DOWN);
//			// tornPath.lineTo(x, y);
//			canvas.drawLine(prevX, prevY, x, y, paint);
//			prevX = x;
//			prevY = y;
//		}
//
//		// use the actionbar image as torn paper
//		// Paint paint = new Paint();
//		// canvas.drawPath(tornPath, paint);
//
//		// Drawable AB_BKG =
//		// mContext.getResources().getDrawable(R.drawable.app_background);
//		// AB_BKG.setBounds(0, 0, width, MAX_DOWN);
//		// paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
//		// canvas.saveLayer(0, 0, width, MAX_DOWN, paint, Canvas.ALL_SAVE_FLAG);
//		// AB_BKG.draw(canvas);
//		// canvas.restore();
//
//		tornImage = paper;
//
//		//		shinePath = new Path();
//		//		shinePath.moveTo(0, 0);
//		//		shinePath.lineTo(width, 0);
//		//		shinePath.lineTo(width, height);
//		//		shinePath.close();
//		//		shinePaint.setShader(new LinearGradient(0, 0, 0, height, 0x60ffffff, 0x30ffffff, TileMode.CLAMP));
//	}
}
