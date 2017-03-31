package com.alexmochalov.kaleidoscope;

import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class MyCamera {

	private static Camera mCamera;
	private static Parameters parameters;
	private static Context mContext;
	private static Size previewSize;
	private static int mNumberOfResolution;
	private static Size pictureSize;
	
    private static boolean mFlashMode;
	private static int mFacing;
	
	private static final String FACING = "FACING";
	private static final String FLASH = "FLASH";
	private static final String RESOLUTION = "RESOLUTION";
	
	public static Camera getCamera(){
		return mCamera;
	}
	
 	@SuppressWarnings("deprecation")
	public static boolean openCamera(Context context){
 		if (mCamera != null)
 			return true;
 		
 		mContext = context;
 		
 		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		mFacing = prefs.getInt(FACING, 0);
		mFlashMode = prefs.getBoolean(FLASH, false);
		mNumberOfResolution = prefs.getInt(RESOLUTION, 0);
 		
		int numCameras= Camera.getNumberOfCameras();
		for(int i=0;i<numCameras;i++){
 		    Camera.CameraInfo info = new CameraInfo();
 		    Camera.getCameraInfo(i, info);
 		    if(mFacing == info.facing){
 	     	   mCamera = Camera.open(mFacing);
 		    }
 		}
 		
 		if (mCamera == null)
 	 		for(int i=0;i<numCameras;i++){
 	 		    Camera.CameraInfo info = new CameraInfo();
 	 		    Camera.getCameraInfo(i, info);
 	 		    mFacing = info.facing;
 	 	     	mCamera = Camera.open(mFacing);
 	 		}
 		
 		if (mCamera == null){
 			Toast.makeText(mContext, "Phone dosn't have any cameras.", Toast.LENGTH_LONG).show();
 			return false;
 		}
        
            parameters = mCamera.getParameters();
            previewSize = parameters.getPreviewSize();
            
            //if (mNumberOfResolution >= 0){
				List<Size> sizes = parameters.getSupportedPictureSizes();
				//mNumberOfResolution = Math.min(mNumberOfResolution, sizes.size()-1);
				mNumberOfResolution = sizes.size()-1;
				//mNumberOfResolution = 0;
				
				pictureSize = sizes.get(mNumberOfResolution);
				parameters.setPictureSize(pictureSize.width, pictureSize.height);
				mCamera.setParameters(parameters);
            //}
            
    		if (mFlashMode)
    			parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
    		else
    			parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
    		mCamera.setParameters(parameters);
            
        return true;
 	}
	
	public static String[] getSupportedPictureSizes(){
		if (parameters == null){
			String[] result = new String[1];
			return result;
		}
		
		List<Size> sizes = parameters.getSupportedPictureSizes();
		String[] result = new String[sizes.size()+1];
		
		int i = 1;
		for (Size size: sizes)
			result[i++] = ""+size.width+"x"+size.height; 
		return result;
	}
 	
	public static int getPictureSize() {
		return mNumberOfResolution;
	}
 	
	public static Size getPreviewSize() {
		return previewSize;
	}


	public static int getFacing() {
		return mFacing;
	}
	
	public static void setFlash(boolean flashMode) {
		mFlashMode = flashMode;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		Editor editor = prefs.edit();
		editor.putBoolean("FLASH", flashMode);
		editor.apply();
		
		if (flashMode)
			parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
		else
			parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
		mCamera.setParameters(parameters);
	}
	
	public static boolean getFlash() {
		return mFlashMode;
	}

	

	public static boolean setFacing(int facing) {
		mFacing = facing;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		Editor editor = prefs.edit();
		editor.putInt(FACING, facing);
		editor.apply();
		
 		if (mCamera != null){
 	        mCamera.stopPreview();
 	        mCamera.setPreviewCallback(null);  
            mCamera.release();  
            mCamera = null;

            openCamera(mContext);
            
            return true;
            
 		} else return false;
	}
	
	/*
	public static boolean setPictureSize(int numberOfResolution) {
		
		if (mNumberOfResolution == numberOfResolution)
			return false;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		Editor editor = prefs.edit();
		editor.putInt(RESOLUTION, numberOfResolution);
		editor.apply();
		
		mNumberOfResolution = numberOfResolution;
		if (numberOfResolution < 0) pictureSize = null;
		else {
			if (parameters != null){
				List<Size> sizes = parameters.getSupportedPictureSizes();
				pictureSize = sizes.get(numberOfResolution);
				parameters.setPictureSize(pictureSize.width, pictureSize.height);
				
				mCamera.stopPreview();
				mCamera.setParameters(parameters);
				mCamera.startPreview();
			}
		} 
		
		return true;
    	//////mSurfaceViewDrawable.setMakePhoto(numberOfResolution != 0);
	}
*/
	
}
