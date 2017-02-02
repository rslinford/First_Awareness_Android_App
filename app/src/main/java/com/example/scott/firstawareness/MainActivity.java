package com.example.scott.firstawareness;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.snapshot.DetectedActivityResult;
import com.google.android.gms.awareness.snapshot.HeadphoneStateResult;
import com.google.android.gms.awareness.snapshot.LocationResult;
import com.google.android.gms.awareness.snapshot.PlacesResult;
import com.google.android.gms.awareness.snapshot.WeatherResult;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.places.PlaceLikelihood;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final static int PERMISSION_REQUEST_CODE_ACCESS_FINE_LOCATION = 1;
    private static final String TAG = "Awareness";

    private GoogleApiClient mGoogleApiClient;
    private final Handler pollAwarenessHandler = new Handler();
    private final Runnable pollAwarenessRunnable = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "\n\nBegin handler");
            takeSnapshot();
            Log.i(TAG, "End handler \n");
            pollAwarenessHandler.postDelayed(pollAwarenessRunnable, 60000);
        }
    };

    static class SnapShot {
        DetectedActivityResult detectedActivityResult;
        HeadphoneStateResult headphoneStateResult;
        LocationResult locationResult;
        PlacesResult placesResult;
        WeatherResult weatherResult;

        @Override
        public String toString() {
            return "SnapShot{" +
                    "detectedActivityResult=" + detectedActivityResult +
                    ", headphoneStateResult=" + headphoneStateResult +
                    ", locationResult=" + locationResult +
                    ", placesResult=" + placesResult +
                    ", weatherResult=" + weatherResult +
                    '}';
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(MainActivity.this).addApi(Awareness.API).build();
        mGoogleApiClient.connect();
        pollAwarenessHandler.post(pollAwarenessRunnable);
    }

    private void takeSnapshot() {
//        final SnapShot snapShot = new SnapShot();

        Awareness.SnapshotApi.getDetectedActivity(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DetectedActivityResult>() {
                    @Override
                    public void onResult(@NonNull DetectedActivityResult detectedActivityResult) {
//                        snapShot.detectedActivityResult = detectedActivityResult;
                        if (!detectedActivityResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Could not get the current activity.");
                            return;
                        }
                        ActivityRecognitionResult ar = detectedActivityResult.getActivityRecognitionResult();
                        DetectedActivity probableActivity = ar.getMostProbableActivity();
                        Log.i(TAG, probableActivity.toString());
                        blerb(probableActivity.toString());
                    }
                });

        Awareness.SnapshotApi.getHeadphoneState(mGoogleApiClient)
                .setResultCallback(new ResultCallback<HeadphoneStateResult>() {
                    @Override
                    public void onResult(@NonNull HeadphoneStateResult headphoneStateResult) {
//                        snapShot.headphoneStateResult = headphoneStateResult;
                        if (!headphoneStateResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Could not get headphone state.");
                            return;
                        }
                        HeadphoneState headphoneState = headphoneStateResult.getHeadphoneState();
                        final String s;
                        if (headphoneState.getState() == HeadphoneState.PLUGGED_IN) {
                            s = "Headphones are plugged in.\n";
                        } else {
                            s = "Headphones are NOT plugged in.\n";
                        }
                        Log.i(TAG, s);
                        blerb(s);
                    }
                });

        checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, PERMISSION_REQUEST_CODE_ACCESS_FINE_LOCATION);
        Awareness.SnapshotApi.getLocation(mGoogleApiClient).setResultCallback(
                new ResultCallback<LocationResult>() {
                    @Override
                    public void onResult(@NonNull LocationResult locationResult) {
//                        snapShot.locationResult = locationResult;
                        if (!locationResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Could not get location.");
                            return;
                        }
                        Location location = locationResult.getLocation();
                        final String s = "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude();
                        Log.i(TAG, s);
                        blerb(s);
                    }
                });

        Awareness.SnapshotApi.getPlaces(mGoogleApiClient).setResultCallback(
                new ResultCallback<PlacesResult>() {
                    @Override
                    public void onResult(@NonNull PlacesResult placesResult) {
//                        snapShot.placesResult = placesResult;
                        if (!placesResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Could not get places.");
                            return;
                        }
                        List<PlaceLikelihood> placeLikelihoodList = placesResult.getPlaceLikelihoods();
                        // Show the top 5 possible location results.
                        if (placeLikelihoodList != null) {
                            for (int i = 0; i < 5 && i < placeLikelihoodList.size(); i++) {
                                PlaceLikelihood p = placeLikelihoodList.get(i);
                                final String s = p.getPlace().getName().toString() + ", likelihood: " + p.getLikelihood();
                                Log.i(TAG, s);
                                blerb(s);
                            }
                        } else {
                            final String s = "Place is null.";
                            Log.e(TAG, s);
                            blerb(s);
                        }
                    }
                });

        Awareness.SnapshotApi.getWeather(mGoogleApiClient)
                .setResultCallback(new ResultCallback<WeatherResult>() {
                    @Override
                    public void onResult(@NonNull WeatherResult weatherResult) {
//                        snapShot.weatherResult = weatherResult;
                        if (!weatherResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Could not get weather.");
                            return;
                        }
                        Weather weather = weatherResult.getWeather();
                        final String s = weather.toString();
                        Log.i(TAG, s);
                        blerb(s);
                    }
                });

//        Log.w(TAG, "\n\n" + snapShot.toString() + "\n\n");
    }

    private void blerb(final String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    private void checkPermission(@NonNull final String permission, final int permissionRequestCode) {
        int permissionCheck = ContextCompat.checkSelfPermission(this, permission);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                showExplanation("Permission Needed", "Rationale", permission, permissionRequestCode);
            } else {
                requestPermission(permission, permissionRequestCode);
            }
        } else {
            Toast.makeText(MainActivity.this, "Permission (already) Granted!", Toast.LENGTH_SHORT).show();
        }
    }

    private void blerbGrantResult(final String permission, final int grantResult) {
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            blerb("Granted: " + permission);
        }
        else {
            blerb("Denied: " + permission);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE_ACCESS_FINE_LOCATION:
                blerbGrantResult(permissions[0], grantResults[0]);
        }
    }

    private void showExplanation(String title, String message, final String permission, final int permissionRequestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermission(permission, permissionRequestCode);
                    }
                });
        builder.create().show();
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(this, new String[]{permissionName}, permissionRequestCode);
    }

}