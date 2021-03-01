package com.nass.ek.w3browser;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import org.mozilla.geckoview.ContentBlocking;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;

import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public String PASSWORD = "0000";
    String InitialUrl;
    String Website;
    private GeckoView mGeckoView;
    private GeckoSession mGeckoSession;
    private final GeckoSessionSettings.Builder settingsBuilder = new GeckoSessionSettings.Builder();
    private ProgressBar mProgressView;
    Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mProgressView = findViewById(R.id.progressBar);

        setupSettings();
        setupGeckoView();
        commitURL(Website);

        //allow opening url from another app
        Intent mIntent = getIntent();
        String action = mIntent.getAction();
        if (action != null && action.equals(Intent.ACTION_VIEW)) {
            commitURL(mIntent.getData().toString());
        }
    }

    private void checkPassword(String title) {
        LayoutInflater li = LayoutInflater.from(this);
        View prompt = li.inflate(R.layout.check_password_dialog, null);
        AlertDialog.Builder checkPasswordDialog = new AlertDialog.Builder(this);
        checkPasswordDialog.setView(prompt);
        final EditText password = prompt.findViewById(R.id.check_password);

        checkPasswordDialog.setTitle(title);
        checkPasswordDialog.setCancelable(false)
                .setPositiveButton("OK", (dialog, id) -> {
                    String PwInput = password.getText().toString();
                    if (PwInput.equals("exit")) {
                        finish();
                    }
                    else if (PwInput.equals("h")) {
                        Intent startSupportActivityIntent = new Intent(getApplicationContext(), SupportActivity.class);
                        startActivity(startSupportActivityIntent);
                    }
                    else if (PwInput.equals(PASSWORD)) {
                        Intent startSettingsActivityIntent = new Intent(getApplicationContext(), SettingsActivity.class);
                        startActivity(startSettingsActivityIntent);
                    } else {
                        checkPassword(getString(R.string.code_or_help));
                    }
                });

        checkPasswordDialog.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.cancel());
        checkPasswordDialog.show();
    }

    private void setupSettings() {
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        ImageButton settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(view -> checkPassword(getString(R.string.code_or_help)));

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        settingsBuilder.allowJavascript(true);
        settingsBuilder.useTrackingProtection(false);
        settingsBuilder.usePrivateMode(false);

        if (sharedPreferences.getBoolean("desktopMode", false)) {
            settingsBuilder.userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP);
            settingsBuilder.viewportMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP);
        } else {
            settingsBuilder.userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE);
            settingsBuilder.viewportMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE);
        }

        InitialUrl = getString(R.string.location_hint);
        Website = sharedPreferences.getString("url", InitialUrl);
    }

    private void setupGeckoView() {
        mGeckoView = findViewById(R.id.geckoView);
        GeckoRuntime runtime = GeckoRuntime.create(this);
        mGeckoSession = new GeckoSession(settingsBuilder.build());

        mGeckoSession.setProgressDelegate(new createProgressDelegate());

        mGeckoSession.open(runtime);
        mGeckoView.setSession(mGeckoSession);
        mGeckoSession.setContentBlockingDelegate(new createBlockingDelegate());
    }

    private void commitURL(String url) {
        if ((url.contains(".") || url.contains(":")) && !url.contains(" ")) {
            url = url.toLowerCase();
            if (!url.contains("https://") && !url.contains("http://")) {
                url = "https://" + url;
            }
            mGeckoSession.loadUri(url);
        } else if (url.equals("") || url.equals(" ")) {
            mGeckoSession.loadUri(InitialUrl);
        } else {
            mGeckoSession.loadUri(Website);
        }

        mGeckoView.requestFocus();
        hideKeyboard(this);
    }

    private void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
         if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    protected void onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        Runtime.getRuntime().exit(0);
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
    }

    private class createProgressDelegate implements GeckoSession.ProgressDelegate {

        @Override
        public void onPageStart(@NonNull GeckoSession session, @NonNull String url) {
        }

        @Override
        public void onProgressChange(@NonNull GeckoSession session, int progress) {
            mProgressView.setProgress(progress);

            if (progress > 0 && progress < 100) {
                mProgressView.setVisibility(View.VISIBLE);
                mGeckoView.setVisibility(View.INVISIBLE);
            } else {
                mProgressView.setVisibility(View.GONE);
                mGeckoView.setVisibility(View.VISIBLE);
            }

        }
    }

    private class createBlockingDelegate implements ContentBlocking.Delegate {

        String ERROR = getString(R.string.an_error_occurred);
        @Override
        public void onContentBlocked(@NonNull GeckoSession session, ContentBlocking.BlockEvent event) {
            Log.e("DEBUG", ERROR + event);

            URL url = null;
            try {
                url = new URL(event.uri);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            assert url != null;
        }
    }

}



