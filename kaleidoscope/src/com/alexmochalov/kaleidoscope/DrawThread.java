package com.alexmochalov.kaleidoscope;

import java.util.ArrayList;

import com.alexmochalov.kaleidoscope.R;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class DrawThread extends Thread{
	
	private Context mContext;
	private static final String SHOWSHADOW = "SHOWSHADOW";
	private final String GLASSES = "GLASSES";
	private final String SHOWICONPHOTO = "SHOWICONPHOTO";
	
	private Bitmap mMask = null;
	private int maskWidth;
	private int maskHeight;
	private Bitmap bitmapShadow;
	
	private Bitmap chip = null;
	private Rect rectChip = null;

//	private Bitmap bitmapCamera;
//	private Rect rectCameraSrc;
//	private Rect rectCameraDst;
	
//	private int leftChip = 0;
	private Canvas canvasChip = null;
	
	private float deltaX = 0;
	private float deltaY = 0;
	
	private Bitmap mOriginal = null;
    private Bitmap background = null;
    private Canvas canvasBackground;
	
	private Rect mRectCameraDst;
    
	private SurfaceHolder mSurfaceHolder;
	private Paint paint;
	
	private boolean runFlag = false;
	private boolean mTest = false;
	
	private int mWidth = 0;
	private int mHeight = 0;
	
	private boolean mGlasses = false;
	private Bitmap glass = null;
	
	private boolean mShowIconPhoto;
	private boolean mShowShadow = true;
	
	private Bitmap bitmapCamera;
	private Rect rectCameraSrc;
	
	private ChipsThread chipsThread;
	
	private Paint paintButton = new Paint(Paint.ANTI_ALIAS_FLAG);
	
    private ArrayList<Sprite> objects = new ArrayList<Sprite>();
	
	private Handler handlerCalcSprites = new Handler();
	
	private String mInformation = "";
	private Rect rectInformation = null;
	private Paint paintInformation = new Paint(Paint.ANTI_ALIAS_FLAG);
	private boolean init = true;
	
	private int mAngle = 0;
	private float mAngleFloat = 0;

    public DrawThread(Context context, SurfaceHolder surfaceHolder){
    	mContext = context;
        mSurfaceHolder = surfaceHolder;
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        mShowShadow = prefs.getBoolean("SHOWSHADOW", false);
		setGlasses(prefs.getBoolean(GLASSES, false));
		
		mShowIconPhoto = prefs.getBoolean("SHOWICONPHOTO", false);
        
         
        bitmapCamera = BitmapFactory.decodeResource(context.getResources(), R.drawable.btn_camera);
        rectCameraSrc = new Rect(0,0, bitmapCamera.getWidth(), bitmapCamera.getHeight());
        paintButton.setAlpha(200);
        	
		setInformation(context.getResources().getString(R.string.init));
        //if (chipsThread != null)
        //	chipsThread.setDrawThread(drawThread);
        
    }
    
    
    public void onStop(){
    	
		handlerCalcSprites.removeCallbacks(updateTimeTaskCalcSprites);
    	
    	Boolean retry = true;
        // finish the thread wirking
        if (chipsThread != null){
            chipsThread.setRunning(false);
            while (retry) {
                try {
                	chipsThread.join();
                    retry = false;
                } catch (InterruptedException e) {
                    // try again ang again
                }
            }
        }
    }
	
	public void setParams(int width, int height, Rect rectCameraDst)
	{
		mWidth = width;
		mHeight = height;
		
        mRectCameraDst = rectCameraDst; 
        
        //bitmapShadow = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.shadow);
        
        bitmapShadow = makeShadow(width, height);
	}

	private Bitmap makeShadow(int width, int height){
        Bitmap bitmapShadow = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas tempCanvas = new Canvas(bitmapShadow);
        
        tempCanvas.drawColor(Color.BLACK);
        
        Paint p = new Paint();
        
        int[] colors = {0xFF000000, 0x00000000}; 
        float[] stops = {0.4f, 0.5f};
        
        RadialGradient gradient = new android.graphics.RadialGradient(
                width/2, height/2,
                width, colors , stops ,
                android.graphics.Shader.TileMode.CLAMP);

        p = new Paint();
        p.setShader(gradient);
        p.setColor(0xFF000000);
        p.setXfermode(new PorterDuffXfermode(Mode.DST_OUT));
        tempCanvas.drawCircle(width/2, height/2,
        		width, p);  
        
		return bitmapShadow;
	}
	

	public int adjustAlpha(int color, float factor) {
	    int alpha = Math.round(Color.alpha(color) * factor);
	    //int alpha = Color.alpha(color);
		//if (alpha > 0)
	    //	alpha = alpha-2;
	    Log.d("MY", "alpha "+alpha);
	    int red = Color.red(color);
	    int green = Color.green(color);
	    int blue = Color.blue(color);
	    
	    
	    return Color.argb(alpha, red, green, blue);
	}	
    
	public synchronized void setGlassBitmap(Bitmap glass) {
		this.glass = glass;
		if (glass == null)
			mGlasses = false;
		else mGlasses = true;
	}
    
    /**
     * 
     * @param original - bitmap with the original image
     * @param mask - the mask for cropping the original image
     * This method crops a part from the center of the original image. We suggest what the mask is smaller when the original image. 
     * 
     */
	public synchronized void bitmapsToChips() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	    
        if (glass != null){
        	background = Bitmap.createBitmap(maskWidth, maskHeight, Config.ARGB_8888 );
			canvasBackground = new Canvas(background);
			
			if (mOriginal != null){
				canvasBackground.drawBitmap(mOriginal, 0, 0, null);
			}
			
			paint.setColor(Color.DKGRAY);
			paint.setAlpha(220);
			
			canvasBackground.drawRect(0,0,maskWidth, maskHeight, paint);
			paint.setAlpha(255);

        	int n = (int) (maskHeight/1.5f);
        	canvasBackground.drawBitmap(glass, 
					   new Rect(0,
								  0, 
								  glass.getWidth(), 
								  glass.getHeight()),											  
					   new Rect(maskWidth/2-n,
					  maskHeight/2-n, 
					  maskWidth/2+n, 
					  maskHeight/2+n),
					  paint);
											  
		} else if (mOriginal == null) return;
			else background = mOriginal;
			
        
        canvasChip.drawARGB(0, 0, 0, 0);
        
        rectChip = new Rect(0, 0, chip.getWidth(), chip.getHeight());
        
        final Paint paint = new Paint();
        
        canvasChip.drawBitmap(mMask, rectChip, rectChip, null);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvasChip.drawBitmap(background, rectChip, rectChip, paint);

        //setInformation("");

        
		//0 1 2
		//3 4 5
        
	}
    
    @Override
    public void run() {
        Canvas canvas;
        
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.RED);
		paint.setTextSize(32);

        while (runFlag) {
            canvas = null;
            try {
                // get the Canvas and start drawing
                canvas = mSurfaceHolder.lockCanvas(null);
                synchronized (mSurfaceHolder) {
                	if (canvas != null){
                		if (mTest)
                			drawTest(canvas, mWidth, mHeight, false);
                		else {
                			draw(canvas, mWidth, mHeight, false);
                		}
                	}
                }
            } 
            finally {
                if (canvas != null) {
                    // drawing is finished
                	mSurfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
     	   //try {
       	   // sleep(10);
       	   //} catch (InterruptedException e) {
       	    // TODO Auto-generated catch block
       	   // e.printStackTrace();
       	   //}
        }
    }
    
    private synchronized void draw(Canvas canvas, int width, int height, boolean hideButton){
    	bitmapsToChips();
		
    	if (chip == null){
			canvas.save();
			canvas.rotate(mAngle, mWidth >> 1, mHeight >> 1);
			paintInfo(canvas);
			canvas.restore();
			return;
		}

    	if (init){
			canvas.save();
			canvas.rotate(mAngle, mWidth >> 1, mHeight >> 1);
			paintInfo(canvas);
			canvas.restore();
			return;
		}

		int dx = 0;
		for (int y = (int)(deltaY); y <= height; y = y + maskHeight){
			for (int x = (int)(deltaX) + dx; x <= width; x = (int)(x + maskWidth*3f)-1){
				canvas.save(Canvas.MATRIX_SAVE_FLAG);
				canvas.drawBitmap(chip, rectChip, new Rect(x, y, x+maskWidth, y+maskHeight), paint);

				canvas.rotate(-60, x+maskWidth, y+maskHeight);
				canvas.scale(-1f, 1f, x+maskWidth, y+maskHeight);
				
				canvas.drawBitmap(chip, rectChip, new Rect(x, y, x+maskWidth, y+maskHeight), paint);
				
				canvas.scale(-1f, 1f, x+maskWidth, y+maskHeight);
				canvas.rotate(-180, x+maskWidth, y+maskHeight);
				
				canvas.drawBitmap(chip, rectChip, new Rect(x, y, x+maskWidth, y+maskHeight), paint);
				
				canvas.scale(-1f, 1f, x+maskWidth, y+maskHeight);
				canvas.rotate(-60, x+maskWidth, y+maskHeight);
				canvas.drawBitmap(chip, rectChip, new Rect(x, y, x+maskWidth, y+maskHeight), paint);
				
				canvas.scale(-1f, 1f, x+maskWidth, y+maskHeight);
				canvas.rotate(60, x+maskWidth, y+maskHeight);
				canvas.drawBitmap(chip, rectChip, new Rect(x, y, x+maskWidth, y+maskHeight), paint);
				
				canvas.scale(1f, -1f, x+maskWidth, y+maskHeight);
				canvas.drawBitmap(chip, rectChip, new Rect(x, y, x+maskWidth, y+maskHeight), paint);
						
				canvas.restore(); 
			}
			if (dx == 0)
				dx = (int)(dx + maskWidth*1.5f);
			else	
				dx = 0;
		}
		
		if (mShowShadow && bitmapShadow != null)
			canvas.drawBitmap(bitmapShadow,0, 0, null);
		
		if (! hideButton){
			if (mShowIconPhoto && bitmapCamera != null){
				
				canvas.save();
				canvas.rotate(mAngleFloat, 
						mRectCameraDst.left + (mRectCameraDst.width() >> 1), 
						mRectCameraDst.top + (mRectCameraDst.height() >> 1));
				
				canvas.drawBitmap(bitmapCamera, rectCameraSrc, mRectCameraDst, paintButton);
				canvas.restore();
				
			}
		}
		
		canvas.save();
		canvas.rotate(mAngle, mWidth >> 1, mHeight >> 1);
		paintInfo(canvas);
		canvas.restore();
		
    }

	private void paintInfo(Canvas canvas)
	{
		if (mInformation.length() != 0){
			
			if (rectInformation == null){
				Rect bounds = new Rect();
				paintInformation.getTextBounds("xx"+mInformation, 0, ("xx"+mInformation).length(), bounds);
				
				rectInformation = new Rect((mWidth - bounds.width()) >> 1, 
						   (mHeight >> 1)- bounds.height(),
						   (mWidth + bounds.width()) >> 1, 
						   (mHeight >> 1) + bounds.height());
			}
			
			paintInformation.setColor(Color.WHITE);
			canvas.drawRect(rectInformation, paintInformation);

			paintInformation.setColor(Color.BLACK);
			canvas.drawText(mInformation, mWidth >> 1, 
							(mHeight >> 1) + paintInformation.descent(), paintInformation);

		}
	}

	public void setInformation(String information) {
		mInformation = information;
		if (mInformation.length() > 0){
			
			paintInformation.setTextSize(60);
			paintInformation.setTextAlign(Align.CENTER);

			if (mWidth > 0){
				Rect bounds = new Rect();
				paintInformation.getTextBounds("xx"+mInformation, 0, ("xx"+mInformation).length(), bounds);
				rectInformation = new Rect((mWidth - bounds.width()) >> 1, 
										   (mHeight >> 1)- bounds.height(),
										   (mWidth + bounds.width()) >> 1, 
										   (mHeight >> 1) + bounds.height());
			}
		}
		
	}
    
	public Bitmap getBitmap() {
		Bitmap bmp = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		draw(new Canvas(bmp), mWidth, mHeight, true);
		return bmp;
	}
	
    private synchronized void drawTest(Canvas canvas, int width, int height, boolean hideButton){
    	bitmapsToChips();
    	
    	if (chip == null) 
    		return;
    	
    	int x = 10;
    	int y = 10;
		canvas.drawBitmap(chip, rectChip, new Rect(x, y, x+1000, y+1000), paint);
   }
    
    public boolean isTest(){
    	return mTest;
    }

    public void setTest(boolean test){
    	mTest = test;
    }
    
	public void setRunning(boolean b) {
		runFlag = b;
	}

	public synchronized void setOriginalBitmap(Bitmap original) {
		mOriginal = original;
	}
	
	public void setMask(Bitmap mask)
	{
		mMask = mask;

		maskWidth = mask.getWidth();
		maskHeight = mask.getHeight();

        chip = Bitmap.createBitmap(maskWidth, maskHeight,  Config.ARGB_8888);
        canvasChip = new Canvas(chip);
        
		deltaX = -maskWidth/2;
		deltaY = -maskHeight;
	}

	public void setShowShadow(boolean showShadow) {
		mShowShadow = showShadow;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		Editor editor = prefs.edit();
		editor.putBoolean("SHOWSHADOW", mShowShadow);
		editor.apply();
	}

	public synchronized void  move(float dX, float dY)
	{
		deltaX += dX;
		deltaY += dY;
		
		if (deltaY > -maskHeight){
			deltaY = -maskHeight*3;
		}
		else 
			if (deltaY < -maskHeight*3){
			deltaY = -maskHeight;
	
		} 
		
		if (deltaX >  -maskWidth/2){
			deltaX = -maskWidth*3.5f;
		} else
			if (deltaX < -maskWidth*3.5f){
				deltaX = -maskWidth/2;
		}
		
	}

	public boolean getGlasses() {
		return mGlasses;
	}

	public void setGlasses(boolean glasses) {
		mGlasses = glasses;
		
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			Editor editor = prefs.edit();
			editor.putBoolean(GLASSES, glasses);
			editor.apply();
			
			if (glasses){
		    	chipsThread = new ChipsThread(mContext, mSurfaceHolder, this, objects);
		    	chipsThread.setRunning(true);
		    	chipsThread.start();
		    	
		    	handlerCalcSprites.postDelayed(updateTimeTaskCalcSprites, 10);
		    	
			} else {
				if (chipsThread != null)
					chipsThread.setRunning(false);
				handlerCalcSprites.removeCallbacks(updateTimeTaskCalcSprites);
			}
	}

	public boolean getShowShadow() {
		return mShowShadow;
	}

    public void resetThread(){
        if (chipsThread != null)
        	chipsThread.reset();
    }
	
	private Runnable updateTimeTaskCalcSprites = new Runnable() { 
		public void run() { 
			for (Sprite i : objects)
				if (i != null)
					i.calc();
			handlerCalcSprites.postDelayed(this, 5);
		} 
	};        

	public boolean getShowIconPhoto() {
		return mShowIconPhoto;
	}

	public void setShowIconPhoto(boolean showIconPhoto) {		
		mShowIconPhoto = showIconPhoto;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		Editor editor = prefs.edit();
		editor.putBoolean("SHOWICONPHOTO", mShowIconPhoto);
		editor.apply();

	}
	
	private synchronized void draw1(Canvas canvas, int width, int height){
    	if (chip == null) return;

		int dx = 0;
		for (int y = (int)(deltaY); y <= height; y = y + maskHeight){
			for (int x = (int)(deltaX) + dx; x <= width; x = (int)(x + maskWidth*3f)){
				canvas.save(Canvas.MATRIX_SAVE_FLAG);
		
				canvas.drawBitmap(chip, rectChip, new Rect(x, y, x+maskWidth, y+maskHeight), paint);

				canvas.rotate(-60, x+maskWidth, y+maskHeight);
				canvas.scale(-1f, 1f, x+maskWidth, y+maskHeight);
				
				canvas.drawBitmap(chip, rectChip, new Rect(x, y, x+maskWidth, y+maskHeight), paint);
				
				canvas.scale(-1f, 1f, x+maskWidth, y+maskHeight);
				canvas.rotate(-180, x+maskWidth, y+maskHeight);
				
				canvas.drawBitmap(chip, rectChip, new Rect(x, y, x+maskWidth, y+maskHeight), paint);
				
				canvas.scale(-1f, 1f, x+maskWidth, y+maskHeight);
				canvas.rotate(-60, x+maskWidth, y+maskHeight);
				canvas.drawBitmap(chip, rectChip, new Rect(x, y, x+maskWidth, y+maskHeight), paint);
				
				canvas.scale(-1f, 1f, x+maskWidth, y+maskHeight);
				canvas.rotate(60, x+maskWidth, y+maskHeight);
				canvas.drawBitmap(chip, rectChip, new Rect(x, y, x+maskWidth, y+maskHeight), paint);
				
				canvas.scale(1f, -1f, x+maskWidth, y+maskHeight);
				canvas.drawBitmap(chip, rectChip, new Rect(x, y, x+maskWidth, y+maskHeight), paint);
						
				canvas.restore(); 
			}
			if (dx == 0)
				dx = (int)(dx + maskWidth*1.5f-2);
			else	
				dx = 0;
		}
    	
		if (mShowShadow){
			float radius = Math.min(width, height);
	        Paint p = new Paint();
	        
	        int[] colors = {0x00000000, 0xFF000000}; 
	        float[] stops = {0.4f, 1f};
	        
	        RadialGradient gradient = new android.graphics.RadialGradient(
	                width/2, height/2,
	                radius, colors , stops ,
	                android.graphics.Shader.TileMode.CLAMP);

	        p.setShader(gradient);
	        p.setColor(0xff000000);
	        
	        canvas.drawCircle(width/2, height/2,
	        		radius, p);
		}
    }
	
	public void repaint(Canvas canvas) {
		draw1(canvas, canvas.getWidth(), canvas.getHeight());
	}


	public void setAngle(float ax, float ay){
		//setInformation(
		//	""+ax+"   "+ay);

		if (Math.abs(ay) > 1 || Math.abs(ax) > 1){
			if (ax > ay && ax > 0)
				mAngle = 0;
			else if (ay > ax && ay > 0)
				mAngle = -90;
			else if (ax < ay && ax < 0)
				mAngle = 180;
			else if (ay < ax && ay < 0)
				mAngle = 90;
		}
		
		mAngleFloat =  (float) (Math.atan2(ax, ay)/(Math.PI/180)) - 90;
		
	}
	
	public void clearInformation() {
		if (init){
			init  = false;
			mInformation = "";
		}
	}
	
}
