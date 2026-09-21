package com.trianguloy.urlchecker.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.history.RecentLinksStore;
import com.trianguloy.urlchecker.utilities.AndroidSettings;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.LocaleUtils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Searchable local link history. */
public class RecentLinksActivity extends Activity {

    private List<RecentLinksStore.Entry> all = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private ListView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidSettings.setTheme(this, false);
        LocaleUtils.setLocale(this);
        setContentView(R.layout.activity_recent_links);
        setTitle(R.string.recent_links_title);
        AndroidUtils.configureUp(this);

        list = findViewById(R.id.recent_links_list);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        list.setAdapter(adapter);
        list.setOnItemClickListener(this::openEntry);
        list.setOnItemLongClickListener(this::deleteEntry);

        findViewById(R.id.recent_links_wipe).setOnClickListener(v -> confirmWipe());
        EditText search = findViewById(R.id.recent_links_search);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        reload();
    }

    private void reload() {
        all = RecentLinksStore.list(this);
        filter("");
    }

    private void filter(String query) {
        String q = query.toLowerCase(Locale.ROOT);
        List<String> lines = new ArrayList<>();
        for (RecentLinksStore.Entry e : all) {
            if (!q.isEmpty() && !e.url().toLowerCase(Locale.ROOT).contains(q)) continue;
            lines.add(formatLine(e));
        }
        adapter.clear();
        adapter.addAll(lines);
        adapter.notifyDataSetChanged();
    }

    private String formatLine(RecentLinksStore.Entry e) {
        String when = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(e.timestampMs()));
        return when + "\n" + e.url();
    }

    private void openEntry(AdapterView<?> parent, View view, int position, long id) {
        String line = adapter.getItem(position);
        if (line == null) return;
        int idx = line.indexOf('\n');
        String url = idx >= 0 ? line.substring(idx + 1) : line;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setPackage(getPackageName());
        startActivity(intent);
    }

    private boolean deleteEntry(AdapterView<?> parent, View view, int position, long id) {
        String line = adapter.getItem(position);
        if (line == null) return true;
        int idx = line.indexOf('\n');
        String url = idx >= 0 ? line.substring(idx + 1) : line;
        RecentLinksStore.delete(this, url);
        reload();
        return true;
    }

    private void confirmWipe() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.recent_links_wipe_confirm)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    RecentLinksStore.wipeAll(this);
                    reload();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
