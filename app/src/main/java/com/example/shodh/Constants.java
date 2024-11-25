package com.example.shodh;


import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

public class Constants {

    public static final int STATUS_SUCCESS = 2;
    public static final int STATUS_FAILED = 4;

    public static final String ACTION_START_LOCATION_SERVICE = "startLocationService";
    public static final String ACTION_STOP_LOCATION_SERVICE = "stopLocationService";
    public static final int LOCATION_SERVICE_ID = 100;

    public static void showToast(String message) {
        Toast.makeText(AppBase.getInstance(), message, Toast.LENGTH_SHORT).show();
    }

    public static void showSnackbar(View view, String message) {

        Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
                .setTextColor(AppBase.getInstance().getColor(R.color.white))
                .setBackgroundTint(AppBase.getInstance().getColor(R.color.red))
                .show();

    }
}
