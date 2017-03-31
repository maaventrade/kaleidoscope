package com.alexmochalov.kaleidoscope;

import java.io.IOException;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;

public class SurfaceViewCamera extends SurfaceView implements Callback, PreviewCallback, Camera.PictureCallback, Camera.AutoFocusCallback {

	private Context mContext;
    // This surfaceHolder is linked with the Camera
    private SurfaceHolder surfaceHolder;
    // We pass picture from the Camera to this SurfaceView
	private SurfaceViewDrawable mSurfaceViewDrawable;

    private boolean makePhoto;
    
	public void setSurfaceViewDrawable(SurfaceViewDrawable surfaceViewDrawable){
		mSurfaceViewDrawable = surfaceViewDrawable;
	}
	
    private void init(Context context){
    	mContext = context;
    	
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    
    
	public SurfaceViewCamera(Context context, AttributeSet attrs,
			int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init(context);
	}

	public SurfaceViewCamera(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	public SurfaceViewCamera(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
        try
        {
            MyCamera.getCamera().setPreviewDisplay(holder);
            MyCamera.getCamera().setPreviewCallback(this);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        Size previewSize = MyCamera.getCamera().getParameters().getPreviewSize();
        float aspect = (float) previewSize.width / previewSize.height;

        int previewSurfaceWidth = getWidth();
        int previewSurfaceHeight = getHeight();

        LayoutParams lp = getLayoutParams();

        // здесь корректируем размер отображаемого preview, чтобы не было искажений

        if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)
        {
            // портретный вид
        	MyCamera.getCamera().setDisplayOrientation(90);
            lp.height = previewSurfaceHeight;
            lp.width = (int) (previewSurfaceHeight / aspect);
            ;
        }
        else
        {
            // ландшафтный
        	MyCamera.getCamera().setDisplayOrientation(0);
            lp.width = previewSurfaceWidth;
            lp.height = (int) (previewSurfaceWidth / aspect);
        }

        setLayoutParams(lp);
        MyCamera.getCamera().startPreview();
	}

	public void makePhoto() {
		makePhoto = true;
		MyCamera.getCamera().autoFocus(this);
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		
	}

	@SuppressWarnings("deprecation")
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		
        if (MyCamera.getCamera() != null)
        {
        	MyCamera.getCamera().setPreviewCallback(null);
        	MyCamera.getCamera().stopPreview();
        	//mCamera.release();
        	//mCamera = null;
        }
        
        
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		
		Size previewSize = MyCamera.getPreviewSize();
		
    	YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21,  previewSize.width, previewSize.height, null);
    	mSurfaceViewDrawable.setBitmaps(yuvImage, previewSize);
	}

	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		
		int scale = 1;
		
    	BitmapFactory.Options options = new BitmapFactory.Options();
    	options.inMutable = true;
    	Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
    	
    	int left = (int)(bitmap.getWidth()-mSurfaceViewDrawable.maskWidth * scale)/2;
    	int top = (int)(bitmap.getHeight()-mSurfaceViewDrawable.maskHeight*scale)/2;
    	int right = (int)(left+mSurfaceViewDrawable.maskWidth*scale);
    	int bottom = (int)(top+mSurfaceViewDrawable.maskHeight*scale);
    	
    	Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, left, top, right-left, bottom-top);
    	
		mSurfaceViewDrawable.setBitmapsMakeScreenShot(bitmap, croppedBitmap);

		MyCamera.getCamera().startPreview();		
	}

    @Override
    public void onAutoFocus(boolean success, Camera paramCamera)
    {
		if (makePhoto && success){
			MyCamera.getCamera().takePicture(null, null, null, this);
		}
    }

	public void setFacing(int facing) {
		if (MyCamera.setFacing(facing)){
            try
            {
            	MyCamera.getCamera().setPreviewDisplay(surfaceHolder);
            	MyCamera.getCamera().setPreviewCallback(this);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            MyCamera.getCamera().startPreview();
		}
		
	}
	
}
