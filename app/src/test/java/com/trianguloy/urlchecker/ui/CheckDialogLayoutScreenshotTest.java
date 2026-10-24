package com.trianguloy.urlchecker.ui;

import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.AndroidSettings;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;

/** JVM-rendered previews of check-dialog sections (not a live device capture). */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "night-notlong-notround-any-420dpi-normal")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CheckDialogLayoutScreenshotTest {

    private static final int WIDTH_PX = 1080;

    @Test
    public void renderRoutingAndTorSections() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        AndroidSettings.setTheme(activity, false);

        Path outDir = Path.of("/cursor/stores/self/linkguard-4.0.3-screenshots");
        outDir.toFile().mkdirs();

        save(bindRouting(activity), outDir.resolve("check-routing-tor-sections.png"));
        save(bindTor(activity), outDir.resolve("check-tor-actions.png"));
        save(bindHistory(activity), outDir.resolve("check-history-note.png"));

        assertTrue(outDir.resolve("check-routing-tor-sections.png").toFile().exists());
    }

    private View bindRouting(Activity activity) {
        View root = LayoutInflater.from(activity).inflate(R.layout.dialog_domain_routing, new ViewGroup(activity) {
            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
            }
        }, false);
        TextView label = root.findViewById(R.id.domain_route_label);
        label.setText(activity.getString(R.string.domain_routing_current,
                activity.getString(R.string.domain_routing_clearnet)));
        return root;
    }

    private View bindTor(Activity activity) {
        View root = LayoutInflater.from(activity).inflate(R.layout.dialog_tor_preview, new ViewGroup(activity) {
            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
            }
        }, false);
        TextView status = root.findViewById(R.id.tor_status_text);
        status.setText(R.string.tor_status_orbot_missing);
        return root;
    }

    private View bindHistory(Activity activity) {
        View root = LayoutInflater.from(activity).inflate(R.layout.dialog_recent_links, new ViewGroup(activity) {
            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
            }
        }, false);
        TextView hint = root.findViewById(R.id.recent_links_hint);
        hint.setText(R.string.mRecentLinks_hint);
        return root;
    }

    private void save(View view, Path path) throws Exception {
        int width = WIDTH_PX;
        int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(widthSpec, heightSpec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        File file = path.toFile();
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        bitmap.recycle();
    }
}
