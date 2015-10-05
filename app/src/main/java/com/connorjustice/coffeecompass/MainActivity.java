package com.connorjustice.coffeecompass;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private ImageView mPointer;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    private float mCurrentDegree = 0f;
    private View rectView;
    private double coffeeLat;
    private double coffeeLong;
    private static final String TAG = MainActivity.class.getSimpleName();
    // Acquire a reference to the system Location Manager
    LocationManager locationManager;
    private TextView txtV;
    LocationListener locationListener;
    private boolean locked = false;
    private float dist;
    Location[] coffeeLocations = initializeCoffeeLocations();
    private int destLoc;
    private final float MAXDIST = 700.0f;
    private float bearingToCoffee;
    private float declination = 346.16f;
    private float headingToCoffee;

    private class MyLocationListener implements LocationListener{
        public void onLocationChanged(Location location) {
            // Called when a new location is found by the network location provider.
            updateProximityIndicator(location);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {}

        public void onProviderEnabled(String provider) {}

        public void onProviderDisabled(String provider) {}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mPointer = (ImageView) findViewById(R.id.pointer);
        txtV = (TextView) findViewById(R.id.myAwesomeTextView);
        rectView = findViewById(R.id.myRectangleView);
        // Register the listener with the Location Manager to receive location updates
        locationManager = (LocationManager)
                this.getSystemService(Context.LOCATION_SERVICE);
        // Define a listener that responds to location updates
        locationListener = new MyLocationListener();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                0, 0, locationListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
        locationManager.removeUpdates(locationListener);
    }

    private Location[] initializeCoffeeLocations() {
        Location[] locs = new Location[6];
        locs[0] = new Location("");
        locs[0].setLatitude(41.55736);
        locs[0].setLongitude(-72.65060);
        locs[1] = new Location("");
        locs[1].setLatitude(41.55327);
        locs[1].setLongitude(-72.65755);
        locs[2] = new Location("");
        locs[2].setLatitude(41.55679);
        locs[2].setLongitude(-72.65683);
        locs[3] = new Location("");
        locs[3].setLatitude(41.55426);
        locs[3].setLongitude(-72.65590);
        locs[4] = new Location("");
        locs[4].setLatitude(41.55222);
        locs[4].setLongitude(-72.65514);
        locs[5] = new Location("");
        locs[5].setLatitude(41.55290);
        locs[5].setLongitude(-72.66024);
        return locs;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //Smooth the output from the sensors using a low-pass filter
    //**IDEA: USE GYROSCOPE FOR ADDITIONAL STABILITY**//
    private float[] exponentialSmoothing( float[] input, float[] output, float alpha ) {
        if ( output == null )
            return input;
        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + alpha * (input[i] - output[i]);
        }
        return output;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == mAccelerometer) {
            exponentialSmoothing(event.values, mLastAccelerometer, 0.5f);
            //System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor == mMagnetometer) {
            exponentialSmoothing(event.values, mLastMagnetometer, 0.5f);
            //System.arraycopy(event.values, 0, mLastMagnetometer, 0, event. values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastMagnetometerSet && mLastAccelerometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);
            float azimuthInRadians = mOrientation[0];
            //Azimuth is a positive float between 0 and 360
            headingToCoffee = (float) (Math.toDegrees(azimuthInRadians)+declination
                    - bearingToCoffee);
            headingToCoffee = (headingToCoffee +180)% 360;
            RotateAnimation ra = new RotateAnimation(mCurrentDegree, -headingToCoffee,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
//            txtV.setText("Location: " + destLoc + "Distance: " + dist +
//                    "Azimuth: " + azimuthInDegrees + " Heading: " + headingToCoffee);
            ra.setDuration(250);
            ra.setFillAfter(true);
            mPointer.startAnimation(ra);
            mCurrentDegree = -headingToCoffee;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private String matchNumWithName() {
        if (destLoc == 0) {
            return "Red and Black";
        } else if (destLoc == 1) {
            return "Pi Cafe";
        } else if (destLoc == 2) {
            return "Usdan Center";
        } else if (destLoc == 3) {
            return "Espwesso";
        } else if (destLoc == 5) {
            return "Neon Deli";
        } else if (destLoc == 4) {
            return "Summerfields";
        } else {
            return "Something got screwed up";
        }
    }
    private int gradientColor(double dist) {
        double H;
        if (dist > MAXDIST) {
            H = 1.0;
        } else {
            H = 360 * (dist / MAXDIST);
        }
        double S = 0.9; // Saturation
        double B = 0.9; // Brightness
        return Color.HSVToColor(new float[]{(float)H, (float)S, (float)B});
    }

    public void updateProximityIndicator(Location location) {
        float tempDist;
        if (!(locked) && (location != null)) {
            for (int i = 0; i < coffeeLocations.length; i++) {
                tempDist = coffeeLocations[i].distanceTo(location);
                if (tempDist < dist || dist == 0.0f) {
                    dist = tempDist;
                    destLoc = i;
                }
            }
            bearingToCoffee = coffeeLocations[destLoc].bearingTo(location);
            locked = true;
            txtV.setText(matchNumWithName());
        } else if (locked && (location != null)) {
            dist = location.distanceTo(coffeeLocations[destLoc]);
            if (dist < 20.0) {
                Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(700);
            }
            rectView.setBackgroundColor(gradientColor(dist));
            bearingToCoffee = coffeeLocations[destLoc].bearingTo(location);
        }
    }

}
