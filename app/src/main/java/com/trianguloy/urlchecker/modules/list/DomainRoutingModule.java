package com.trianguloy.urlchecker.modules.list;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.DescriptionConfig;
import com.trianguloy.urlchecker.routing.DomainRouteAction;
import com.trianguloy.urlchecker.routing.DomainRoutingEngine;
import com.trianguloy.urlchecker.routing.DomainRoutingHelper;
import com.trianguloy.urlchecker.url.UrlData;

/** Shows domain routing decision and quick-add rules. */
public class DomainRoutingModule extends AModuleData {

    @Override
    public String getId() {
        return "domainRouting";
    }

    @Override
    public int getName() {
        return R.string.mDomainRouting_name;
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    public AModuleDialog getDialog(MainDialog cntx) {
        return new DomainRoutingDialog(cntx);
    }

    @Override
    public AModuleConfig getConfig(ModulesActivity cntx) {
        return new DescriptionConfig(R.string.mDomainRouting_desc);
    }
}

class DomainRoutingDialog extends AModuleDialog {

    private TextView routeLabel;

    DomainRoutingDialog(MainDialog dialog) {
        super(dialog);
    }

    @Override
    public int getLayoutId() {
        return R.layout.dialog_domain_routing;
    }

    @Override
    public void onInitialize(View views) {
        routeLabel = views.findViewById(R.id.domain_route_label);
        Button tor = views.findViewById(R.id.domain_add_tor);
        Button clear = views.findViewById(R.id.domain_add_clearnet);
        Button edit = views.findViewById(R.id.domain_edit_rules);
        tor.setOnClickListener(v -> DomainRoutingHelper.addRuleQuick(getActivity(), getUrl(), DomainRouteAction.TOR));
        clear.setOnClickListener(v -> DomainRoutingHelper.addRuleQuick(getActivity(), getUrl(), DomainRouteAction.CLEARNET));
        edit.setOnClickListener(v -> new com.trianguloy.urlchecker.routing.DomainRoutingCatalog(getActivity()).showEditor());
    }

    @Override
    public void onDisplayUrl(UrlData urlData) {
        DomainRouteAction action = DomainRoutingEngine.resolve(getActivity(), urlData.url);
        int label = switch (action) {
            case TOR -> R.string.domain_routing_tor;
            case CLEARNET -> R.string.domain_routing_clearnet;
            case ASK -> R.string.domain_routing_ask;
        };
        routeLabel.setText(getActivity().getString(R.string.domain_routing_current, getActivity().getString(label)));
    }
}
