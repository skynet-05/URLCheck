package com.trianguloy.urlchecker.modules.companions;

import android.content.Context;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.generics.GenericPref.StringPref;

import java.util.ArrayList;
import java.util.List;

/** Fallback when the open-bar browser cannot open a private tab from a third-party app. */
public final class PrivateLinkBrowser {

    public static final String PKG_FIREFOX = "org.mozilla.firefox";
    /** Pref value: copy URL and guide user to paste in Brave private tab. */
    public static final String MODE_BRAVE_COPY = "__brave_copy_private__";

    private PrivateLinkBrowser() {
    }

    public static StringPref PREF(Context cntx) {
        return new StringPref("open_private_link_browser_pkg", "", cntx);
    }

    public static boolean isInstalled(Context context, String packageName) {
        if (packageName == null || packageName.isEmpty()) return false;
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /** Firefox redirect when explicit Firefox mode is selected and installed. */
    public static String resolveFirefoxRedirect(Context context) {
        String saved = PREF(context).get();
        if (PKG_FIREFOX.equals(saved) && isInstalled(context, PKG_FIREFOX)) {
            return PKG_FIREFOX;
        }
        return "";
    }

    /**
     * Copy+paste Brave flow when private browsing is on and the user opens with Brave,
     * unless they explicitly chose Firefox in settings.
     */
    public static boolean useBraveCopyFlow(Context context, String targetPackage) {
        if (!BravePrivateTabHelper.isBravePackage(targetPackage)) return false;
        String mode = PREF(context).get();
        if (PKG_FIREFOX.equals(mode)) return false;
        if (MODE_BRAVE_COPY.equals(mode)) return true;
        return mode == null || mode.isEmpty();
    }

    public static void attachToSpinner(Spinner spinner, Context context) {
        var entries = buildEntries(context);
        var labels = new ArrayList<String>();
        var packages = new ArrayList<String>();
        for (var entry : entries) {
            labels.add(entry.label);
            packages.add(entry.packageName);
        }
        var adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        String current = PREF(context).get();
        int index = packages.indexOf(current);
        if (index < 0) index = 0;
        spinner.setSelection(index);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PREF(context).set(packages.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private static List<Entry> buildEntries(Context context) {
        var list = new ArrayList<Entry>();
        list.add(new Entry(context.getString(R.string.mOpen_private_browser_none), ""));
        list.add(new Entry(context.getString(R.string.mOpen_private_browser_brave_copy), MODE_BRAVE_COPY));
        list.add(new Entry(context.getString(R.string.mOpen_private_browser_firefox), PKG_FIREFOX));
        return list;
    }

    private record Entry(String label, String packageName) {
    }
}
