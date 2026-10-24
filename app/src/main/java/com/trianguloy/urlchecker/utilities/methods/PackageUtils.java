package com.trianguloy.urlchecker.utilities.methods;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.trianguloy.urlchecker.utilities.UserFacingErrors;

/** Static utilities related to packages */
public interface PackageUtils {

    /** Wrapper for {@link Context#startActivity(Intent)} to catch thrown exceptions and show a toast instead */
    static void startActivity(Intent intent, int toastError, Context cntx) {
        try {
            cntx.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(cntx, toastMessage(cntx, toastError), Toast.LENGTH_SHORT).show();
        }
    }

    /** Wrapper for {@link Activity#startActivityForResult(Intent, int)} to catch thrown exceptions and show a toast instead */
    static void startActivityForResult(Intent intent, int requestCode, int toastError, Activity cntx) {
        try {
            cntx.startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            Toast.makeText(cntx, toastMessage(cntx, toastError), Toast.LENGTH_SHORT).show();
        }
    }

    private static String toastMessage(Context cntx, int toastError) {
        if (toastError != 0) {
            try {
                return cntx.getString(toastError);
            } catch (Exception ignored) {
            }
        }
        return UserFacingErrors.noApp(cntx);
    }
}
