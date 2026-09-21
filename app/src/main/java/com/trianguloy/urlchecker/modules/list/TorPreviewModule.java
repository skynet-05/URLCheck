package com.trianguloy.urlchecker.modules.list;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import com.trianguloy.urlchecker.tor.TorStatusCoordinator;
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

class TorPreviewDialog extends AModuleDialog implements TorStatusCoordinator.Host {

    static final List<AutomationRules.Automation<TorPreviewDialog>> AUTOMATIONS = List.of(
            new AutomationRules.Automation<>("torPreview", R.string.auto_torPreview, dialog ->
                    TorPreviewLauncher.start(dialog.getActivity(), dialog.getUrl()))
    );

    private Button preview;
    private Button newCircuit;
    private ImageView statusIcon;
    private TextView statusText;
    private ProgressBar statusProgress;
    private View statusRow;
    private TorStatusCoordinator torStatus;

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
        statusProgress = views.findViewById(R.id.tor_status_progress);
        statusRow = views.findViewById(R.id.tor_status_row);

        torStatus = new TorStatusCoordinator(this);

        preview.setOnClickListener(v -> TorPreviewLauncher.start(getActivity(), getUrl()));
        newCircuit.setOnClickListener(v -> torStatus.requestNewCircuit(null));
        statusRow.setOnClickListener(v -> onStatusTapped());
        AndroidUtils.longTapForDescription(statusRow);

        torStatus.refresh(false);
    }

    @Override
    public void onDisplayUrl(UrlData urlData) {
        preview.setEnabled(urlData.url != null && !urlData.url.isBlank());
        if (!torStatus.isCircuitBusy()) {
            torStatus.refresh(false);
        }
    }

    private void onStatusTapped() {
        if (!OrbotTorHelper.isOrbotInstalled(getActivity())) {
            OrbotOnboarding.showMissing(getActivity());
            return;
        }
        torStatus.refresh(true);
    }

    @Override
    public android.app.Activity torHostActivity() {
        return getActivity();
    }

    @Override
    public ImageView statusIcon() {
        return statusIcon;
    }

    @Override
    public TextView statusText() {
        return statusText;
    }

    @Override
    public ProgressBar statusProgress() {
        return statusProgress;
    }

    @Override
    public View[] circuitControls() {
        return new View[]{newCircuit, preview};
    }
}
