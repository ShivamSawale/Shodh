package com.example.shodh;


import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationService extends Service {

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }
            for (Location location : locationResult.getLocations()) {
                if (location != null) {
                    // Save the location to SharedPreferences
                    saveLocationToLocalStorage(location.getLatitude(), location.getLongitude());
                }
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            String action = intent.getAction();
            assert action != null;
            if (action.equals(Constants.ACTION_START_LOCATION_SERVICE)) {
                startLocationUpdates();
            } else if (action.equals(Constants.ACTION_STOP_LOCATION_SERVICE)) {
                stopLocationUpdates();
            }
        }
        return START_STICKY;
    }

    private void startLocationUpdates() {
        String CHANEL_ID = "location_notification_chanel";
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent resultIntent = new Intent();

        PendingIntent pendingIntent;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                    17, resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                    17, resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(AppBase.getInstance(), CHANEL_ID)
                .setSmallIcon(R.drawable.app_icon_white)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText("Location tracking is active")
                .setAutoCancel(false)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager != null && notificationManager.getNotificationChannel(CHANEL_ID) == null) {
                NotificationChannel notificationChannel = new NotificationChannel(CHANEL_ID, "Location Service", NotificationManager.IMPORTANCE_HIGH);
                notificationChannel.setDescription("This notification channel is used to notify that location tracking is active");
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30000).build();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return;
        }
        LocationServices.getFusedLocationProviderClient(this)
                .requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        startForeground(Constants.LOCATION_SERVICE_ID, builder.build());
    }

    private void stopLocationUpdates() {
        LocationServices.getFusedLocationProviderClient(this)
                .removeLocationUpdates(locationCallback);
        stopForeground(true);
        stopSelf();
    }

    private void saveLocationToLocalStorage(double latitude, double longitude) {
        SharedPreferences sharedPreferences = getSharedPreferences("LocationPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("last_latitude", String.valueOf(latitude));
        editor.putString("last_longitude", String.valueOf(longitude));
        editor.apply();
    }
}
