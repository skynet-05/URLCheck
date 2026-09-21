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
    public static final String ACTION_NEWNYM = "org.torproject.android.intent.action.NEWNYM";
    public static final String ACTION_STATUS = "org.torproject.android.intent.action.STATUS";
    public static final String EXTRA_STATUS = "org.torproject.android.intent.extra.STATUS";
    public static final String EXTRA_PACKAGE_NAME = "org.torproject.android.intent.extra.PACKAGE_NAME";
    public static final String EXTRA_HTTP_PROXY_HOST = "org.torproject.android.intent.extra.HTTP_PROXY_HOST";
    public static final String EXTRA_HTTP_PROXY_PORT = "org.torproject.android.intent.extra.HTTP_PROXY_PORT";

    public static final String STATUS_ON = "ON";
    public static final String STATUS_OFF = "OFF";
    public static final String STATUS_STARTING = "STARTING";
    public static final String STATUS_STOPPING = "STOPPING";
    public static final String STATUS_STARTS_DISABLED = "STARTS_DISABLED";

    public static final String DEFAULT_PROXY_HOST = "127.0.0.1";
    public static final int DEFAULT_HTTP_PROXY_PORT = 8118;

    public static final long PREVIEW_START_TIMEOUT_MS = 90_000;
    public static final long STATUS_UI_TIMEOUT_MS = 12_000;

    public record TorProxy(String host, int httpPort) {
    }

    public interface Callback {
        void onTorReady(TorProxy proxy);

        void onTorError(int messageResId);
    }

    /** Cancels an in-flight {@link #queryTorStatus} request. */
    public static final class QuerySession {
        private BroadcastReceiver receiver;
        private Runnable timeoutRunnable;
        private Handler main;
        private Context app;
        private boolean finished;

        private void finish() {
            if (finished) return;
            finished = true;
            if (main != null && timeoutRunnable != null) {
                main.removeCallbacks(timeoutRunnable);
            }
            if (app != null && receiver != null) {
                try {
                    app.unregisterReceiver(receiver);
                } catch (IllegalArgumentException ignored) {
                }
            }
            receiver = null;
            timeoutRunnable = null;
        }

        public void cancel() {
            finish();
        }
    }

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
     */
    public static QuerySession ensureTorRunning(Context context, Callback callback) {
        return queryTorStatus(context, true, PREVIEW_START_TIMEOUT_MS, callback);
    }

    /**
     * Listens for Orbot STATUS. When {@code startIfOff} is false, OFF is reported immediately without
     * asking Orbot to start (for passive UI). When true, sends START until ON or failure.
     */
    public static QuerySession queryTorStatus(
            Context context,
            boolean startIfOff,
            long timeoutMs,
            Callback callback) {

        if (!isOrbotInstalled(context)) {
            callback.onTorError(R.string.tor_status_orbot_missing);
            return new QuerySession();
        }

        QuerySession session = new QuerySession();
        session.main = new Handler(Looper.getMainLooper());
        session.app = context.getApplicationContext();
        session.finished = false;

        session.receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (session.finished || intent == null || !ACTION_STATUS.equals(intent.getAction())) return;

                String status = intent.getStringExtra(EXTRA_STATUS);
                if (status == null) return;

                if (STATUS_ON.equals(status)) {
                    session.finish();
                    String host = intent.getStringExtra(EXTRA_HTTP_PROXY_HOST);
                    int port = intent.getIntExtra(EXTRA_HTTP_PROXY_PORT, DEFAULT_HTTP_PROXY_PORT);
                    if (host == null || host.isEmpty()) host = DEFAULT_PROXY_HOST;
                    callback.onTorReady(new TorProxy(host, port));
                } else if (STATUS_STARTS_DISABLED.equals(status)) {
                    session.finish();
                    callback.onTorError(R.string.tor_orbot_starts_disabled);
                } else if (STATUS_OFF.equals(status)) {
                    if (startIfOff) {
                        requestOrbotStart(session.app);
                    } else {
                        session.finish();
                        callback.onTorError(R.string.tor_status_off);
                    }
                } else if (STATUS_STOPPING.equals(status)) {
                    session.finish();
                    callback.onTorError(R.string.tor_status_off);
                }
                // STARTING: wait for ON or timeout
            }
        };

        session.timeoutRunnable = () -> {
            session.finish();
            callback.onTorError(R.string.tor_orbot_timeout);
        };

        IntentFilter filter = new IntentFilter(ACTION_STATUS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            session.app.registerReceiver(session.receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            session.app.registerReceiver(session.receiver, filter);
        }
        session.main.postDelayed(session.timeoutRunnable, timeoutMs);

        requestOrbotStart(session.app);
        return session;
    }

    private static void requestOrbotStart(Context context) {
        Intent start = new Intent(ACTION_START);
        start.setPackage(ORBOT_PACKAGE);
        start.putExtra(EXTRA_PACKAGE_NAME, context.getPackageName());
        context.sendBroadcast(start);
    }

    /** Ask Orbot to build a new Tor circuit (new identity). */
    public static void requestNewCircuit(Context context) {
        if (!isOrbotInstalled(context)) return;
        Intent nym = new Intent(ACTION_NEWNYM);
        nym.setPackage(ORBOT_PACKAGE);
        context.sendBroadcast(nym);
    }
}
