package com.alexmochalov.kaleidoscope;

import com.alexmochalov.kaleidoscope.SurfaceViewDrawable.TouchEventCallback;
import com.example.cameraexample.R;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera.CameraInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity  extends Activity   implements SensorEventListener
{

	private SensorManager sensorManager; 
	double ax,ay,az; // these are the acceleration in x,y and z axis
	
	private SurfaceViewCamera mSurfaceViewCamera;
	private SurfaceViewDrawable mSurfaceViewDrawable;
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.activity_main);

		MyCamera.openCamera(this);
        
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;

        final int flags = 
        		View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        

        //final int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
        
        // This work only for android 4.4+
        if(currentApiVersion >= Build.VERSION_CODES.KITKAT)
        {

            getWindow().getDecorView().setSystemUiVisibility(flags);

            // Code below is to handle presses of Volume up or Volume down.
            // Without this, after pressing volume buttons, the navigation bar will
            // show up and won't hide
            final View decorView = getWindow().getDecorView();
            decorView
                .setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener()
                {

                    @Override
                    public void onSystemUiVisibilityChange(int visibility)
                    {
                    	if((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0)
                        {
                            decorView.setSystemUiVisibility(flags);
                        }
                    }
                });
            
        }        

		mSurfaceViewCamera =  (SurfaceViewCamera)findViewById(R.id.SurfaceView01);
		mSurfaceViewDrawable =  (SurfaceViewDrawable)findViewById(R.id.SurfaceView02);
		mSurfaceViewCamera.setSurfaceViewDrawable(mSurfaceViewDrawable);
        
		mSurfaceViewDrawable.setScale(1);
		mSurfaceViewDrawable.touchEventCallback = new TouchEventCallback(){
			@Override
			public void callbackCall() {
				DialogSettings dialog = new DialogSettings(MainActivity.this, mSurfaceViewCamera, 
						mSurfaceViewDrawable, mSurfaceViewDrawable.getDrawThread() ); 
				dialog.paramChangedCallback  = new DialogSettings.
					ParamChangedCallback(){

					@Override
					public void paramChanged(String name)
					{
						// TODO: Implement this method
					}

					@Override
					public void closeApp()
					{
						finish();
					}
				};
				dialog.show();
			}

			@Override
			public void callbackPhoto() {
				mSurfaceViewCamera.makePhoto();
			}
		};


		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE); 
		sensorManager.registerListener(this, 
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 
				SensorManager.SENSOR_DELAY_NORMAL); //SENSOR_DELAY_GAME 
		
    }

    //@SuppressLint("NewApi")
    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        
		if(currentApiVersion >= Build.VERSION_CODES.KITKAT && hasFocus)
        {
            getWindow().getDecorView().setSystemUiVisibility(
            		View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
		
    }    
    
    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
		this.sensorManager.unregisterListener(this); 

    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.

		int id = item.getItemId();

		switch (item.getItemId()) {

		case R.id.action_settings: {
			//DialogSettings dialog = new DialogSettings(MainActivity.this); 
			//dialog.show();
			return true;
		}
		default:	return super.onOptionsItemSelected(item);
		}
		
	}
	
	double ax0,ay0; // these are the acceleration in x,y and z axis
	
	double last_x,last_y,last_z;
	long lastUpdate;
	private static final int SHAKE_THRESHOLD = 400;
	
	@Override
	public void onSensorChanged(SensorEvent event)
	{
		if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){ 
			ax=-event.values[0]; 
			ay=event.values[1]; 
			az=event.values[2];
			
			Scene.setAcceleration(ax, ay);
			//Var.gx = ax-ax0;
			//Var.gy = ay-ay0;
			
			ax0 = ax;
			ay0 = ay;
			
			long curTime = System.currentTimeMillis();
		    // only allow one update every 100ms.
		    if ((curTime - lastUpdate) > 100) {
		      long diffTime = (curTime - lastUpdate);
		      lastUpdate = curTime;

		      double speed = Math.abs(ax+ay+az - last_x - last_y - last_z) / diffTime * 10000;

		      if (speed > SHAKE_THRESHOLD) {
		    	  if (mSurfaceViewDrawable != null)
		    		  mSurfaceViewDrawable.getDrawThread().resetThread();
		    	  Log.d("sensor", "shake detected w/ speed: " + speed);
		    	  Toast.makeText(this, "shake detected w/ speed: " + speed, Toast.LENGTH_SHORT).show();
		      }
		      last_x = ax;
		      last_y = ay;
		      last_z = az;
		    }			
			
		}
	}

	@Override
	public void onAccuracyChanged(Sensor p1, int p2)
	{
		// TODO: Implement this method
	}

	
}
