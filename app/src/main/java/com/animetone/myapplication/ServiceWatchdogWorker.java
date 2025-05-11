package com.animetone.myapplication;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;


public class ServiceWatchdogWorker extends Worker {

    public ServiceWatchdogWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);

        boolean isMonitoringEnabled = prefs.getBoolean("monitoring_enabled", false);
        boolean isBotEnabled = prefs.getBoolean("bot_control_enabled", false);

        if (isMonitoringEnabled && !isServiceRunning(MonitoringService.class)) {
            Intent monitoringIntent = new Intent(getApplicationContext(), MonitoringService.class);
            ContextCompat.startForegroundService(getApplicationContext(), monitoringIntent);
        }

        if (isBotEnabled && !isServiceRunning(BotControlService.class)) {
            Intent botIntent = new Intent(getApplicationContext(), BotControlService.class);
            ContextCompat.startForegroundService(getApplicationContext(), botIntent);
        }

        return Result.success();
    }


    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) return true;
        }
        return false;
    }
}


