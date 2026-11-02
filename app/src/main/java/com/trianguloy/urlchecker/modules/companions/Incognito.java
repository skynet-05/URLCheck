package com.trianguloy.urlchecker.modules.companions;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageButton;

import com.trianguloy.urlchecker.BuildConfig;
import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.generics.GenericPref.EnumerationPref;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;

/** Manages the incognito / private-browsing toggle on the open bar. */
public class Incognito {

    public static final String FIREFOX_EXTRA = IncognitoIntentHelper.FIREFOX_EXTRA;
    public static final String CHROME_EXTRA = IncognitoIntentHelper.CHROME_EXTRA;

    public static EnumerationPref<OnOffConfig> PREF(Context cntx) {
        return new EnumerationPref<>("open_incognito", OnOffConfig.AUTO, OnOffConfig.class, cntx);
    }

    private final Context context;
    private final EnumerationPref<OnOffConfig> pref;
    private boolean state = false;
    private ImageButton button;

    public Incognito(Context cntx) {
        this.context = cntx;
        this.pref = PREF(cntx);
    }

    /** Initialization from a given intent and a button to toggle */
    public void initFrom(Intent intent, ImageButton button) {
        this.button = button;
        state = switch (pref.get()) {
            case DEFAULT_ON, ALWAYS_ON -> true;
            case DEFAULT_OFF, ALWAYS_OFF -> false;
            case HIDDEN, AUTO -> IncognitoIntentHelper.isIncognitoRequested(intent);
        };

        boolean visible = switch (pref.get()) {
            case ALWAYS_ON, ALWAYS_OFF, HIDDEN -> false;
            case DEFAULT_ON, DEFAULT_OFF, AUTO -> true;
        };
        if (visible) {
            button.setVisibility(View.VISIBLE);
            AndroidUtils.longTapForDescription(button);
            button.setOnClickListener(v1 -> setState(!state));
            setState(state);
        } else {
            button.setVisibility(View.GONE);
        }
    }

    public boolean isOn() {
        return state;
    }

    /** Sets the incognito state and updates the toggle affordance. */
    public void setState(boolean state) {
        this.state = state;
        button.setImageResource(state ? R.drawable.incognito : R.drawable.no_incognito);
        button.setAlpha(state ? 1f : 0.55f);
        button.setSelected(state);
        if (BuildConfig.WHITE_LABEL) {
            button.setContentDescription(context.getString(
                    state ? R.string.mOpen_incognito_on_desc : R.string.mOpen_incognito_off_desc));
        }
    }

    /**
     * Applies private-browsing extras for the resolved target browser.
     *
     * @return true if incognito was off, or the target browser supports private mode
     */
    public boolean apply(Intent intent) {
        return IncognitoIntentHelper.applyIncognitoExtras(intent, state);
    }
}
