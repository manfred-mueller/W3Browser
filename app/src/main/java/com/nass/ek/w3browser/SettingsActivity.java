package com.nass.ek.w3browser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class SettingsActivity extends AppCompatActivity {

    Context context = this;

    @SuppressLint("ApplySharedPref")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageButton b = findViewById(R.id.settingsSaveButton);
        b.setOnClickListener(view -> {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            @SuppressLint("UseSwitchCompatOrMaterialCode") Switch s;

            s = findViewById(R.id.autoStart);
            editor.putBoolean("autoStart", s.isChecked());

            s = findViewById(R.id.desktopToggle);
            editor.putBoolean("desktopMode", s.isChecked());

            EditText e;

            e = findViewById(R.id.urlEditText);
            editor.putString("url", e.getText().toString());

            if (!Build.CPU_ABI.startsWith("arm")) {
                editor.commit();
            }
            else {
                editor.apply();
            }

            restartApp();
        });

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch s;

        s = findViewById(R.id.autoStart);
        s.setChecked(sharedPreferences.getBoolean("autoStart", false));

        s = findViewById(R.id.desktopToggle);
        s.setChecked(sharedPreferences.getBoolean("desktopMode", false));

        EditText e;
        e = findViewById(R.id.urlEditText);
        e.setText(sharedPreferences.getString("url", ""));
    }

    private void restartApp() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        this.startActivity(intent);
        Runtime.getRuntime().exit(0);
    }

    public void startSystemSettings(View v){
        startActivity(new Intent(Settings.ACTION_SETTINGS));
    }

    public void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);

        View view = this.getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}