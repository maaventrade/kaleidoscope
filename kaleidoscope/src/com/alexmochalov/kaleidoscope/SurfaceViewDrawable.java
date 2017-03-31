package com.alexmochalov.kaleidoscope;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import com.example.cameraexample.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.Bitmap.Config;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.Toast;

public class SurfaceViewDrawable extends SurfaceView implements Callback {

	private Context mContext;
	private final String SCALE = "SCALE";
	
	private SurfaceHolder surfaceHolder;
	
	private DrawThread drawThread;
	
	// Mask for a cutting triangles from the original images
	private Bitmap mask = null;
	// Size of the Mask
	public float maskWidth = 221f;
	public float maskHeight = maskWidth/1.157894737f;  //190f;
	
	// Triangles can be scaled
	private float mScale = 1;
	
	private int x = 0;

	private int y = 0;
	private int zx = 1;

	private int zy = 1;
	private int dx = 0;

	private int dy = 0;
	
	private Rect rectCameraDst;
	
	// Coordinates of touching down
	private Point downCoords = new Point();
	// Time of the touching event
	private long eventTime;
	// Coordinates of previous touching event
	private float prevX;
	private float prevY;
	
	public TouchEventCallback touchEventCallback;
	
	private Handler handler = new Handler(); 
	private int slideY = 0; // 
	private int slideX = 0; // 
	
	private boolean mMakePhoto = false;
	
	interface TouchEventCallback { 
		void callbackCall(); 
		void callbackPhoto(); 
	}
	
	private void init(Context context){
		mContext = context;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

		mScale = prefs.getFloat(SCALE, 1);

		surfaceHolder = getHolder();
	    surfaceHolder.addCallback(this);
	    surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
	}
	
	public SurfaceViewDrawable(Context context, AttributeSet attrs,
			int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		
		init(context);
	}
	
	public SurfaceViewDrawable(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		
		init(context);
	}
	
	public SurfaceViewDrawable(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		init(context);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
    	fillMask();
		
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
    	//fillMask();
    	
		drawThread = new DrawThread(mContext, holder);
        drawThread.setRunning(true);
        
		drawThread.setMask(mask);
		
		//drawThread.setShowIconPhoto(showIconPhoto);

		rectCameraDst = new Rect(width-width/6, height-width/6, width-10, height-10); 
		drawThread.setParams(width, height, rectCameraDst);
		
        drawThread.start();
	}

	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		boolean retry = true;
        // finish the thread wirking
		
		drawThread.onStop();
        drawThread.setRunning(false);
        while (retry) {
            try {
                drawThread.join();
                retry = false;
            } catch (InterruptedException e) {
                // try again ang again
            }
        }
	}

    private void randomZero() {
		double n = Math.random();
		if (n < 0.5f)
			zx = 1 - zx;
		n = Math.random();
		if (n < 0.5f)
			zy = 1 - zy;
		if (zx == 0 && zy == 0){
			n = Math.random();
			if (n < 0.5f) zx = 1;
			else zy = 1;
		}
	}

 	public void setScale(float scale){
 		mScale = scale;
 		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		Editor editor = prefs.edit();
		editor.putFloat("SCALE", mScale);
		editor.apply();
 		
 		fillMask();
 		if (drawThread != null)
 			drawThread.setMask(mask);
 		
 		//invalidate();
 	}
    
	private final float CTG_60 = 0.57735026919f;
	private final float TG_60 = 1.73205080757f;
    	
	private void fillMask(){

		mask = Bitmap.createBitmap((int)(maskWidth*mScale), (int)(maskHeight*mScale), Config.ARGB_8888);
    	//Clear the canvas
    	Canvas canvas = new Canvas(mask);

    	Paint paint = new Paint();
    	
    	paint.setXfermode(new PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR));
    	canvas.drawRect(0, 0, maskWidth*mScale, maskHeight*mScale, paint);    	
    	paint.setXfermode(null);
		
    	// Add white triangle.
    	for (float x = 0 ; x < maskWidth*mScale ; x++)
        	for (float y = 0; y < maskHeight*mScale; y++)
        		// If the point locates in the triangle area
        		if (y <= x/CTG_60 && x <= maskWidth*mScale/2  
        		|| y < maskWidth*mScale*TG_60 - (x)/CTG_60 && x > maskWidth*mScale/2
        		// "+ 2" for overlapping triangles  
        				) 
        			canvas.drawPoint(x, maskHeight*mScale-y, paint);
    	
	}
    
	public void setBitmaps(YuvImage yuvImage, Size previewSize) {
		 
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	
    	int left = (int)(previewSize.width - maskWidth * mScale)/2+x;
    	int top = (int)(previewSize.height - maskHeight*mScale)/2+y;
    	
    	int right = (int)(left + maskWidth * mScale);
    	int bottom = (int)(top + maskHeight * mScale);
    	
    	if (left + dx * zx < 0 ||
    		right + dx * zx > previewSize.width) dx = -dx;
    	else {
    		randomZero(); 
        	if (left + dx * zx < 0 ||
            		right + dx * zx > previewSize.width) dx = -dx;
    	}
    		
    	if (top + dy * zy < 0 ||
    		bottom + dy * zy > previewSize.height) dy = -dy;
    	else {
    		randomZero(); 
        	if (top + dy * zy < 0 ||
            		bottom + dy * zy > previewSize.height) dy = -dy;
    	}

    	if (left + dx * zx < 0 ||
        		right + dx * zx > previewSize.width) zx = 0;
    	if (top + dy * zy < 0 ||
        		bottom + dy * zy > previewSize.height) zy = 0;
    	
    	x += dx * zx;
		y += dy * zy;
		
    	yuvImage.compressToJpeg(new Rect(left, top, right, bottom), 90, out);
    	//yuvImage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 90, out);
    	
    	byte[] imageBytes = out.toByteArray();
    	Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

		if (drawThread != null)
			/*
			if (drawThread.isTest())
				drawThread.setOriginalBitmap(
		    		Bitmap.createBitmap(
		    				BitmapFactory.decodeResource(mContext.getResources(),R.drawable.image), 0,0, 
		    				mask.getWidth(), 
		    				mask.getHeight()));
			else	
			*/	
				drawThread.setOriginalBitmap(bitmap);
	}

	
	@Override
    public boolean onTouchEvent(MotionEvent event) {

		float x = event.getX();
		float y = event.getY();
	
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			
			if (rectCameraDst.contains((int)x, (int)y)){
				if (mMakePhoto)
					if (touchEventCallback != null){
						drawThread.setInformation("Saving...");
						touchEventCallback.callbackPhoto();
					}	
					else;
				else {
					drawThread.setInformation("Saving...");
					makeScreenShot(null);
					drawThread.setInformation("");
				}
				break; 
			}
			
			downCoords.x = (int)x;
			downCoords.y = (int)y;
			
			eventTime = event.getEventTime();
			break;
		case MotionEvent.ACTION_MOVE:
			drawThread.move(x-prevX, y-prevY);
			break;
		case MotionEvent.ACTION_UP:
			if (downCoords.x == (int)x && downCoords.y == (int)y)
				if (touchEventCallback != null)
					touchEventCallback.callbackCall();
			
        	long eventTime1 = event.getEventTime();
		    
            if (downCoords.y > y) slideY = -Math.round((downCoords.y - y)/(eventTime1-eventTime)*50);
            else if (downCoords.y < y) slideY = -Math.round((downCoords.y - y)/(eventTime1-eventTime)*50);
            else downCoords.y = 0;
		    
            if (downCoords.x > x) slideX = -Math.round((downCoords.x - x)/(eventTime1-eventTime)*50);
            else if (downCoords.x < x) slideX = -Math.round((downCoords.x - x)/(eventTime1-eventTime)*50);
            else downCoords.x = 0;
            
            if (slideY !=0 || slideX != 0){
            	handler.postDelayed(updateTimeTask, 10);
            }	
			
			break; 
		}		
		prevX = x;
		prevY = y;
		return true; //super.onTouchEvent(event);
    }
	
	@SuppressLint("SimpleDateFormat")
	public void makeScreenShot(Bitmap bmp) {
		File pix = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		if (bmp == null)
			bmp = drawThread.getBitmap(); 
	    
	    Calendar c = Calendar.getInstance();
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    String strDate = sdf.format(c.getTime());	    
	    
	    String filename = pix+"/"+strDate+".png";
	    
	    FileOutputStream out = null;
	    try {
	        out = new FileOutputStream(filename);
	        bmp.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
	        // PNG is a lossless format, the compression factor (100) is ignored
	    } catch (Exception e) {
	        e.printStackTrace();
	    } finally {
	        try {
	            if (out != null) {
	                out.close();
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	            return;
	        }
	    }
	    
		Toast.makeText(mContext, "Screen saved to "+filename, Toast.LENGTH_LONG).show();
	}
	
	private void slide() {
		if (slideY > 0) slideY--;
		else if (slideY < 0) slideY++;
		
		if (slideX > 0) slideX--;
		else if (slideX < 0) slideX++;
		drawThread.move(slideX, slideY);
	}
	
    private Runnable updateTimeTask = new Runnable() { 
		   public void run() { 
			   slide();
			   handler.postDelayed(this, 10);
		       if (slideY == 0 && slideX == 0)
			   	   handler.removeCallbacks(updateTimeTask); 
		   } 
		};

	public boolean getGlasses() {
		if (drawThread != null) return drawThread.getGlasses();
		return false;
	}

	public float getScale() {
		return mScale;
	}

	public int getSliding() {
		return Math.max(Math.abs(dx), Math.abs(dy));
	}

	public void setSliding(int i) {
		dx = i;
		dy = i;
	}

	public DrawThread getDrawThread() {
		return drawThread;
	}

	public void setBitmaps(Bitmap original) {
		if (drawThread != null)
			if (drawThread.isTest())
				drawThread.setOriginalBitmap(
		    		Bitmap.createBitmap(
		    				BitmapFactory.decodeResource(getResources(),R.drawable.image), 0,0, mask.getWidth(), mask.getHeight()));
			else		
				drawThread.setOriginalBitmap(original);
	}
	
	public void setBitmapsMakeScreenShot(Bitmap bitmap, Bitmap bitmapMask) {
		setBitmaps(bitmapMask);
		drawThread.repaint(new Canvas(bitmap));
		makeScreenShot(bitmap);
		drawThread.setInformation("");
	}

	public void setMakePhoto(boolean  param){
		mMakePhoto = param;
	}
	
}
