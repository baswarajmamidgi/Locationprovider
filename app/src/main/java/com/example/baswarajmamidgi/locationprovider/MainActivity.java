package com.example.baswarajmamidgi.locationprovider;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements  GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    EditText user_name,pass_word;
    Spinner spinner;
    private FirebaseAuth mAuth;
    ProgressDialog dialog;
    ArrayAdapter<String> adapter;
    boolean isconnected;
    Button login;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    PendingResult<LocationSettingsResult> result;
    final static int REQUEST_LOCATION = 199;
    private DatabaseReference databaseReference;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseAuth.AuthStateListener mAuthListener;

    ArrayList<String> viapoints;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Login page");
        mAuth=FirebaseAuth.getInstance();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        mGoogleApiClient.connect();
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d("log", "onAuthStateChanged:signed_in:" + user.getUid());
                    Intent i=new Intent(MainActivity.this, MapsActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(i);

                } else {
                    // User is signed out
                    Log.d("log", "onAuthStateChanged:signed_out");
                }

            }
        };




        firebaseDatabase=FirebaseDatabase.getInstance();
        databaseReference=firebaseDatabase.getReference().child("routeno");
        user_name= (EditText) findViewById(R.id.editText);
        pass_word= (EditText) findViewById(R.id.editText2);
        spinner= (Spinner) findViewById(R.id.spinner);
        login= (Button) findViewById(R.id.button2);
        dialog=new ProgressDialog(this);
        dialog.setMessage("Logging....");
        ConnectivityManager manager= (ConnectivityManager) getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info=manager.getActiveNetworkInfo();
        isconnected=info!=null && info.isConnectedOrConnecting();
        final ArrayList<String> arraylist=new ArrayList<>();
        databaseReference=firebaseDatabase.getReference().child("routeno");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot postsnapshot : dataSnapshot.getChildren()) {
                    String message = postsnapshot.getValue(String.class);
                    arraylist.add(message);
                    Log.i("log", message);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, arraylist);
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        spinner.setAdapter(adapter);


        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isconnected)
                {
                    Toast.makeText(MainActivity.this, "No network", Toast.LENGTH_SHORT).show();
                    Log.i("log","no network called");
                }
                else
                {
                    login();
                }
            }
        });



    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    public void login()
    {

        final String username=user_name.getText().toString();
        String password=pass_word.getText().toString();
        final String routeno = spinner.getSelectedItem().toString();

        Log.i("log",routeno);
        if(TextUtils.isEmpty(username))
        {
            Toast.makeText(this, "enter Login id", Toast.LENGTH_SHORT).show();
            return;
        }
        if(TextUtils.isEmpty(password))
        {
            Toast.makeText(this, "enter password", Toast.LENGTH_SHORT).show();
            return;
        }
        if(password.length()<6)
        {
            Toast.makeText(this, "Password length should be atlest 6 chars", Toast.LENGTH_SHORT).show();
            return;
        }
        if(TextUtils.isEmpty(routeno))
        {
            Toast.makeText(this, "Select route", Toast.LENGTH_SHORT).show();
            return;
        }
        dialog.show();




        mAuth.signInWithEmailAndPassword(username,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                dialog.dismiss();
                if(task.isSuccessful())
                {
                    databaseReference=firebaseDatabase.getReference().child("drivers");
                    String data[]=username.split("@");
                    databaseReference.child(data[0]).setValue(spinner.getSelectedItem().toString());

                    Toast.makeText(MainActivity.this, "Login success", Toast.LENGTH_SHORT).show();
                    Intent i=new Intent(getApplicationContext(),MapsActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    i.putExtra("routeno",routeno);
                    startActivity(i);
                    finish();
                }
                else
                {

                    Toast.makeText(MainActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                }

            }
        });


    }
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(30 * 1000);
        mLocationRequest.setFastestInterval(5 * 1000);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);

        result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                //final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        //...
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    MainActivity.this,
                                    REQUEST_LOCATION);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        // break;
                }
            }
        });

    }
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.d("onActivityResult()", Integer.toString(resultCode));
        switch (requestCode)
        {
            case REQUEST_LOCATION:
                switch (resultCode)
                {
                    case Activity.RESULT_OK:
                    {

                        break;
                    }
                    case Activity.RESULT_CANCELED:
                    {
                        Toast.makeText(MainActivity.this, "Location not enabled.", Toast.LENGTH_LONG).show();
                        finish();
                        break;
                    }
                    default:
                    {
                        break;
                    }
                }
                break;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


}



