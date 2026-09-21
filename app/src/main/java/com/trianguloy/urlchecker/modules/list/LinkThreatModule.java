package com.trianguloy.urlchecker.modules.list;

import android.view.View;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.DescriptionConfig;
import com.trianguloy.urlchecker.threat.LinkThreatAnalyzer;
import com.trianguloy.urlchecker.url.UrlData;

import java.util.List;

/** Offline URL heuristics shown before opening / Tor preview. */
public class LinkThreatModule extends AModuleData {

    @Override
    public String getId() {
        return "linkThreat";
    }

    @Override
    public int getName() {
        return R.string.mLinkThreat_name;
    }

    @Override
    public AModuleDialog getDialog(MainDialog cntx) {
        return new LinkThreatDialog(cntx);
    }

    @Override
    public AModuleConfig getConfig(ModulesActivity cntx) {
        return new DescriptionConfig(R.string.mLinkThreat_desc);
    }
}

class LinkThreatDialog extends AModuleDialog {

    private TextView warnings;
    private int lastThreatCount;

    LinkThreatDialog(MainDialog dialog) {
        super(dialog);
    }

    @Override
    public int getLayoutId() {
        return R.layout.dialog_link_threat;
    }

    @Override
    public void onInitialize(View views) {
        warnings = views.findViewById(R.id.link_threat_warnings);
    }

    @Override
    public void onDisplayUrl(UrlData urlData) {
        List<String> flags = LinkThreatAnalyzer.analyze(urlData.url);
        lastThreatCount = flags.size();
        if (flags.isEmpty()) {
            warnings.setVisibility(View.GONE);
            getActivity().setModuleVisibility(this, false);
            return;
        }
        getActivity().setModuleVisibility(this, true);
        warnings.setVisibility(View.VISIBLE);
        StringBuilder sb = new StringBuilder();
        for (String f : flags) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("• ").append(labelFor(f));
        }
        warnings.setText(sb.toString());
    }

    int getLastThreatCount() {
        return lastThreatCount;
    }

    private String labelFor(String key) {
        int res = switch (key) {
            case "ip_literal_host" -> R.string.threat_ip_literal;
            case "suspicious_tld" -> R.string.threat_suspicious_tld;
            case "punycode_homograph" -> R.string.threat_punycode;
            case "long_query" -> R.string.threat_long_query;
            case "userinfo_trick" -> R.string.threat_userinfo;
            case "sensitive_path_pattern" -> R.string.threat_sensitive_path;
            case "very_long_url" -> R.string.threat_very_long;
            default -> R.string.threat_generic;
        };
        return getActivity().getString(res);
    }
}
