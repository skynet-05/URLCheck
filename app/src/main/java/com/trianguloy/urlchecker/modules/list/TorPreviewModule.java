package com.trianguloy.urlchecker.modules.list;

import android.view.View;
import android.widget.Button;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.AutomationRules;
import com.trianguloy.urlchecker.modules.DescriptionConfig;
import com.trianguloy.urlchecker.tor.TorPreviewLauncher;
import com.trianguloy.urlchecker.url.UrlData;

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
        preview.setOnClickListener(v -> TorPreviewLauncher.start(getActivity(), getUrl()));
    }

    @Override
    public void onDisplayUrl(UrlData urlData) {
        preview.setEnabled(urlData.url != null && !urlData.url.isBlank());
    }
}
