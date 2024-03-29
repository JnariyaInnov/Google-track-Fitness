package com.blogspot.athletio;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import general.Day;
import general.SmallWorkout;
import general.Workout;
import services.FirebaseUploadService;
import stepdetector.StepDetector;
import storage.SharedPrefData;


///Tracks workout of running type
public class RunningTrackActivity  extends AppCompatActivity implements OnMapReadyCallback {

    final static int INTERVAL=5000;

    GoogleMap mgoogleMap;
    Vector<LatLng> latLngs=new Vector<LatLng>();
    LocationManager locationManager;
    android.location.LocationListener locationListener;

    double distanceCovered =0;
    int locationNumbersGot =0;
    boolean firstLocation =true, viewThreadRunning =true;
    long timeOfWorkoutInSecond =0;
    Date date;
    Thread viewThread;

    DatabaseReference mDatabase,mCurrentWorkoutDb;
    FirebaseAuth mAuth;


    TextView dataTextview;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running_track);

        mAuth=FirebaseAuth.getInstance();
        mDatabase= FirebaseDatabase.getInstance().getReference().child("workouts");
        mCurrentWorkoutDb=FirebaseDatabase.getInstance().getReference().child("currentworkouts");
        String key=mCurrentWorkoutDb.push().getKey();
        mCurrentWorkoutDb=mCurrentWorkoutDb.child(key);

        setupUI();
        date =new Date();

        if (gservicesAvailable()) {
            initMap();
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            ///Listener to get location after some interval and updates UI according to that
            locationListener = new android.location.LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    LatLng ll=new LatLng(location.getLatitude(),location.getLongitude());
                    if(firstLocation){
                        gotoloc(ll.latitude,ll.longitude,15);
                        firstLocation =false;
                        viewThread = new Thread() {
                            @Override
                            public void run() {
                                try {
                                    while (!isInterrupted()&& viewThreadRunning) {
                                        Thread.sleep(1000);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                timeOfWorkoutInSecond =new Date().getTime()- date.getTime();
                                                timeOfWorkoutInSecond /=1000;
                                                updateUI();
                                            }
                                        });
                                    }
                                } catch (InterruptedException e) {
                                }
                            }
                        };

                        viewThread.start();

                    }
                    else {
                        CameraUpdate update= CameraUpdateFactory.newLatLng(ll);
                        mgoogleMap.animateCamera(update);
                    }
                    latLngs.add(ll);
                    mCurrentWorkoutDb.setValue(new SmallWorkout(0,ll,mAuth.getCurrentUser().getUid(),mAuth.getCurrentUser().getDisplayName()));
                    mgoogleMap.clear();
                    MarkerOptions markerStart=new MarkerOptions().title("Start").position(latLngs.get(0));
                    mgoogleMap.addMarker(markerStart);
                    MarkerOptions markerEnd=new MarkerOptions().title("End").position(latLngs.get(latLngs.size()-1));
                    mgoogleMap.addMarker(markerEnd);
                    for (int i=0;i<latLngs.size()-1;i++){
                        PolylineOptions line=new PolylineOptions().add(latLngs.get(i),latLngs.get(i+1)).width(10).color(Color.BLUE);
                        mgoogleMap.addPolyline(line);
                    }
                    if(latLngs.size()>1){
                        LatLng last=latLngs.get(latLngs.size()-2);
                        distanceCovered +=haversine(ll.latitude,ll.longitude,last.latitude,last.longitude);
                    }
                    locationNumbersGot++;
                    if(locationNumbersGot >200){

                        startActivity(new Intent(RunningTrackActivity.this,TrackWorkoutMenuActivity.class));
                        finish();
                    }
                }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle) {

                }

                @Override
                public void onProviderEnabled(String s) {

                }

                @Override
                public void onProviderDisabled(String s) {

                }

            };
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            boolean isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network statusTextview
            boolean isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if(!isGPSEnabled){
                Toast.makeText(this,"Turn Location on",Toast.LENGTH_SHORT).show();
            }
            if(isNetworkEnabled)
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,INTERVAL,0,locationListener);

            else if(isGPSEnabled)
                locationManager.requestLocationUpdates("gps", INTERVAL, 0, locationListener);

        }
    }

    private void updateUI() {
        dataTextview.setText("Distance: "+Math.round(distanceCovered *100)/100+"m"+"\n"+"Time: "+ timeOfWorkoutInSecond);
    }

    private void setupUI() {
        dataTextview =(TextView)findViewById(R.id.running_track_layout_data_textview);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mgoogleMap=googleMap;
    }

    private void initMap() {
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.running_track_layout_map);
        mapFragment.getMapAsync(this);

    }

    public boolean gservicesAvailable() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int isavailable = api.isGooglePlayServicesAvailable(this);
        if (isavailable == ConnectionResult.SUCCESS) {
            return true;
        } else if (api.isUserResolvableError(isavailable)) {
            Dialog dialog = api.getErrorDialog(this, isavailable, 0);
            dialog.show();
        } else {
            Toast.makeText(this, "Could not connect to Play Services", Toast.LENGTH_LONG).show();
        }
        return false;

    }

    ///Updates camera view
    private void gotoloc(double lat, double lng, float zoom) {
        LatLng ll = new LatLng(lat, lng);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, zoom);
        mgoogleMap.moveCamera(update);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewThreadRunning =false;
        String key = mDatabase.push().getKey();
        SharedPreferences pref = RunningTrackActivity.this.getSharedPreferences(SharedPrefData.USERINFO, MODE_PRIVATE);
        Workout pushWorkout=new Workout(Workout.RUNTYPE, distanceCovered, timeOfWorkoutInSecond,pref.getInt(SharedPrefData.WEIGHT, 0),latLngs,new Day(), Calendar.getInstance().get(Calendar.HOUR_OF_DAY),Calendar.getInstance().get(Calendar.MINUTE));
        mDatabase.child(key).setValue(pushWorkout);
        locationManager.removeUpdates(locationListener);
        FirebaseDatabase.getInstance().getReference().child("Users").child(mAuth.getCurrentUser().getUid()).child("workouts").child(key).setValue(key);
        mCurrentWorkoutDb.setValue(null);

    }

    ///Returns distance between two latlng points
    public double haversine(
            double lat1, double lng1, double lat2, double lng2) {
        int r = 6371; // average radius of the earth in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = r * c*1000;
        return d;
    }
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_home:
                startActivity(new Intent(this,MainActivity.class));
                finish();
                return true;
            case R.id.menu_track_workout:
                startActivity(new Intent(this, TrackWorkoutMenuActivity.class));
                finish();
                return true;
            case R.id.menu_online_workout:
                startActivity(new Intent(this, OnlineWorkoutActivity.class));
                finish();
                return true;
            case R.id.menu_my_workouts:
                startActivity(new Intent(this, MyWorkoutsActivity.class));
                finish();
                return true;
            case R.id.menu_excersices:
                startActivity(new Intent(this, ExercisesActivity.class));
                finish();
                return true;
            case R.id.menu_social:
                startActivity(new Intent(this, SocialMainActivity.class));
                finish();
                return true;
            case R.id.menu_events:
                startActivity(new Intent(this, EventsActivity.class));
                finish();
                return true;
            case R.id.menu_event_reminder:
                startActivity(new Intent(this, ShowEventRemindersActivity.class));
                finish();
                return true;
            case R.id.menu_create_event:
                startActivity(new Intent(this, CreateEventActivity.class));
                finish();
                return true;
            case R.id.menu_nearby_place:
                startActivity(new Intent(this, MapsActivity.class));
                finish();
                return true;
            case R.id.menu_chat_bot:
                startActivity(new Intent(this, ChatBotMain.class));
                finish();
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
                return true;
            case R.id.menu_signout:
                signOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    void signOut(){
        SharedPrefData sharedPrefData=new SharedPrefData(this);
        sharedPrefData.clear();
        Intent intent=new Intent(this, FirebaseUploadService.class);
        stopService(intent);

        Intent intent2=new Intent(this, StepDetector.class);
        stopService(intent2);
        FirebaseAuth.getInstance().signOut();
    }
}
