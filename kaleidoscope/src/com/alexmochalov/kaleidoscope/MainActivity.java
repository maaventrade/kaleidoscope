package com.alexmochalov.kaleidoscope;

import android.app.*;
import android.content.pm.*;
import android.content.res.*;
import android.hardware.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.alexmochalov.kaleidoscope.*;
import com.alexmochalov.kaleidoscope.SurfaceViewDrawable.*;

public class MainActivity  extends Activity   implements SensorEventListener
{

	private SensorManager sensorManager; 
	double ax,ay,az; // these are the acceleration in x,y and z axis
	
	private SurfaceViewCamera mSurfaceViewCamera;
	private SurfaceViewDrawable mSurfaceViewDrawable;
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		// Checks the orientation of the screen
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
			Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
		}
	}    
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

       // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
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
		    	 // Log.d("sensor", "shake detected w/ speed: " + speed);
		    	 // Toast.makeText(this, "shake detected w/ speed: " + speed, Toast.LENGTH_SHORT).show();
		      }
		      last_x = ax;
		      last_y = ay;
		      last_z = az;
		    }			
		}
		float X_Axis = event.values[0]; 
        float Y_Axis = event.values[1]; 
		
		if (mSurfaceViewDrawable.getDrawThread() != null){
			mSurfaceViewDrawable.getDrawThread().setAngle(X_Axis, 
							Y_Axis);
		}
			
/*
        if((X_Axis <= 6 && X_Axis >= -6) && Y_Axis > 5){
			isLandscape = false; 
        }
        else if(X_Axis >= 6 || X_Axis <= -6){
			isLandscape = true;
        }
*/
		
		
		
	}

	@Override
	public void onAccuracyChanged(Sensor p1, int p2)
	{
		// TODO: Implement this method
	}

	
}

/*


float[] g = new float[3]; 
g = event.values.clone();

double norm_Of_g = Math.sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2]);

// Normalize the accelerometer vector
g[0] = g[0] / norm_Of_g
g[1] = g[1] / norm_Of_g
g[2] = g[2] / norm_Of_g
Then the inclination can be calculated as

int inclination = (int) Math.round(Math.toDegrees(Math.acos(g[2])));
Thus

if (inclination < 25 || inclination > 155)
{
    // device is flat
}
else
{
    // device is not flat
}
For the case of laying flat, you have to use a compass to see how much the device is rotating from the starting position.

For the case of not flat, the rotation (tilt) is calculated as follow

int rotation = (int) Math.round(Math.toDegrees(Math.atan2(g[0], g[1])));
Now rotation = 0 means the device is in normal position. That is portrait witho


*/
