package io.agora.openvcall.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import io.agora.openvcall.AGApplication;
import io.agora.openvcall.model.*;
import io.agora.rtc.RtcEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public abstract class BaseActivity extends AppCompatActivity {
    private final static Logger log = LoggerFactory.getLogger(BaseActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUIAndEvent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        deInitUIAndEvent();
    }

    protected abstract void initUIAndEvent();

    protected abstract void deInitUIAndEvent();

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        new Handler().postDelayed(() -> {
            if (isFinishing()) return;
            checkSelfPermissions();
        }, 500);
    }

    private boolean checkSelfPermissions() {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO, ConstantApp.PERMISSION_REQ_ID_RECORD_AUDIO) &&
                checkSelfPermission(Manifest.permission.CAMERA, ConstantApp.PERMISSION_REQ_ID_CAMERA) &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, ConstantApp.PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE);
    }

    public boolean checkSelfPermission(String permission, int requestCode) {
        log.debug("checkSelfPermission " + permission + " " + requestCode);
        if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{permission}, requestCode);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        log.debug("onRequestPermissionsResult " + requestCode + " " + Arrays.toString(permissions) + " " + Arrays.toString(grantResults));
        switch (requestCode) {
            case ConstantApp.PERMISSION_REQ_ID_RECORD_AUDIO: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkSelfPermission(Manifest.permission.CAMERA, ConstantApp.PERMISSION_REQ_ID_CAMERA);
                } else {
                    finish();
                }
                break;
            }
            case ConstantApp.PERMISSION_REQ_ID_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, ConstantApp.PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE);
                } else {
                    finish();
                }
                break;
            }
            case ConstantApp.PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    finish();
                }
                break;
            }
        }
    }

    protected AGApplication application() {
        return (AGApplication) getApplication();
    }

    protected RtcEngine rtcEngine() {
        return application().getWorker().getRtcEngine();
    }

    protected final RtcWorker worker() {
        return application().getWorker();
    }

    protected final EngineConfig config() {
        return worker().getEngineConfig();
    }

    protected final MyEngineEventHandler event() {
        return worker().eventHandler();
    }

    protected void closeIME(View v) {
        InputMethodManager mgr = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
        v.clearFocus();
    }

    protected void showLongToast(final String msg) {
        runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show(); });
    }

    protected int virtualKeyHeight() {
        boolean hasPermanentMenuKey = ViewConfiguration.get(getApplication()).hasPermanentMenuKey();
        if (hasPermanentMenuKey) {
            return 0;
        }

        // Also can use getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        DisplayMetrics metrics = new DisplayMetrics();
        Display display = getWindowManager().getDefaultDisplay();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            display.getRealMetrics(metrics);
        } else {
            display.getMetrics(metrics);
        }

        int fullHeight = metrics.heightPixels;
        int fullWidth = metrics.widthPixels;

        if (fullHeight < fullWidth) {
            fullHeight ^= fullWidth;
            fullWidth ^= fullHeight;
            fullHeight ^= fullWidth;
        }

        display.getMetrics(metrics);

        int newFullHeight = metrics.heightPixels;
        int newFullWidth = metrics.widthPixels;

        if (newFullHeight < newFullWidth) {
            newFullHeight ^= newFullWidth;
            newFullWidth ^= newFullHeight;
            newFullHeight ^= newFullWidth;
        }

        int virtualKeyHeight = fullHeight - newFullHeight;

        if (virtualKeyHeight > 0) {
            return virtualKeyHeight;
        }

        virtualKeyHeight = fullWidth - newFullWidth;

        return virtualKeyHeight;
    }

    protected final int getStatusBarHeight() {
        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier(
                "status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }

        if (statusBarHeight == 0) {
            log.error("Can not get height of status bar");
        }

        return statusBarHeight;
    }

    protected final int getActionBarHeight() {
        final TypedArray styledAttributes = this.getTheme().obtainStyledAttributes(
                new int[]{ android.R.attr.actionBarSize });
        int actionBarHeight = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();
        if (actionBarHeight == 0) {
            log.error("Can not get height of action bar");
        }

        return actionBarHeight;
    }
}
