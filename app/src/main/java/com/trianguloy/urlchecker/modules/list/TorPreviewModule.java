package com.trianguloy.urlchecker.modules.list;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.AutomationRules;
import com.trianguloy.urlchecker.modules.DescriptionConfig;
import com.trianguloy.urlchecker.tor.OrbotOnboarding;
import com.trianguloy.urlchecker.tor.OrbotTorHelper;
import com.trianguloy.urlchecker.tor.TorPreviewLauncher;
import com.trianguloy.urlchecker.url.UrlData;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;

import java.util.List;

/** Preview the current URL in a Tor-routed sandbox before opening in the default browser. */
public class TorPreviewModule extends AModuleData {

    @Override
    public String getId() {
        return "torPreview";
    }

    @Override
    public int getName() {
        return R.string.mTorPreview_name;
    }

    @Override
    public AModuleDialog getDialog(MainDialog cntx) {
        return new TorPreviewDialog(cntx);
    }

    @Override
    public AModuleConfig getConfig(ModulesActivity cntx) {
        return new DescriptionConfig(R.string.mTorPreview_desc);
    }

    @Override
    public List<AutomationRules.Automation<AModuleDialog>> getAutomations() {
        return (List<AutomationRules.Automation<AModuleDialog>>) (List<?>) TorPreviewDialog.AUTOMATIONS;
    }
}

class TorPreviewDialog extends AModuleDialog {

    static final List<AutomationRules.Automation<TorPreviewDialog>> AUTOMATIONS = List.of(
            new AutomationRules.Automation<>("torPreview", R.string.auto_torPreview, dialog ->
                    TorPreviewLauncher.start(dialog.getActivity(), dialog.getUrl()))
    );

    private Button preview;
    private Button newCircuit;
    private ImageView statusIcon;
    private TextView statusText;
    private View statusRow;
    private OrbotTorHelper.QuerySession statusQuery;

    public TorPreviewDialog(MainDialog dialog) {
        super(dialog);
    }

    @Override
    public int getLayoutId() {
        return R.layout.dialog_tor_preview;
    }

    @Override
    public void onInitialize(View views) {
        preview = views.findViewById(R.id.tor_preview);
        newCircuit = views.findViewById(R.id.tor_new_circuit_btn);
        statusIcon = views.findViewById(R.id.tor_status_icon);
        statusText = views.findViewById(R.id.tor_status_text);
        statusRow = views.findViewById(R.id.tor_status_row);

        preview.setOnClickListener(v -> TorPreviewLauncher.start(getActivity(), getUrl()));
        newCircuit.setOnClickListener(v -> {
            OrbotTorHelper.requestNewCircuit(getActivity());
            android.widget.Toast.makeText(getActivity(), R.string.tor_new_circuit_sent, android.widget.Toast.LENGTH_SHORT).show();
        });
        statusRow.setOnClickListener(v -> onStatusTapped());
        AndroidUtils.longTapForDescription(statusRow);

        refreshTorStatus(false);
    }

    @Override
    public void onDisplayUrl(UrlData urlData) {
        preview.setEnabled(urlData.url != null && !urlData.url.isBlank());
        refreshTorStatus(false);
    }

    private void onStatusTapped() {
        if (!OrbotTorHelper.isOrbotInstalled(getActivity())) {
            OrbotOnboarding.showMissing(getActivity());
            return;
        }
        refreshTorStatus(true);
    }

    private void refreshTorStatus(boolean startIfOff) {
        cancelStatusQuery();
        setStatusUi(false, R.string.tor_status_checking);

        if (!OrbotTorHelper.isOrbotInstalled(getActivity())) {
            setStatusUi(false, R.string.tor_status_orbot_missing);
            return;
        }

        statusQuery = OrbotTorHelper.queryTorStatus(
                getActivity(),
                startIfOff,
                OrbotTorHelper.STATUS_UI_TIMEOUT_MS,
                new OrbotTorHelper.Callback() {
                    @Override
                    public void onTorReady(OrbotTorHelper.TorProxy proxy) {
                        getActivity().runOnUiThread(() -> setStatusUi(true, R.string.tor_status_ready));
                    }

                    @Override
                    public void onTorError(int messageResId) {
                        getActivity().runOnUiThread(() -> {
                            setStatusUi(false, messageResId);
                            if (startIfOff) OrbotOnboarding.showNotReady(getActivity(), messageResId);
                        });
                    }
                });
    }

    private void cancelStatusQuery() {
        if (statusQuery != null) {
            statusQuery.cancel();
            statusQuery = null;
        }
    }

    private void setStatusUi(boolean ready, int messageResId) {
        statusIcon.setImageResource(ready ? R.drawable.tor_status_ok : R.drawable.tor_status_error);
        statusText.setText(messageResId);
        statusIcon.setContentDescription(getActivity().getString(messageResId));
    }
}
