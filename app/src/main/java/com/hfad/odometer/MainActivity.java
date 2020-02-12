package com.hfad.odometer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private OdometerService odometer;
    private boolean bound = false;
    private final int PERMISSION_REQUEST_CODE = 698;
    private final int NOTIFICATION_ID = 423;
    public static final String CHANNEL_ID = "NOti Channel";

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            OdometerService.OdometerBinder odometerBinder =
                    (OdometerService.OdometerBinder) binder;
            odometer = odometerBinder.getOdometer();
            bound = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            //Code that runs when the service is disconnected
            bound = false;

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        displayDistance();
    }

    @Override
    protected void onStart(){
        super.onStart();
        if(ContextCompat.checkSelfPermission(this, OdometerService.PERMISSION_STRING)
        != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{OdometerService.PERMISSION_STRING},
                    PERMISSION_REQUEST_CODE);
        } else {
            Intent intent = new Intent(this, OdometerService.class);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onStop(){
        super.onStop();
        if (bound){
            unbindService(connection);
            bound = false;
        }
    }


    private void displayDistance(){
        final TextView distanceView = (TextView) findViewById(R.id.distance);
        final Handler handler = new Handler();
        handler.post(new Runnable(){
            @Override
            public void run(){
                double distance = 0.0;
                if (bound && odometer != null){
                    distance = odometer.getDistance();
                }
                String distanceStr = String.format(Locale.getDefault(),
                        "%1$,.2f miles", distance);
                distanceView.setText(distanceStr);
                handler.postDelayed(this, 1000);
            }
        });
    }

    public void onRequestPermissionResult(int requestCode,
                                          String permissions[], int[] grantResults){
        switch (requestCode){
            case PERMISSION_REQUEST_CODE:{
                if (grantResults.length >0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Intent intent = new Intent(this, OdometerService.class);
                    bindService(intent, connection, Context.BIND_AUTO_CREATE);

                } else {
                    //Code to run if permission was denied
                    //Create a notificaiton builder

                    createNotificationChannel();
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setSmallIcon(android.R.drawable.ic_menu_compass)
                            .setContentTitle(getResources().getString(R.string.app_name))
                            .setContentText(getResources().getString(R.string.permission_denied))
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setVibrate(new long[] {1000, 1000})
                            .setAutoCancel(true);
                    //Create an action
                    Intent actionIntent = new Intent(this, MainActivity.class);
                    PendingIntent actionPendingIntent = PendingIntent.getActivity(
                            this,
                            0,
                            actionIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                    builder.setContentIntent(actionPendingIntent);

                    //Issue the notification
                    NotificationManager notificationManager =
                            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.notify(NOTIFICATION_ID, builder.build());

                }
            }
        }
    }


    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
