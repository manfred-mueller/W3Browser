package com.nass.ek.w3browser;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import org.mozilla.geckoview.ContentBlocking;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public String PASSWORD = "0000";
    String InitialUrl;
    String Website;
    private GeckoView mGeckoView;
    private GeckoSession mGeckoSession;
    private final GeckoSessionSettings.Builder settingsBuilder = new GeckoSessionSettings.Builder();
    private ProgressBar mProgressView;
    Context context = this;
    private int currentApiVersion;
    private static final String TV_URI = "com.teamviewer.quicksupport.market";
    private static final String AD_URI = "com.anydesk.anydeskandroid";
    private static final int CAMERA_REQUEST = 1888;
    private static final int REQUEST_OVERLAY_PERMISSION = 1;
    private static final int REQUEST_PERMISSIONS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements and settings...

        checkForAndAskForPermissions();
        checkOverlayPermission();

        if (android.os.Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, getString(R.string.additional_perms), Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 4712);
        }

        currentApiVersion = Build.VERSION.SDK_INT;
        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        if (currentApiVersion >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(flags);
            final View decorView = getWindow().getDecorView();
            decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.setSystemUiVisibility(flags);
                }
            });
        }

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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    return;

                }  // permission denied, boo! Disable the
                // functionality that depends on this permission.


                return;
            }
            case REQUEST_PERMISSIONS: {
                final ExamplePermissionDelegate permission = (ExamplePermissionDelegate)
                        mGeckoSession.getPermissionDelegate();
                permission.onRequestPermissionsResult(permissions, grantResults);
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void checkForAndAskForPermissions() {
        checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, CAMERA_REQUEST + 1);
        checkPermission(Manifest.permission.RECORD_AUDIO, CAMERA_REQUEST + 2);
        checkPermission(Manifest.permission.CAMERA, CAMERA_REQUEST);
    }

    private void checkPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                // Show rationale or explanation...
            } else {
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            }
        }
    }

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, getString(R.string.additional_perms), Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        }
    }

    private class ExamplePermissionDelegate implements GeckoSession.PermissionDelegate {

        public int androidPermissionRequestCode = 1;
        private Callback mCallback;

        public void onRequestPermissionsResult(final String[] permissions,
                                               final int[] grantResults) {
            if (mCallback == null) {
                return;
            }

            final Callback cb = mCallback;
            mCallback = null;
            for (final int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    // At least one permission was not granted.
                    cb.reject();
                    return;
                }
            }
            cb.grant();
        }

        @Override
        public void onAndroidPermissionsRequest(final GeckoSession session, final String[] permissions,
                                                final Callback callback) {
            if (Build.VERSION.SDK_INT >= 23) {
                // requestPermissions was introduced in API 23.
                mCallback = callback;
                requestPermissions(permissions, androidPermissionRequestCode);
            } else {
                callback.grant();
            }
        }

        public void onContentPermissionRequest(final GeckoSession session, final String uri,
                                               final int type, final Callback callback) {
            final int resId;
    /*        if (PERMISSION_GEOLOCATION == type) {
                resId = R.string.request_geolocation;
            } else if (PERMISSION_DESKTOP_NOTIFICATION == type) {
                resId = R.string.request_notification;
            } else if (PERMISSION_AUTOPLAY_MEDIA == type) {
                resId = R.string.request_autoplay;
            } else {
                Log.w(LOGTAG, "Unknown permission: " + type);
                callback.reject();
                return;
            }

            final String title = getString(resId, Uri.parse(uri).getAuthority());
            final BasicGeckoViewPrompt prompt = (BasicGeckoViewPrompt)
                    mGeckoSession.getPromptDelegate();
            prompt.onPermissionPrompt(session, title, callback);*/
            callback.grant();
        }

        @Override
        public void onMediaPermissionRequest(final GeckoSession session, final String uri,
                                             final MediaSource[] video, final MediaSource[] audio,
                                             final MediaCallback callback) {
        /*    final String host = Uri.parse(uri).getAuthority();
            final String title;
            if (audio == null) {
                title = getString(R.string.request_video, host);
            } else if (video == null) {
                title = getString(R.string.request_audio, host);
            } else {
                title = getString(R.string.request_media, host);
            }

            String[] videoNames = normalizeMediaName(video);
            String[] audioNames = normalizeMediaName(audio);

            final BasicGeckoViewPrompt prompt = (BasicGeckoViewPrompt)
                    mGeckoSession.getPromptDelegate();
            prompt.onMediaPrompt(session, title, video, audio, videoNames, audioNames, callback);*/
            String[] videoNames = normalizeMediaName(video);
            String[] audioNames = normalizeMediaName(audio);

            MediaSource v = video.length > 0 ? video[0] : null;
            MediaSource a = video.length > 0 ? audio[0]:null;
            callback.grant(v,a);
        }
    }

    private String[] normalizeMediaName(final GeckoSession.PermissionDelegate.MediaSource[] sources) {
        if (sources == null) {
            return null;
        }

        String[] res = new String[sources.length];
        for (int i = 0; i < sources.length; i++) {
            final int mediaSource = sources[i].source;
            final String name = sources[i].name;
            if (GeckoSession.PermissionDelegate.MediaSource.SOURCE_CAMERA == mediaSource) {
                if (name.toLowerCase(Locale.ROOT).contains("front")) {
                    res[i] = "Front camera";
                } else {
                    res[i] = "Back camera";
                }
            } else if (!name.isEmpty()) {
                res[i] = name;
            } else if (GeckoSession.PermissionDelegate.MediaSource.SOURCE_MICROPHONE == mediaSource) {
                res[i] = "Microphone";
            } else {
                res[i] = "Unknown source";
            }
        }

        return res;
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
                    }
                    else if (PwInput.equals("teamviewer")) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + TV_URI));
                        startActivity(intent);
                    }
                    else if (PwInput.equals("anydesk")) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + AD_URI));
                        startActivity(intent);
                    }
                     else {
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
            settingsBuilder.viewportMode(GeckoSessionSettings.VIEWPORT_MODE_DESKTOP);
        } else {
            settingsBuilder.userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE);
            settingsBuilder.viewportMode(GeckoSessionSettings.VIEWPORT_MODE_DESKTOP);
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
        final ExamplePermissionDelegate permission = new ExamplePermissionDelegate();
        permission.androidPermissionRequestCode = REQUEST_PERMISSIONS;
        mGeckoSession.setPermissionDelegate(permission);

        mGeckoView.requestFocus();
        hideKeyboard(this);
        findViewById(R.id.settingsButton).bringToFront();
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
        public void onPageStart(GeckoSession session, String url) {
        }

        @Override
        public void onProgressChange(GeckoSession session, int progress) {
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

    @Override
    public void onBackPressed() {
        mGeckoSession.goBack();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (currentApiVersion >= Build.VERSION_CODES.KITKAT && hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private class createBlockingDelegate implements ContentBlocking.Delegate {

        String ERROR = getString(R.string.an_error_occurred);
        @Override
        public void onContentBlocked(GeckoSession session, ContentBlocking.BlockEvent event) {
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
