package com.nass.ek.w3browser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class StartActivityOnBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
       SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
       boolean autostartCheck = sharedPreferences.getBoolean("autoStart", false);
        if ((Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) && (autostartCheck)) {
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}