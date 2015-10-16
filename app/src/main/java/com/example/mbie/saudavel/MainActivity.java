package com.example.mbie.saudavel;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends ActionBarActivity {
    private static final String TAG = "MainActivity";

    private static final int REQUEST_OAUTH = 1;

    /**
     *  Track whether an authorization activity is stacking over the current activity, i.e. when
     *  a known auth error is being resolved, such as showing the account chooser or presenting a
     *  consent dialog. This avoids common duplications as might happen on screen rotations, etc.
     */
    private static final String AUTH_PENDING = "auth_state_pending";
    private boolean authInProgress = false;

    private GoogleApiClient mClient = null;

    BodyData bodyData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton nextEvalFAB = (FloatingActionButton) findViewById(R.id.next_eval_FAB);

        final Context context = this;
        nextEvalFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Insert new calories today");

                // Set up the input
                final EditText input = new EditText(context);
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        float newCalorieIntake = Float.parseFloat(input.getText().toString());
                        BodyData.addFoodToday(context, newCalorieIntake);
                        setupView();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });

        bodyData = new BodyData();

        setupView();

        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }

        buildFitnessClient();

    }

    private void setupView() {
        int exerciseCalTarget = (int) bodyData.getExerciseCalTarget();
        int exerciseCalReached = (int) bodyData.getExerciseCalReached();
        int foodCalTarget = (int) bodyData.getFoodCalTarget(this);
        int foodCalReached = (int) BodyData.getFoodCalReached(this);
        int BMRTarget = (int) BodyData.getBMRTarget(this);
        int BMRReached = (int) bodyData.getBMRReached(this);
        int remainingDays = (int) BodyData.getRemainingDays(this);

        TextView food = (TextView) findViewById(R.id.food_text);
        TextView exercise = (TextView) findViewById(R.id.exercise_text);
        TextView bmr = (TextView) findViewById(R.id.bmr_text);
        TextView remainingDaysText = (TextView) findViewById(R.id.remaining_days_text);


        food.setText("food: " + foodCalReached + '/' + foodCalTarget + " cal");
        exercise.setText ("exercise: " + exerciseCalReached + '/' + exerciseCalTarget + " cal");
        bmr.setText("bmr: " + BMRReached + '/' + BMRTarget + " cal");

        remainingDaysText.setText(remainingDays + " days");

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
        switch (id) {
        case R.id.action_settings:
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        case R.id.action_reset_food:
            BodyData.resetFood(this);
            setupView();
            return true;
        case R.id.action_add_new_weight:
            setNewWeightDialog();
            return true;
        case R.id.action_reset_weight_to_cal:
            BodyData.setCalToKgFactor(this, 9300);
            setupView();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setNewWeightDialog() {
        final Context context = this;

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Insert new calories today");

        // Set up the input
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16,16,16,16);

        TextView inputWeightLabel = new TextView(this);
        inputWeightLabel.setText("New weight");
        layout.addView(inputWeightLabel);
        final EditText inputWeight = new EditText(this);
        inputWeight.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(inputWeight);

        TextView inputDaysLabel = new TextView(this);
        inputDaysLabel.setText("Days since your last measurement");
        layout.addView(inputDaysLabel);
        final EditText inputDaysPassed = new EditText(this);
        inputDaysPassed.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(inputDaysPassed);

        builder.setView(layout);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    double newWeight = Double.parseDouble(inputWeight.getText().toString());
                    int daysPassed = Integer.parseInt(inputDaysPassed.getText().toString());
                    BodyData.setWeight(context, newWeight, daysPassed);
                    setupView();
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error reading input values");
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    /**
     *  Build a {@link GoogleApiClient} that will authenticate the user and allow the application
     *  to connect to Fitness APIs. The scopes included should match the scopes your app needs
     *  (see documentation for details). Authentication will occasionally fail intentionally,
     *  and in those cases, there will be a known resolution, which the OnConnectionFailedListener()
     *  can address. Examples of this include the user never having signed in before, or having
     *  multiple accounts on the device and needing to specify which account to use, etc.
     */
    private void buildFitnessClient() {
        final MainActivity that = this;
        // Create the Google API Client
        mClient = new GoogleApiClient.Builder(this)
            .addApi(Fitness.SENSORS_API)
            .addScope(new Scope(Scopes.FITNESS_LOCATION_READ))
            .addApi(Fitness.HISTORY_API)
            .addScope(new Scope(Scopes.FITNESS_BODY_READ))
            .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
            .addScope(new Scope(Scopes.FITNESS_NUTRITION_READ))
            .addConnectionCallbacks(
                    new GoogleApiClient.ConnectionCallbacks() {

                        @Override
                        public void onConnected(Bundle bundle) {
                            Log.i(TAG, "Connected!!!");
                            // Now you can make calls to the Fitness APIs.
                            // Put application specific code here.

                            that.downloadLastWeekCalories();
                            that.downloadTodayCalories();
                        }

                        @Override
                        public void onConnectionSuspended(int i) {
                            // If your connection to the sensor gets lost at some point,
                            // you'll be able to determine the reason and react to it here.
                            if (i == ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                Log.i(TAG, "Connection lost.  Cause: Network Lost.");
                            } else if (i == ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                Log.i(TAG, "Connection lost.  Reason: Service Disconnected");
                            }
                        }
                    }
            )
            .addOnConnectionFailedListener(
                new GoogleApiClient.OnConnectionFailedListener() {
                    // Called whenever the API client fails to connect.
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.i(TAG, "Connection failed. Cause: " + result.toString());
                        if (!result.hasResolution()) {
                            // Show the localized error dialog
                            Log.e(TAG, "Connection error: " + result.getErrorMessage());
                            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(),
                                    MainActivity.this, 0).show();
                            return;
                        }
                        // The failure has a resolution. Resolve it.
                        // Called typically when the app is not yet authorized, and an
                        // authorization dialog is displayed to the user.
                        if (!authInProgress) {
                            try {
                                Log.i(TAG, "Attempting to resolve failed connection");
                                authInProgress = true;
                                result.startResolutionForResult(MainActivity.this,
                                        REQUEST_OAUTH);
                            } catch (IntentSender.SendIntentException e) {
                                Log.e(TAG,
                                        "Exception while starting resolution activity", e);
                            }
                        }
                    }
                }
            )
            .build();
    }

    static final long WEEK_IN_MS = 1000 * 60 * 60 * 24 * 7;
    private void downloadLastWeekCalories() {
        Log.i(TAG, "Downloading last week calories");
        Date now = new Date();
        long endTime = now.getTime();
        long startTime = endTime - (WEEK_IN_MS);

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_CALORIES_EXPENDED,
                        DataType.AGGREGATE_CALORIES_EXPENDED)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime,TimeUnit.MILLISECONDS)
                .build();

        PendingResult<DataReadResult> pendingResult =
                Fitness.HistoryApi.readData(mClient, readRequest);

        pendingResult.setResultCallback(
                new ResultCallback<DataReadResult>() {
                    @Override
                    public void onResult(DataReadResult dataReadResult) {
                        Log.i(TAG, "Result for last week calories");
                        if (dataReadResult.getBuckets().size() > 0) {
                            float exerciseCalTarget = 0;
                            for (Bucket bucket : dataReadResult.getBuckets()) {
                                List<DataSet> dataSets = bucket.getDataSets();
                                for (DataSet dataSet : dataSets) {
                                    // show data points
                                    exerciseCalTarget += processDataSetCalories(dataSet);
                                }
                            }
                            exerciseCalTarget /= dataReadResult.getBuckets().size();
                            bodyData.setExerciseCalTarget(exerciseCalTarget);

                            setupView();
                        }
                    }
                }
        );
    }

    private float processDataSetCalories(DataSet dataSet) {
        float exerciseCalTarget = 0;
        for (DataPoint dp : dataSet.getDataPoints()) {

            // obtain human-readable start and end times
            long dpStart = dp.getStartTime(TimeUnit.MILLISECONDS);
            long dpEnd   = dp.getEndTime(TimeUnit.MILLISECONDS);
            Log.i(TAG, "Data point:");
            Log.i(TAG, "\tType:  " + dp.getDataType().getName());
            Log.i(TAG, "\tStart: " + new Date(dpStart));
            Log.i(TAG, "\tEnd:   " + new Date(dpEnd));
            for (Field field : dp.getDataType().getFields()) {
                String fieldName = field.getName();
                Log.i(TAG, "\tField: " + fieldName + " Value: " + dp.getValue(field));
                if (fieldName.equals("calories")) {
                    exerciseCalTarget += dp.getValue(field).asFloat();
                }
            }
        }
        return exerciseCalTarget;
    }

    private void downloadTodayCalories() {
        Log.i(TAG, "Downloading today calories");

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        long now = cal.getTimeInMillis();

        Calendar dayStartCal = Calendar.getInstance();
        dayStartCal.setTime(new Date());
        dayStartCal.set(Calendar.HOUR_OF_DAY, 0);
        dayStartCal.set(Calendar.MINUTE, 0);
        dayStartCal.set(Calendar.SECOND, 0);
        dayStartCal.set(Calendar.MILLISECOND, 0);
        long dayStart = dayStartCal.getTimeInMillis();


        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_CALORIES_EXPENDED,
                        DataType.AGGREGATE_CALORIES_EXPENDED)
                .bucketByTime(42, TimeUnit.DAYS)
                .setTimeRange(dayStart, now, TimeUnit.MILLISECONDS)
                .build();

        PendingResult<DataReadResult> pendingResult =
                Fitness.HistoryApi.readData(mClient, readRequest);

        pendingResult.setResultCallback(
                new ResultCallback<DataReadResult>() {
                    @Override
                    public void onResult(DataReadResult dataReadResult) {
                        Log.i(TAG, "Result for last week calories");
                        if (dataReadResult.getBuckets().size() > 0) {
                            float exerciseCalReached = 0;
                            int counter = 0;
                            for (Bucket bucket : dataReadResult.getBuckets()) {
                                List<DataSet> dataSets = bucket.getDataSets();
                                for (DataSet dataSet : dataSets) {
                                    // show data points
                                    dataSet.getDataType();
                                    for (DataPoint dp : dataSet.getDataPoints()) {
                                        for (Field field : dp.getDataType().getFields()) {
                                            String fieldName = field.getName();
                                            Log.i(TAG, "\tField: " + fieldName + " Value: " + dp.getValue(field));
                                            if (fieldName.equals("calories")) {
                                                counter++;
                                                exerciseCalReached += dp.getValue(field).asFloat();
                                            }
                                        }
                                    }
                                }
                            }
                            exerciseCalReached /= counter;
                            bodyData.setExerciseCalReached(exerciseCalReached);


                            setupView();
                        }
                    }
                }
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Connect to the Fitness API
        Log.i(TAG, "Connecting...");
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.INTERNET);
        Log.e(TAG, "permission to internet: " + permissionCheck);
        mClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mClient.isConnected()) {
            mClient.disconnect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OAUTH) {
            authInProgress = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mClient.isConnecting() && !mClient.isConnected()) {
                    mClient.connect();
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(AUTH_PENDING, authInProgress);
    }
}
