package com.trianguloy.urlchecker.routing;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.tor.TorPreviewLauncher;
import com.trianguloy.urlchecker.utilities.methods.PackageUtils;

/** Applies domain rules when opening links. */
public final class DomainRoutingHelper {

    public interface Callback {
        void onTor();

        void onClearnet();
    }

    private DomainRoutingHelper() {
    }

    public static void run(Activity activity, String url, Callback callback) {
        DomainRouteAction action = DomainRoutingEngine.resolve(activity, url);
        switch (action) {
            case TOR -> callback.onTor();
            case CLEARNET -> callback.onClearnet();
            case ASK -> ask(activity, url, callback);
        }
    }

    public static void startTorPreview(Activity activity, String url) {
        run(activity, url, new Callback() {
            @Override
            public void onTor() {
                TorPreviewLauncher.startTorPreviewDirect(activity, url);
            }

            @Override
            public void onClearnet() {
                openClearnet(activity, url);
            }
        });
    }

    public static void openWithRouting(Activity activity, String url, Runnable clearnetOpen) {
        run(activity, url, new Callback() {
            @Override
            public void onTor() {
                TorPreviewLauncher.startTorPreviewDirect(activity, url);
            }

            @Override
            public void onClearnet() {
                clearnetOpen.run();
            }
        });
    }

    private static void ask(Activity activity, String url, Callback callback) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.domain_routing_ask_title)
                .setMessage(activity.getString(R.string.domain_routing_ask_message, hostOf(url)))
                .setPositiveButton(R.string.domain_routing_tor, (d, w) -> callback.onTor())
                .setNeutralButton(R.string.domain_routing_clearnet, (d, w) -> callback.onClearnet())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public static void addRuleQuick(Activity activity, String url, DomainRouteAction action) {
        try {
            String host = hostOf(url);
            if (host == null) return;
            String pattern = host.contains(".") ? "*." + host.substring(host.indexOf('.') + 1) : host;
            DomainRoutingCatalog catalog = new DomainRoutingCatalog(activity);
            JSONObjectWrapper(catalog, pattern, action);
        } catch (Exception ignored) {
        }
    }

    private static void JSONObjectWrapper(DomainRoutingCatalog catalog, String pattern, DomainRouteAction action) throws Exception {
        var cat = catalog.getCatalog();
        cat.put(pattern, action.toCatalogValue());
        catalog.save(cat);
    }

    private static String hostOf(String url) {
        try {
            return Uri.parse(url).getHost();
        } catch (Exception e) {
            return url;
        }
    }

    private static void openClearnet(Activity activity, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        PackageUtils.startActivity(intent, R.string.toast_noApp, activity);
    }
}
