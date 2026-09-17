package com.trianguloy.urlchecker.tor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.trianguloy.urlchecker.R;

/**
 * Minimal Orbot integration: requests Tor startup and listens for STATUS broadcasts.
 * Does not route clearnet traffic when Orbot is unavailable — callers must handle errors.
 */
public final class OrbotTorHelper {

    public static final String ORBOT_PACKAGE = "org.torproject.android";

    public static final String ACTION_START = "org.torproject.android.intent.action.START";
    public static final String ACTION_STATUS = "org.torproject.android.intent.action.STATUS";
    public static final String EXTRA_STATUS = "org.torproject.android.intent.extra.STATUS";
    public static final String EXTRA_PACKAGE_NAME = "org.torproject.android.intent.extra.PACKAGE_NAME";
    public static final String EXTRA_HTTP_PROXY_HOST = "org.torproject.android.intent.extra.HTTP_PROXY_HOST";
    public static final String EXTRA_HTTP_PROXY_PORT = "org.torproject.android.intent.extra.HTTP_PROXY_PORT";

    public static final String STATUS_ON = "ON";
    public static final String STATUS_OFF = "OFF";
    public static final String STATUS_STARTS_DISABLED = "STARTS_DISABLED";

    public static final String DEFAULT_PROXY_HOST = "127.0.0.1";
    public static final int DEFAULT_HTTP_PROXY_PORT = 8118;

    public record TorProxy(String host, int httpPort) {
    }

    public interface Callback {
        void onTorReady(TorProxy proxy);

        void onTorError(int messageResId);
    }

    private static final long STATUS_TIMEOUT_MS = 90_000;

    private OrbotTorHelper() {
    }

    public static boolean isOrbotInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo(ORBOT_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Ask Orbot for status / startup and invoke callback when Tor HTTP proxy is available.
     * Unregisters the status receiver when done or on timeout.
     */
    public static void ensureTorRunning(Context context, Callback callback) {
        if (!isOrbotInstalled(context)) {
            callback.onTorError(R.string.tor_orbot_missing);
            return;
        }

        Handler main = new Handler(Looper.getMainLooper());
        Context app = context.getApplicationContext();

        final BroadcastReceiver[] receiverHolder = new BroadcastReceiver[1];
        final Runnable[] timeoutHolder = new Runnable[1];

        receiverHolder[0] = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (intent == null || !ACTION_STATUS.equals(intent.getAction())) return;

                String status = intent.getStringExtra(EXTRA_STATUS);
                if (status == null) return;

                if (STATUS_ON.equals(status)) {
                    cleanup();
                    String host = intent.getStringExtra(EXTRA_HTTP_PROXY_HOST);
                    int port = intent.getIntExtra(EXTRA_HTTP_PROXY_PORT, DEFAULT_HTTP_PROXY_PORT);
                    if (host == null || host.isEmpty()) host = DEFAULT_PROXY_HOST;
                    callback.onTorReady(new TorProxy(host, port));
                } else if (STATUS_STARTS_DISABLED.equals(status)) {
                    cleanup();
                    callback.onTorError(R.string.tor_orbot_starts_disabled);
                } else if (STATUS_OFF.equals(status)) {
                    requestOrbotStart(app);
                }
            }

            private void cleanup() {
                main.removeCallbacks(timeoutHolder[0]);
                try {
                    app.unregisterReceiver(receiverHolder[0]);
                } catch (IllegalArgumentException ignored) {
                }
            }
        };

        timeoutHolder[0] = () -> {
            try {
                app.unregisterReceiver(receiverHolder[0]);
            } catch (IllegalArgumentException ignored) {
            }
            callback.onTorError(R.string.tor_orbot_timeout);
        };

        IntentFilter filter = new IntentFilter(ACTION_STATUS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            app.registerReceiver(receiverHolder[0], filter, Context.RECEIVER_EXPORTED);
        } else {
            app.registerReceiver(receiverHolder[0], filter);
        }
        main.postDelayed(timeoutHolder[0], STATUS_TIMEOUT_MS);

        requestOrbotStart(app);
    }

    private static void requestOrbotStart(Context context) {
        Intent start = new Intent(ACTION_START);
        start.setPackage(ORBOT_PACKAGE);
        start.putExtra(EXTRA_PACKAGE_NAME, context.getPackageName());
        context.sendBroadcast(start);
    }
}
