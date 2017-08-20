package com.example.baswarajmamidgi.locationprovider;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, DirectionFinderListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private GoogleMap mMap;
    private FirebaseAuth mAuth;
    private boolean isconnected;
    private GoogleApiClient mGoogleApiClient;
    private List<Marker> originMarkers = new ArrayList<>();
    private List<Marker> destinationMarkers = new ArrayList<>();
    private List<Polyline> polylinePaths = new ArrayList<>();
    private ProgressDialog progressDialog;
    private DatabaseReference databaseReference;
    private FirebaseDatabase firebaseDatabase;
    private ArrayList<String> viapoints;
    private LocationRequest locationRequest;
    private String routeno;
    private static Location currentlocation;
    private int a = 0;
    boolean doubleBackToExitPressedOnce = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mAuth = FirebaseAuth.getInstance();
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Location Provider");
        firebaseDatabase = FirebaseDatabase.getInstance();
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        ConnectivityManager connmanager = (ConnectivityManager) getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = connmanager.getActiveNetworkInfo();
        isconnected = info != null && info.isConnectedOrConnecting();
        if (!isconnected) {
            Toast.makeText(this, "No network available", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = getIntent();
        routeno = i.getStringExtra("routeno");
        if (routeno == null) {
            String data[] = mAuth.getCurrentUser().getEmail().split("@");
            databaseReference = firebaseDatabase.getReference().child("drivers").child(data[0]);
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    routeno = dataSnapshot.getValue(String.class);
                    Log.i("f log", routeno);

                    getViapoints(routeno);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } else {
            getViapoints(routeno);
        }
        locationRequest = new LocationRequest();
        locationRequest.setInterval(100);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        FloatingActionButton faab = (FloatingActionButton) findViewById(R.id.floatingbutton);
        progressDialog = ProgressDialog.show(this, "Please wait.",
                "Finding direction..!", true);
        progressDialog.setCancelable(true);
        progressDialog.show();

        faab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String vnr = "VNRVJIET+Students+Bus+Stop";
                Location college = new Location("");
                college.setLatitude(17.539980);
                college.setLongitude(78.386271);
                if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    // return TODO;
                }
                Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (location == null) {
                    Toast.makeText(MapsActivity.this, "Location not available", Toast.LENGTH_SHORT).show();
                }
                try {

                    if (location != null) {
                        if (location.distanceTo(college) > 2000) {
                            coustomdialog(viapoints.get(viapoints.size() - 1));
                        } else {
                            Collections.reverse(viapoints);
                            coustomdialog(viapoints.get(viapoints.size() - 1));
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(MapsActivity.this, "Route unavailable", Toast.LENGTH_SHORT).show();
                }
            }

        });

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (progressDialog.isShowing()) {
                    Intent i = new Intent(MapsActivity.this, MapsActivity.class);
                    i.putExtra("viapoints", viapoints);
                    i.putExtra("routeno", routeno);
                    startActivity(i);
                    finish();
                }
            }
        }, 3000);


    }

    protected void onStart() {
        super.onStart();
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.clear();
        UiSettings uiSettings = mMap.getUiSettings();
        uiSettings.setAllGesturesEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);
        uiSettings.setMapToolbarEnabled(false);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET}
                        , 10);
                return;
            }
        } else {
            uiSettings.setMyLocationButtonEnabled(true);
            mMap.setMyLocationEnabled(true);
        }
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location != null) {
            Log.i("log", "in passive provider");
            LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
            CameraPosition cameraPosition = new CameraPosition.Builder().target(position).bearing(5).tilt(50).build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            mMap.moveCamera(CameraUpdateFactory.zoomTo(16));
        } else {
            Log.i("log", "location is null");
            LatLng position = new LatLng(17.539165, 78.385787);
            CameraPosition cameraPosition = new CameraPosition.Builder().target(position).bearing(5).tilt(50).build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            mMap.moveCamera(CameraUpdateFactory.zoomTo(16));
        }
        mMap.addMarker(new MarkerOptions().position(new LatLng(17.539980, 78.386271)).title("VNR VJIET"));

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (routeno != null) {
                    updatelocation(routeno, viapoints);
                    Log.i("log", "update location called");
                } else {
                    handler.post(this);
                }

            }
        }, 1000);


    }


    public void updatelocation(String routeno, ArrayList<String> viapoints) {
        Log.i("log", "update location block called");
        if (!isconnected) {
            Toast.makeText(this, "No network available", Toast.LENGTH_SHORT).show();
            return;
        }
        Location college = new Location("");
        college.setLatitude(17.539980);
        college.setLongitude(78.386271);
        Log.i("log", "update location called");
        databaseReference = firebaseDatabase.getReference().child("busroutes").child(routeno);
        Date date = new Date();
        databaseReference.child("time").setValue(date.toString());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        while (currentlocation != null) {


            Log.i("log", "cal dist called");
            if (viapoints.isEmpty()) {
                Log.i("log", "viapoints is empty");
            } else {
                sendRequest(viapoints);
            }
            return;

        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mapmenu, menu);
        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.refresh) {
            Intent i = new Intent(this, MapsActivity.class);
            i.putExtra("viapoints", viapoints);
            i.putExtra("routeno", routeno);
            startActivity(i);
            finish();
        }
        if (id == R.id.logout) {
            mAuth.signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        return super.onOptionsItemSelected(item);

    }


    public void coustomdialog(final String destination) {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setTitle("Start Navigation ?");
        builder1.setMessage("Warning: Navigation may result in different path apart from specified bus path.");
        builder1.setCancelable(false);

        builder1.setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                                Uri.parse("google.navigation:q=" + destination));
                        startActivity(intent);
                        dialog.cancel();
                    }
                });

        builder1.setNegativeButton(
                "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert11 = builder1.create();
        alert11.show();


    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 10:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("log", "permissions granted");
                }

        }
    }


    private void sendRequest(ArrayList<String> viapoints) {
        Log.i("log", "send request called");

        try {
            new DirectionFinder(this, viapoints).execute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onDirectionFinderStart() {


        if (originMarkers != null) {
            for (Marker marker : originMarkers) {
                marker.remove();
            }
        }

        if (destinationMarkers != null) {
            for (Marker marker : destinationMarkers) {
                marker.remove();
            }
        }

        if (polylinePaths != null) {
            for (Polyline polyline : polylinePaths) {
                polyline.remove();
            }
        }

    }


    @Override
    public void onDirectionFinderSuccess(List<Route> routes) {
        progressDialog.dismiss();
        polylinePaths = new ArrayList<>();
        originMarkers = new ArrayList<>();
        destinationMarkers = new ArrayList<>();

        for (Route route : routes) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, 16));
            originMarkers.add(mMap.addMarker(new MarkerOptions()
                    .title(route.startAddress)
                    .position(route.startLocation)));
            destinationMarkers.add(mMap.addMarker(new MarkerOptions()
                    .title(route.endAddress)
                    .position(route.endLocation)));

            PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true).
                    color(Color.BLUE).
                    width(10);

            for (int i = 0; i < route.points.size(); i++)
                polylineOptions.add(route.points.get(i));

            polylinePaths.add(mMap.addPolyline(polylineOptions));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);

    }


    @Override
    public void onLocationChanged(Location location) {
        databaseReference = firebaseDatabase.getReference().child("busroutes").child(routeno);

        currentlocation = location;
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        Locationcoordinates locationcoordinates = new Locationcoordinates(Double.toString(latitude), Double.toString(longitude));
        databaseReference.child("latitude").setValue(locationcoordinates.getLatitude());
        databaseReference.child("longitude").setValue(locationcoordinates.getLongitude());
        Log.i("updated", "location updated");
        LatLng latLng = new LatLng(latitude, longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public void getViapoints(String routeno) {
        viapoints = new ArrayList<String>();
        Log.i("log", "getviapoints called");
        databaseReference = firebaseDatabase.getReference().child("viapoints").child(routeno);
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot postsnapshot : dataSnapshot.getChildren()) {
                    String message = postsnapshot.getValue(String.class);
                    String[] data = message.split(",");
                    viapoints.addAll(Arrays.asList(data));//convertin array to arraylist
                    Log.i("log", message);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;


        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finishAffinity();
                System.exit(0);
            }
        }, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        databaseReference = firebaseDatabase.getReference().child("busroutes").child(routeno);
        android.location.LocationListener listener = new android.location.LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                Locationcoordinates locationcoordinates = new Locationcoordinates(Double.toString(latitude), Double.toString(longitude));
                //databaseReference.setValue(locationcoordinates);
                databaseReference.child("latitude").setValue(locationcoordinates.getLatitude());
                databaseReference.child("longitude").setValue(locationcoordinates.getLongitude());
                Log.i("log", "location updated");

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        manager.requestLocationUpdates("gps", 1000, 0, listener);

    }
}