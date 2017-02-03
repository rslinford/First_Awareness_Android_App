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
import android.widget.TextView;
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

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE_ACCESS_FINE_LOCATION = 1;
    private static final String TAG = "Awareness";
    private static final String RESULT_FAILURE = "result failure";
    private static final String LOCAL_ACTIVITY_LOG_FILENAME = "local_activity_log.txt";
    private static final DateTimeFormatter ISO_DATE_TIME_FORMAT = ISODateTimeFormat.dateTime();

    private GoogleApiClient mGoogleApiClient;
    private final Handler pollAwarenessHandler = new Handler();
    private final Runnable pollAwarenessRunnable = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "Begin handler");
            takeSnapshot();
            Log.i(TAG, "End handler");
            pollAwarenessHandler.postDelayed(pollAwarenessRunnable, 60000);
        }
    };
    private FileOutputStream localActivityLogOutputStream;


//    static class SnapShot {
//        DetectedActivityResult detectedActivityResult;
//        HeadphoneStateResult headphoneStateResult;
//        LocationResult locationResult;
//        PlacesResult placesResult;
//        WeatherResult weatherResult;
//
//        @Override
//        public String toString() {
//            return "SnapShot{" +
//                    "detectedActivityResult=" + detectedActivityResult +
//                    ", headphoneStateResult=" + headphoneStateResult +
//                    ", locationResult=" + locationResult +
//                    ", placesResult=" + placesResult +
//                    ", weatherResult=" + weatherResult +
//                    '}';
//        }
//    }

    static class TextSnapShot {
        String detectedActivityResult = "";
        String headphoneStateResult = "";
        String locationResult = "";
        String placesResult = "";
        String weatherResult = "";
        DateTime timeStamp = new DateTime();

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("TextSnapShot{");
            sb.append("detectedActivityResult='").append(detectedActivityResult).append('\'');
            sb.append(", headphoneStateResult='").append(headphoneStateResult).append('\'');
            sb.append(", locationResult='").append(locationResult).append('\'');
            sb.append(", placesResult='").append(placesResult).append('\'');
            sb.append(", weatherResult='").append(weatherResult).append('\'');
            sb.append(", timeStamp=").append(ISO_DATE_TIME_FORMAT.print(timeStamp));
            sb.append('}');
            return sb.toString();
        }
    }

    final private TextSnapShot textSnapShot = new TextSnapShot();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(MainActivity.this).addApi(Awareness.API).build();
        mGoogleApiClient.connect();

        final TextView textView = (TextView) findViewById (R.id.LOCAL_LOG_TEXT_VIEW);

        boolean existingLog = false;
        try {
            final FileInputStream fis = openFileInput(LOCAL_ACTIVITY_LOG_FILENAME);
            textView.append("\n** Reading existing local activity log");
            int c;
            while ((c = fis.read()) != -1) {
                textView.append(String.valueOf((char)c));
            }
            existingLog = true;
        } catch (FileNotFoundException e) {
            textView.append("\n** Starting new local activity log");
            Log.i(TAG, LOCAL_ACTIVITY_LOG_FILENAME + " does not exist yet");
        } catch (IOException e) {
            Log.e(TAG, "Failure reading " + LOCAL_ACTIVITY_LOG_FILENAME + ": " + e);
        }

        try {
            final int mode = existingLog ? MODE_APPEND : MODE_PRIVATE;
            localActivityLogOutputStream = openFileOutput(LOCAL_ACTIVITY_LOG_FILENAME, mode);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Failure opening " + LOCAL_ACTIVITY_LOG_FILENAME + ": " + e);
        }

        pollAwarenessHandler.post(pollAwarenessRunnable);
    }

    private void textViewAppend(final String s) {
        final TextView textView = (TextView) findViewById (R.id.LOCAL_LOG_TEXT_VIEW);
        textView.append(s);
        try {
            localActivityLogOutputStream.write(s.getBytes());
        } catch (IOException e) {
            Log.e(TAG, "Activity log write failure: " + e);
        }
    }

    private void takeSnapshot() {
        textSnapShot.timeStamp = new DateTime();
        textViewAppend("\n[TS] " + ISO_DATE_TIME_FORMAT.print(textSnapShot.timeStamp));

        Awareness.SnapshotApi.getDetectedActivity(mGoogleApiClient).setResultCallback(
                new ResultCallback<DetectedActivityResult>() {
                    @Override
                    public void onResult(@NonNull DetectedActivityResult detectedActivityResult) {
                        final String rtag = "\n[Activity] ";
                        if (!detectedActivityResult.getStatus().isSuccess()) {
                            Log.e(TAG, rtag + RESULT_FAILURE);
                            return;
                        }
                        ActivityRecognitionResult ar = detectedActivityResult.getActivityRecognitionResult();
                        DetectedActivity probableActivity = ar.getMostProbableActivity();
                        final String s = probableActivity.toString();
                        if (!textSnapShot.detectedActivityResult.equals(s)) {
                            textSnapShot.detectedActivityResult = s;
                            textViewAppend(rtag + s);
                        }
                        Log.i(TAG, rtag + s);
                    }
                });

        Awareness.SnapshotApi.getHeadphoneState(mGoogleApiClient).setResultCallback(
                new ResultCallback<HeadphoneStateResult>() {
                    @Override
                    public void onResult(@NonNull HeadphoneStateResult headphoneStateResult) {
                        final String rtag = "\n[Headphones] ";
                        if (!headphoneStateResult.getStatus().isSuccess()) {
                            Log.e(TAG, rtag + RESULT_FAILURE);
                            return;
                        }
                        HeadphoneState headphoneState = headphoneStateResult.getHeadphoneState();
                        final String s;
                        if (headphoneState.getState() == HeadphoneState.PLUGGED_IN) {
                            s = "plugged in";
                        } else {
                            s = "NOT plugged in";
                        }
                        if (!textSnapShot.headphoneStateResult.equals(s)) {
                            textSnapShot.headphoneStateResult = s;
                            textViewAppend(rtag + s);
                        }
                        Log.i(TAG, rtag + s);
                    }
                });

        checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, PERMISSION_REQUEST_CODE_ACCESS_FINE_LOCATION);
        Awareness.SnapshotApi.getLocation(mGoogleApiClient).setResultCallback(
                new ResultCallback<LocationResult>() {
                    @Override
                    public void onResult(@NonNull LocationResult locationResult) {
                        final String rtag = "\n[Location] ";
                        if (!locationResult.getStatus().isSuccess()) {
                            Log.e(TAG, rtag + RESULT_FAILURE);
                            return;
                        }
                        Location location = locationResult.getLocation();
                        final String s = "Lat(" + location.getLatitude() + ") Lon(" + location.getLongitude() + ")";
                        if (!textSnapShot.locationResult.equals(s)) {
                            textSnapShot.locationResult = s;
                            textViewAppend(rtag + s);
                        }
                        Log.i(TAG, rtag + s);
                    }
                });

        Awareness.SnapshotApi.getPlaces(mGoogleApiClient).setResultCallback(
                new ResultCallback<PlacesResult>() {
                    @Override
                    public void onResult(@NonNull PlacesResult placesResult) {
                        final String rtag = "\n[Places] ";
                        if (!placesResult.getStatus().isSuccess()) {
                            Log.e(TAG, rtag + RESULT_FAILURE);
                            return;
                        }
                        List<PlaceLikelihood> placeLikelihoodList = placesResult.getPlaceLikelihoods();
                        // Show the top 5 possible location results.
                        if (placeLikelihoodList != null) {
                            for (int i = 0; i < 5 && i < placeLikelihoodList.size(); i++) {
                                PlaceLikelihood p = placeLikelihoodList.get(i);
                                final String s = p.getPlace().getName().toString() + " odds(" + p.getLikelihood() + ")";
                                if (i == 0) {
                                    if (textSnapShot.placesResult.equals(s)) {
                                        break;
                                    }
                                    else {
                                       textSnapShot.placesResult = s;
                                   }
                                }

                                textViewAppend(rtag + s);
                                Log.i(TAG, rtag + s);
                            }
                        } else {
                            final String s = "null result";
                            if (!textSnapShot.placesResult.equals(s)) {
                                textSnapShot.placesResult = s;
                                textViewAppend(rtag + s);
                            }
                            Log.i(TAG, rtag + s);
                        }
                    }
                });

        Awareness.SnapshotApi.getWeather(mGoogleApiClient).setResultCallback(
                new ResultCallback<WeatherResult>() {
                    @Override
                    public void onResult(@NonNull WeatherResult weatherResult) {
                        final String rtag = "\n[Weather] ";
                        if (!weatherResult.getStatus().isSuccess()) {
                            Log.e(TAG, rtag + RESULT_FAILURE);
                            return;
                        }
                        Weather weather = weatherResult.getWeather();
                        final String s = weather.toString();
                        if (!textSnapShot.weatherResult.equals(s)) {
                            textSnapShot.weatherResult = s;
                            textViewAppend(rtag + s);
                        }
                        Log.i(TAG, rtag + s);
                    }
                });
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
            Log.i(TAG, "Permission (already) Granted!");
        }
    }

    private void blerbGrantResult(final String permission, final int grantResult) {
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Granted: " + permission);
        }
        else {
            final String s = "Denied: " + permission;
            blerb(s);
            Log.e(TAG, s);
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
