package tech.loksuvidha.finalgeofire;

import android.*;
import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar;

import java.util.Random;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
     GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener{

    private GoogleMap mMap;

     public static final int MY_PERMISSION_REQUEST_CODE = 7192;
     public static final int PLAY_SERVICES_RESOLUTION_REQUEST = 8192;
     private LocationRequest mLocationRequest;
     private GoogleApiClient mGoogleApiClient;
     private Location mLastLocation;

     private static int UPDATE_INTERVAL = 5000;
     private static int FASTEST_INTERVAL = 3000;
     private static int DISPLACEMENT = 10;

     DatabaseReference ref;
     GeoFire geoFire;
     Marker mCurrent;
    VerticalSeekBar mSeekBar;





     @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        ref = FirebaseDatabase.getInstance().getReference("MyLocation");

        geoFire = new GeoFire(ref);

        mSeekBar = (VerticalSeekBar)findViewById(R.id.verticalSeekBar);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mMap.animateCamera(CameraUpdateFactory.zoomTo(i),2000,null);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        setUpLocation();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case MY_PERMISSION_REQUEST_CODE:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    if (checkPlayServices())
                    {
                        buildGoogleApiClient();
                        createLocationRequest();
                        displayLocation();
                    }
                }
                break;
        }
    }

    private void setUpLocation() {
         if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                 ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
         {
             ActivityCompat.requestPermissions(this, new String[]{
                     android.Manifest.permission.ACCESS_COARSE_LOCATION,
                     android.Manifest.permission.ACCESS_FINE_LOCATION
             },MY_PERMISSION_REQUEST_CODE);
         }
         else
         {
             if (checkPlayServices())
             {
                 buildGoogleApiClient();
                 createLocationRequest();
                 displayLocation();
             }
         }
     }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(mLastLocation != null)
        {
            final double latitude = mLastLocation.getLatitude();
            final double longitude = mLastLocation.getLongitude();

            geoFire.setLocation("You", new GeoLocation(latitude, longitude),
                    new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {

                            //Add Marker
                            if (mCurrent != null)
                                mCurrent.remove();
                            mCurrent = mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(latitude,longitude))
                                    .title("You"));
                            //Move camera to this location
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,longitude),12.0f));
                        }
                    });


            Log.d("Chinmay",String.format("Your location was change : %f %f",latitude, longitude));
        }
        else
        {
            Log.d("Chinmay",String.format("Cannot detect your location!"));
        }
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);

    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    private boolean checkPlayServices() {
         int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
         if(resultCode != ConnectionResult.SUCCESS)
         {
             if(GooglePlayServicesUtil.isUserRecoverableError(resultCode))
                 GooglePlayServicesUtil.getErrorDialog(resultCode,this,PLAY_SERVICES_RESOLUTION_REQUEST).show();
             else
             {
                 Toast.makeText(this, "This device is not supported", Toast.LENGTH_SHORT).show();
                 finish();
             }
             return false;

         }
         return true;
     }


     @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //create fence
         LatLng fence = new LatLng(21.1232000,79.0381680);

         mMap.addCircle(new CircleOptions()
         .center(fence)
         .radius(1000)
         .strokeColor(Color.BLUE)
         .fillColor(0x220000FF)
         .strokeWidth(1.0f)

         );

         //Add GeoQuery here
         GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(fence.latitude,fence.longitude),1.0f);
         geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
             @Override
             public void onKeyEntered(String key, GeoLocation location) {
                 sendNotification("Chinmay",String.format("%s entered the fence! You may login now.",key));
             }

             @Override
             public void onKeyExited(String key) {
                 sendNotification("Chinmay",String.format("%s exited the fence! Thank you!",key));
             }

             @Override
             public void onKeyMoved(String key, GeoLocation location) {
                Log.d("MOVE",String.format("%s moved withim the fence[%f,%f]",key,location.latitude,location.longitude));
             }

             @Override
             public void onGeoQueryReady() {

             }

             @Override
             public void onGeoQueryError(DatabaseError error) {
                Log.e("ERROR",""+error);
             }
         });
    }

    private void sendNotification(String title, String content) {
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(content);
        NotificationManager manager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(this,MapsActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(contentIntent);
        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.defaults |= Notification.DEFAULT_SOUND;


        manager.notify(new Random().nextInt(),notification);

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest,this);
    }

    @Override
    public void onConnectionSuspended(int i)  {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        displayLocation();
    }





}