package com.trianguloy.urlchecker.modules.list;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.activities.RecentLinksActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.history.RecentLinksStore;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.DescriptionConfig;
import com.trianguloy.urlchecker.threat.LinkThreatAnalyzer;
import com.trianguloy.urlchecker.url.UrlData;

/** Records checked URLs locally (retention configurable in Link Guard settings). */
public class RecentLinksModule extends AModuleData {

    @Override
    public String getId() {
        return "recentLinks";
    }

    @Override
    public int getName() {
        return R.string.mRecentLinks_name;
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    public AModuleDialog getDialog(MainDialog cntx) {
        return new RecentLinksDialog(cntx);
    }

    @Override
    public AModuleConfig getConfig(ModulesActivity cntx) {
        return new DescriptionConfig(R.string.mRecentLinks_desc);
    }

    static void recordUrl(MainDialog dialog, String url) {
        int threats = LinkThreatAnalyzer.analyze(url).size();
        RecentLinksStore.record(dialog, url, threats);
    }
}

class RecentLinksDialog extends AModuleDialog {

    private TextView hint;

    RecentLinksDialog(MainDialog dialog) {
        super(dialog);
    }

    @Override
    public int getLayoutId() {
        return R.layout.dialog_recent_links;
    }

    @Override
    public void onInitialize(View views) {
        hint = views.findViewById(R.id.recent_links_hint);
        Button open = views.findViewById(R.id.recent_links_open);
        open.setOnClickListener(v -> getActivity().startActivity(new Intent(getActivity(), RecentLinksActivity.class)));
    }

    @Override
    public void onFinishUrl() {
        RecentLinksModule.recordUrl(getActivity(), getUrl());
    }

    @Override
    public void onDisplayUrl(UrlData urlData) {
        if (!RecentLinksStore.ENABLED(getActivity()).get()) {
            getActivity().setModuleVisibility(this, false);
            return;
        }
        hint.setText(R.string.mRecentLinks_hint);
    }
}
