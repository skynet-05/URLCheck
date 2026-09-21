package com.trianguloy.urlchecker.tor;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;

/**
 * Tor status row: checking, ready, error, and new-circuit in-progress states.
 */
public final class TorStatusCoordinator {

    public static final long NEW_CIRCUIT_SETTLE_MS = 3_000L;

    public interface Host {
        Activity torHostActivity();

        ImageView statusIcon();

        TextView statusText();

        ProgressBar statusProgress();

        /** New circuit / retry controls to disable while busy. */
        View[] circuitControls();
    }

    private final Host host;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private OrbotTorHelper.QuerySession statusQuery;
    private Runnable afterNymRunnable;
    private boolean circuitBusy;
    private boolean showOnboardingOnError;

    public TorStatusCoordinator(Host host) {
        this.host = host;
    }

    public boolean isCircuitBusy() {
        return circuitBusy;
    }

    public void refresh(boolean startIfOff) {
        if (circuitBusy) return;
        showOnboardingOnError = startIfOff;
        cancelQuery();
        applyChecking();

        Activity activity = host.torHostActivity();
        if (!OrbotTorHelper.isOrbotInstalled(activity)) {
            applyError(R.string.tor_status_orbot_missing);
            return;
        }

        statusQuery = OrbotTorHelper.queryTorStatus(
                activity,
                startIfOff,
                OrbotTorHelper.STATUS_UI_TIMEOUT_MS,
                new OrbotTorHelper.Callback() {
                    @Override
                    public void onTorReady(OrbotTorHelper.TorProxy proxy) {
                        activity.runOnUiThread(() -> applyReady());
                    }

                    @Override
                    public void onTorError(int messageResId) {
                        activity.runOnUiThread(() -> {
                            applyError(messageResId);
                            if (showOnboardingOnError) {
                                OrbotOnboarding.showNotReady(activity, messageResId);
                            }
                        });
                    }
                });
    }

    /**
     * Sends NEWNYM, shows pending UI, re-queries Orbot, then runs {@code onSettled} on success.
     */
    public void requestNewCircuit(Runnable onSettled) {
        if (circuitBusy) return;
        Activity activity = host.torHostActivity();
        if (!OrbotTorHelper.isOrbotInstalled(activity)) {
            applyError(R.string.tor_status_orbot_missing);
            return;
        }

        circuitBusy = true;
        setCircuitControlsEnabled(false);
        cancelQuery();
        applyNewCircuit();

        OrbotTorHelper.requestNewCircuit(activity);
        afterNymRunnable = () -> {
            afterNymRunnable = null;
            statusQuery = OrbotTorHelper.queryTorStatus(
                    activity,
                    true,
                    OrbotTorHelper.STATUS_UI_TIMEOUT_MS,
                    new OrbotTorHelper.Callback() {
                        @Override
                        public void onTorReady(OrbotTorHelper.TorProxy proxy) {
                            activity.runOnUiThread(() -> {
                                finishCircuitBusy();
                                applyReady();
                                if (onSettled != null) onSettled.run();
                            });
                        }

                        @Override
                        public void onTorError(int messageResId) {
                            activity.runOnUiThread(() -> {
                                finishCircuitBusy();
                                applyError(messageResId);
                            });
                        }
                    });
        };
        handler.postDelayed(afterNymRunnable, NEW_CIRCUIT_SETTLE_MS);
    }

    public void destroy() {
        cancelQuery();
        if (afterNymRunnable != null) {
            handler.removeCallbacks(afterNymRunnable);
            afterNymRunnable = null;
        }
        finishCircuitBusy();
    }

    private void finishCircuitBusy() {
        circuitBusy = false;
        setCircuitControlsEnabled(true);
    }

    private void setCircuitControlsEnabled(boolean enabled) {
        View[] controls = host.circuitControls();
        if (controls == null) return;
        for (View v : controls) {
            if (v != null) v.setEnabled(enabled);
        }
    }

    private void cancelQuery() {
        if (statusQuery != null) {
            statusQuery.cancel();
            statusQuery = null;
        }
    }

    private void applyChecking() {
        setProgressVisible(true);
        host.statusIcon().setImageResource(R.drawable.tor_status_pending);
        host.statusText().setText(R.string.tor_status_checking);
        host.statusIcon().setContentDescription(host.torHostActivity().getString(R.string.tor_status_checking));
    }

    private void applyNewCircuit() {
        setProgressVisible(true);
        host.statusIcon().setImageResource(R.drawable.tor_status_pending);
        host.statusText().setText(R.string.tor_status_new_circuit);
        host.statusIcon().setContentDescription(host.torHostActivity().getString(R.string.tor_status_new_circuit));
    }

    private void applyReady() {
        setProgressVisible(false);
        host.statusIcon().setImageResource(R.drawable.tor_status_ok);
        host.statusText().setText(R.string.tor_status_ready);
        host.statusIcon().setContentDescription(host.torHostActivity().getString(R.string.tor_status_ready));
    }

    private void applyError(int messageResId) {
        setProgressVisible(false);
        host.statusIcon().setImageResource(R.drawable.tor_status_error);
        host.statusText().setText(messageResId);
        host.statusIcon().setContentDescription(host.torHostActivity().getString(messageResId));
    }

    private void setProgressVisible(boolean visible) {
        ProgressBar bar = host.statusProgress();
        if (bar != null) {
            bar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}
