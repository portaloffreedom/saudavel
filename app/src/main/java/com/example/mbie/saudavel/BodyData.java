package com.example.mbie.saudavel;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import algorithms.Coach;

/**
 * Created by matteo on 15/10/15.
 */
public class BodyData {
    private static final String TAG = "BodyData";
    static final String SETTING_WEIGHT = "weight";
    static final String SETTING_HEIGHT = "height";
    static final String SETTING_AGE = "age";
    static final String SETTING_BFP = "body_fat_percentage";
    static final String SETTING_SEX = "sex";
    static final String SETTING_TARGET_WEIGHT = "target_weight";
    static final String SETTING_TARGET_DAY = "target_day";
    static final String SETTING_FOOD_TODAY = "food_today";
    static final String SETTING_FOOD_LAST_MEASURED = "food_last_measured";
    static final String SETTING_CAL_TO_KG_FACTOR = "calToKgFactor";
    private static final long DAY_IN_MS = 1000 * 60 * 60 * 24;
    private static final double WARNING_ES_TARGET = -1000;
    private static final double WARNING_ES_EXERCISE_TARGET = 1000;

    static private boolean warningShown = false;

    private float exerciseCalTarget;
    private float exerciseCalReached;

    public BodyData() {
        exerciseCalTarget = 0;
        exerciseCalReached = 0;
    }

    static private String getSetting(Context context, String key) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getString(key, "0");
    }

    static public float getWeight(Context context) {
        return Float.parseFloat(getSetting(context, SETTING_WEIGHT));
    }

    static public void setWeight(Context context, double weight, int daysPassed) {
        double expectedNewWeight = getWeight(context);
        double targetWeight = getTargetWeight(context);
        double daysToGoal = getRemainingDays(context) + daysPassed;
        for (int i=0; i<daysPassed; i++) {
            expectedNewWeight = Coach.calculateWeighChangeTarget(expectedNewWeight, targetWeight, (int) daysToGoal);
            daysToGoal -= 1;
        }

        double expectedWeightChange = expectedNewWeight - getWeight(context);
        setWeight(context, weight, expectedWeightChange);
    }

    static public void setWeight(Context context, double weight, double expectedWeightChange) {
        double oldWeight = getWeight(context);
        double calToKgFactor = getCalToKgFactor(context);
        calToKgFactor = Coach.recalculateCalToKgFactor(calToKgFactor, oldWeight, weight, expectedWeightChange);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(SETTING_WEIGHT, Double.toString(weight));
        editor.commit();

        setCalToKgFactor(context, (float) calToKgFactor);
    }

    static public float getHeight(Context context) {
        return Float.parseFloat(getSetting(context, SETTING_HEIGHT));
    }

    static public float getAge(Context context) {
        return Float.parseFloat(getSetting(context, SETTING_AGE));
    }

    static public float getBodyFatPercentage(Context context) {
        return Float.parseFloat(getSetting(context, SETTING_BFP));
    }

    static public String getSex(Context context) {
        return getSetting(context, SETTING_SEX);
    }

    static public float getTargetWeight(Context context) {
        return Float.parseFloat(getSetting(context, SETTING_TARGET_WEIGHT));
    }

    static public void resetFood(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        setFood(sharedPref, 0);
    }

    static private void setFood(SharedPreferences sharedPref, float foodToday) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(SETTING_FOOD_TODAY, foodToday);
        editor.putLong(SETTING_FOOD_LAST_MEASURED, new Date().getTime());
        editor.commit();
    }

    static public void addFoodToday(Context context, float addFood) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        float food = getFoodCalReached(sharedPref);
        setFood(sharedPref, food + addFood);
    }

    static public float getFoodCalReached(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return getFoodCalReached(sharedPref);
    }

    static private float getFoodCalReached(SharedPreferences sharedPref) {
        Calendar lastMeasured = Calendar.getInstance();
        lastMeasured.setTime(new Date(sharedPref.getLong(SETTING_FOOD_LAST_MEASURED, 0)));

        Calendar now = Calendar.getInstance();
        now.setTime(new Date());

        if (lastMeasured.get(Calendar.DAY_OF_YEAR) != now.get(Calendar.DAY_OF_YEAR)) {
            setFood(sharedPref, 0);
            return 0;
        }

        return sharedPref.getFloat(SETTING_FOOD_TODAY, 0);
    }

    static public float getRemainingDays(Context context) {
        String targetDayString = getSetting(context, SETTING_TARGET_DAY);
        Date now = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date target;
        try {
            target = format.parse(targetDayString);
        } catch (ParseException e) {
            e.printStackTrace();
            target = new Date();
        }
        long distance = target.getTime() - now.getTime();
        return distance / DAY_IN_MS;
    }

    public float getExerciseCalTarget() {
        return exerciseCalTarget;
    }

    public void setExerciseCalTarget(float exerciseCalTarget) {
        this.exerciseCalTarget = exerciseCalTarget;
    }

    public float getExerciseCalReached() {
        return exerciseCalReached;
    }

    public void setExerciseCalReached(float exerciseCalReached) {
        this.exerciseCalReached = exerciseCalReached;
    }

    public double getFoodCalTarget(Context context) {
        double weightChange = Coach.calculateWeighChangeTarget(
                getWeight(context),
                getTargetWeight(context),
                (int) getRemainingDays(context));

        double ESTarget = Coach.calculateESTarget(weightChange, (int) getRemainingDays(context), getCalToKgFactor(context));
        double total = exerciseCalTarget + ESTarget + getBMRTarget(context);

        if (ESTarget < WARNING_ES_TARGET && !warningShown) {
            warningShown = true;
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Your calories target is too low!");
            builder.setMessage("You should set a more feasible target!\n" +
                    "Negative " + ESTarget + " calories per day is not good for your health.\n" +
                    "\n" +
                    "Please change your weight target or your day target to something more reasonable");

            // Set up the buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    warningShown = false;
                    Log.i(TAG, "dialog dismissed");
                }
            });

            builder.show();
        } else if ( total < WARNING_ES_EXERCISE_TARGET && !warningShown) {
            warningShown = true;
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Your calories target is too low!");
            builder.setMessage("You should make more physical exercise!\n" +
                    "A total of " + total + " calories per day is not good for your health.\n" +
                    "\n" +
                    "Please do more physical exercise in order to have more calories input");

            // Set up the buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    warningShown = false;
                    Log.i(TAG, "dialog dismissed");
                }
            });

            builder.show();
        }

        return total;
    }

    static public double getBMRTarget(Context context) {
        return Coach.calculateBMR(getSex(context), getWeight(context), getHeight(context), (int) getAge(context));
    }

    static public double getBMRReached(Context context) {
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

        return (getBMRTarget(context) * ((now - dayStart)/ DAY_IN_MS ));
    }

    static public float getCalToKgFactor(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getFloat(SETTING_CAL_TO_KG_FACTOR, 9300);
    }

    static public void setCalToKgFactor(Context context, float value) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(SETTING_CAL_TO_KG_FACTOR, value);
        editor.commit();
    }
}
