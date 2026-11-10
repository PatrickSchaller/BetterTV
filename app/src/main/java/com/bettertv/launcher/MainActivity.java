package com.bettertv.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.LruCache;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int NAVY = Color.rgb(7, 18, 38);
    private static final int WHITE = Color.rgb(247, 245, 239);
    private static final int MUTED = Color.rgb(196, 205, 218);
    private static final int GOLD = Color.rgb(244, 214, 163);
    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private final LruCache<String, Bitmap> artwork = new LruCache<String, Bitmap>(8 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount(); }
    };
    private List<TvApp> apps = new ArrayList<>();
    private Set<String> favorites;
    private SharedPreferences preferences;
    private LinearLayout rows;
    private ScrollView scroll;
    private ImageView backdrop;
    private TextView selectedLabel;
    private Button allAppsButton;
    private TvApp selected;
    private View firstApp;
    private String lastFocus = "", fingerprint = "";
    private boolean destroyed;
    private boolean firstDiscovery = true;
    private int wallpaperRequest;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        preferences = getSharedPreferences("home", MODE_PRIVATE);
        favorites = new HashSet<>(preferences.getStringSet("favorites", new HashSet<>()));
        buildHome();
    }

    private void buildHome() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(NAVY);
        backdrop = new ImageView(this);
        backdrop.setScaleType(ImageView.ScaleType.CENTER_CROP);
        backdrop.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        root.addView(backdrop, new FrameLayout.LayoutParams(-1, -1));
        View shade = new View(this);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x77050B17, 0x33050B17, 0xDD050B17}));
        root.addView(shade, new FrameLayout.LayoutParams(-1, -1));

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        LinearLayout content = vertical();
        content.setPadding(dp(36), dp(22), dp(36), dp(24));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("Better TV", 17, WHITE);
        brand.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        header.addView(brand, new LinearLayout.LayoutParams(0, dp(36), 1));
        header.addView(button("Search", () -> startActivity(new Intent(this, SearchActivity.class))));
        header.addView(button("Live TV", this::openLiveTv));
        header.addView(button("Inputs", this::openInputs));
        header.addView(button("Settings", this::openSettings));
        content.addView(header, new LinearLayout.LayoutParams(-1, dp(38)));

        LinearLayout gallery = vertical();
        gallery.setGravity(Gravity.CENTER);
        gallery.setMinimumHeight(dp(220));
        gallery.setPadding(0, dp(18), 0, dp(22));
        TextClock clock = galleryClock("h:mm", "HH:mm", 76);
        clock.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        gallery.addView(clock, new LinearLayout.LayoutParams(-1, -2));
        TextClock date = galleryClock("EEEE, MMMM d", "EEEE, MMMM d", 15);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(-1, -2);
        dateParams.topMargin = dp(2);
        gallery.addView(date, dateParams);
        selectedLabel = text("Your apps, a little room to breathe.", 13, WHITE);
        selectedLabel.setGravity(Gravity.CENTER);
        selectedLabel.setSingleLine(true);
        selectedLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
        selectedLabel.setShadowLayer(dp(3), 0, dp(1), Color.BLACK);
        LinearLayout.LayoutParams selectedParams = new LinearLayout.LayoutParams(-1, -2);
        selectedParams.topMargin = dp(17);
        gallery.addView(selectedLabel, selectedParams);
        content.addView(gallery, new LinearLayout.LayoutParams(-1, 0, 1));

        rows = vertical();
        content.addView(rows, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
    }

    @Override protected void onResume() {
        super.onResume();
        refreshWallpaper();
        loader.execute(() -> {
            List<TvApp> discovered = discoverApps();
            StringBuilder snapshot = new StringBuilder();
            for (TvApp app : discovered) snapshot.append(app.component.flattenToString()).append(app.label);
            runOnUiThread(() -> {
                if (destroyed || snapshot.toString().equals(fingerprint) && !apps.isEmpty()) return;
                fingerprint = snapshot.toString();
                apps = discovered;
                if (!preferences.contains("favorites")) {
                    for (TvApp app : apps) {
                        if (AppOrder.isSuggestedFavorite(app.packageName)) favorites.add(app.packageName);
                    }
                    preferences.edit().putStringSet("favorites", new HashSet<>(favorites)).apply();
                }
                renderRows();
            });
        });
    }

    private TextClock galleryClock(String twelveHour, String twentyFourHour, int size) {
        TextClock clock = new TextClock(this);
        clock.setFormat12Hour(twelveHour);
        clock.setFormat24Hour(twentyFourHour);
        clock.setTextSize(size);
        clock.setTextColor(WHITE);
        clock.setGravity(Gravity.CENTER);
        clock.setIncludeFontPadding(false);
        clock.setShadowLayer(dp(5), 0, dp(2), 0xCC000000);
        return clock;
    }

    private void refreshWallpaper() {
        int request = ++wallpaperRequest;
        String previous = preferences.getString("last_wallpaper", "");
        loader.execute(() -> {
            List<String> choices = new ArrayList<>();
            choices.add("cinema.png");
            try {
                String[] files = getAssets().list("gallery");
                if (files != null) for (String file : files) {
                    if (file.endsWith(".png")) choices.add("gallery/" + file);
                }
                if (choices.size() > 1) choices.remove(previous);
                String chosen = choices.get(ThreadLocalRandom.current().nextInt(choices.size()));
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                try (InputStream stream = getAssets().open(chosen)) {
                    BitmapFactory.decodeStream(stream, null, options);
                }
                options.inSampleSize = 1;
                while (options.outWidth / options.inSampleSize > 1920
                        || options.outHeight / options.inSampleSize > 1080) options.inSampleSize *= 2;
                options.inJustDecodeBounds = false;
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                Bitmap bitmap;
                try (InputStream stream = getAssets().open(chosen)) {
                    bitmap = BitmapFactory.decodeStream(stream, null, options);
                }
                if (bitmap != null) runOnUiThread(() -> {
                    if (!destroyed && request == wallpaperRequest) {
                        backdrop.setImageBitmap(bitmap);
                        preferences.edit().putString("last_wallpaper", chosen).apply();
                    }
                });
            } catch (IOException ignored) {
                // Bundled artwork is optional; keep the last image or the plain navy background.
            }
        });
    }

    private List<TvApp> discoverApps() {
        PackageManager manager = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<TvApp> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<ResolveInfo> candidates;
        try { candidates = manager.queryIntentActivities(intent, 0); }
        catch (RuntimeException unavailable) {
            runOnUiThread(() -> {
                if (!destroyed) toast("Couldn't read TV apps. Open TV settings, then return here.");
            });
            return result;
        }
        for (ResolveInfo info : candidates) {
            if (info.activityInfo == null || !info.activityInfo.exported) continue;
            String packageName = info.activityInfo.packageName;
            if (getPackageName().equals(packageName) || !seen.add(packageName)) continue;
            try {
                CharSequence label = info.loadLabel(manager);
                result.add(new TvApp(info, label == null ? packageName : label.toString()));
            } catch (RuntimeException packageChanged) {
                // App removal can race discovery; the next resume refreshes this list.
            }
        }
        Collator collator = Collator.getInstance();
        result.sort((a, b) -> collator.compare(a.label, b.label));
        return result;
    }

    private void renderRows() {
        boolean restoreFocus = firstDiscovery || isInsideDock(getCurrentFocus());
        rows.removeAllViews();
        firstApp = null;
        List<TvApp> pinned = new ArrayList<>();
        for (TvApp app : apps) if (favorites.contains(app.packageName)) pinned.add(app);
        pinned.sort(Comparator.comparingInt(app -> AppOrder.rank(app.packageName)));
        addFavoriteDock(pinned);
        if (selected == null || pinned.stream().noneMatch(a -> a.packageName.equals(selected.packageName))) {
            select(!pinned.isEmpty() ? pinned.get(0) : null);
        }
        if (restoreFocus) {
            View restore = rows.findViewWithTag(lastFocus);
            if (restore != null) restore.requestFocus();
            else if (firstApp != null) firstApp.requestFocus();
            else allAppsButton.requestFocus();
        }
        firstDiscovery = false;
    }

    private boolean isInsideDock(View focused) {
        while (focused != null) {
            if (focused == rows) return true;
            focused = focused.getParent() instanceof View ? (View) focused.getParent() : null;
        }
        return false;
    }

    private void addFavoriteDock(List<TvApp> items) {
        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(text("Favorites", 13, WHITE), new LinearLayout.LayoutParams(0, dp(34), 1));
        heading.addView(text("Hold OK to pin or unpin", 11, MUTED));
        allAppsButton = button("All apps", this::openAllApps);
        allAppsButton.setTag("all_apps");
        allAppsButton.setOnFocusChangeListener((view, focused) -> {
            if (focused) lastFocus = "all_apps";
        });
        heading.addView(allAppsButton);
        rows.addView(heading);
        if (items.isEmpty()) {
            TextView empty = text("Open All apps, then hold OK to add a favorite.", 13, WHITE);
            empty.setPadding(0, dp(12), 0, dp(8));
            rows.addView(empty);
            return;
        }
        HorizontalScrollView rail = new HorizontalScrollView(this);
        rail.setHorizontalScrollBarEnabled(false);
        rail.setClipChildren(false);
        rail.setClipToPadding(false);
        rail.setPadding(dp(3), dp(6), dp(3), dp(12));
        LinearLayout cards = new LinearLayout(this);
        cards.setClipChildren(false);
        for (TvApp app : items) {
            View card = appCard(app);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(112), -2);
            params.rightMargin = dp(9);
            cards.addView(card, params);
            if (firstApp == null) firstApp = card;
        }
        rail.addView(cards, new HorizontalScrollView.LayoutParams(-2, -2));
        rows.addView(rail, new LinearLayout.LayoutParams(-1, -2));
    }

    private View appCard(TvApp app) {
        LinearLayout card = vertical();
        card.setMinimumHeight(dp(86));
        card.setTag("favorite:" + app.packageName);
        card.setPadding(dp(5), dp(5), dp(5), dp(4));
        card.setFocusable(true);
        card.setClickable(true);
        card.setContentDescription(app.label + (favorites.contains(app.packageName) ? ", favorite" : "")
                + ". Hold OK to " + (favorites.contains(app.packageName) ? "unpin." : "pin."));
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        card.addView(image, new LinearLayout.LayoutParams(-1, dp(54)));
        TextView label = text(app.label, 12, WHITE);
        label.setSingleLine(true);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setGravity(Gravity.CENTER);
        label.setMinHeight(dp(24));
        label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        card.addView(label, new LinearLayout.LayoutParams(-1, -2));
        card.setBackground(panel(0xBB152137, 0));
        card.setOnFocusChangeListener((view, focused) -> {
            card.setBackground(panel(focused ? GOLD : 0xBB152137, focused ? WHITE : 0));
            label.setTextColor(focused ? NAVY : WHITE);
            if (focused) {
                lastFocus = card.getTag().toString();
                select(app);
            }
        });
        card.setOnClickListener(view -> launch(app));
        card.setOnLongClickListener(view -> {
            toggleFavorite(app);
            return true;
        });
        Bitmap cached = artwork.get(app.packageName);
        if (cached != null) image.setImageBitmap(cached);
        else loader.execute(() -> {
            Bitmap bitmap = artwork.get(app.packageName);
            if (bitmap == null) {
                bitmap = loadArtwork(app);
                artwork.put(app.packageName, bitmap);
            }
            Bitmap ready = bitmap;
            runOnUiThread(() -> { if (!destroyed) image.setImageBitmap(ready); });
        });
        return card;
    }

    private Bitmap loadArtwork(TvApp app) {
        Bitmap bitmap = Bitmap.createBitmap(320, 180, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.rgb(23, 38, 61));
        try {
            Drawable drawable = app.info.activityInfo.loadBanner(getPackageManager());
            boolean banner = drawable != null;
            if (!banner) drawable = app.info.loadIcon(getPackageManager());
            int width = banner ? 320 : 110, height = banner ? 180 : 110;
            drawable.setBounds((320 - width) / 2, (180 - height) / 2,
                    (320 + width) / 2, (180 + height) / 2);
            drawable.draw(canvas);
        } catch (RuntimeException ignored) {
            // An app may disappear between discovery and artwork loading; its text label remains.
        }
        return bitmap;
    }

    private void select(TvApp app) {
        selected = app;
        selectedLabel.setText(app == null ? "Open All apps to choose a favorite." : app.label);
    }

    private void toggleFavorite(TvApp app) {
        boolean removed = favorites.remove(app.packageName);
        if (!removed) favorites.add(app.packageName);
        preferences.edit().putStringSet("favorites", new HashSet<>(favorites)).apply();
        renderRows();
        toast(app.label + (removed ? " removed from favorites" : " added to favorites"));
    }

    private void openAllApps() {
        if (apps.isEmpty()) {
            toast("No TV apps found. Install a TV app, then return Home.");
            return;
        }
        List<TvApp> drawerApps = new ArrayList<>(apps);
        CharSequence[] labels = new CharSequence[drawerApps.size()];
        for (int i = 0; i < drawerApps.size(); i++) labels[i] = drawerLabel(drawerApps.get(i));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("All apps")
                .setItems(labels, (prompt, index) -> launch(drawerApps.get(index)))
                .setNegativeButton("Close", null).create();
        dialog.setOnShowListener(prompt -> dialog.getListView().setOnItemLongClickListener(
                (parent, view, index, id) -> {
                    TvApp app = drawerApps.get(index);
                    toggleFavorite(app);
                    labels[index] = drawerLabel(app);
                    ((BaseAdapter) dialog.getListView().getAdapter()).notifyDataSetChanged();
                    return true;
                }));
        dialog.show();
    }

    private CharSequence drawerLabel(TvApp app) {
        return app.label + (favorites.contains(app.packageName) ? " (favorite)" : "");
    }

    private void launch(TvApp app) {
        startSafely(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                .setComponent(app.component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
                "This app is unavailable. Check it in TV settings.");
    }

    private void openLiveTv() {
        startSafely(new Intent(Intent.ACTION_VIEW, TvContract.Channels.CONTENT_URI),
                "Use your remote's TV or Input button to open antenna TV.");
    }

    private void openInputs() {
        List<TvInputInfo> inputs = new ArrayList<>();
        try {
            TvInputManager manager = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
            if (manager != null) for (TvInputInfo input : manager.getTvInputList()) {
                if (input.isPassthroughInput()) inputs.add(input);
            }
        } catch (SecurityException ignored) { }
        if (inputs.isEmpty()) {
            toast("Use the Input button on your TV remote to switch sources.");
            return;
        }
        CharSequence[] labels = new CharSequence[inputs.size()];
        for (int i = 0; i < inputs.size(); i++) labels[i] = inputs.get(i).loadLabel(this);
        new AlertDialog.Builder(this).setTitle("Choose an input").setItems(labels, (dialog, index) ->
                startSafely(new Intent(Intent.ACTION_VIEW,
                        TvContract.buildChannelUriForPassthroughInput(inputs.get(index).getId())),
                        "Use the Input button on your TV remote for this source."))
                .setNegativeButton("Cancel", null).show();
    }

    private void openSettings() {
        new AlertDialog.Builder(this).setTitle("Settings")
                .setItems(new String[]{"TV settings", "Make Better TV the home screen", "Home button fix",
                        "Search settings", "New background"}, (dialog, index) -> {
                    if (index == 0) startSafely(new Intent(Settings.ACTION_SETTINGS),
                            "Open settings using your TV remote.");
                    else if (index == 1) requestHomeRole();
                    else if (index == 2) new AlertDialog.Builder(this).setTitle("Home button fix")
                            .setMessage("Enable Better TV Home button fix in Android Accessibility settings. It opens Better TV when a stock home screen appears, so Google TV may briefly show first. It receives only stock-home window events and does not read screen contents or filter keys. Turn the service off there to access the original home screen normally. Startup after a restart still needs testing on this TV.")
                            .setPositiveButton("Open Accessibility settings", (prompt, which) ->
                                    startSafely(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                                            "Open Accessibility in your TV settings, then select Better TV Home button fix."))
                            .setNegativeButton("Cancel", null).show();
                    else if (index == 3) startActivity(new Intent(this, SearchActivity.class)
                            .putExtra("open_settings", true));
                    else refreshWallpaper();
                }).setNegativeButton("Close", null).show();
    }

    private void requestHomeRole() {
        if (Build.VERSION.SDK_INT >= 29) {
            RoleManager roles = getSystemService(RoleManager.class);
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (roles.isRoleHeld(RoleManager.ROLE_HOME)) toast("Android has selected Better TV. If Home still opens Google TV, use Settings > Home button fix.");
                else {
                    try { startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_HOME), 1); }
                    catch (ActivityNotFoundException | SecurityException unavailable) {
                        toast("This TV manages its home screen. Open Better TV from Apps instead.");
                    }
                }
                return;
            }
        }
        startSafely(new Intent(Settings.ACTION_HOME_SETTINGS),
                "This TV manages its home screen. You can still open Better TV from Apps.");
    }

    private void startSafely(Intent intent, String fallback) {
        try { startActivity(intent); }
        catch (ActivityNotFoundException | SecurityException unavailable) { toast(fallback); }
    }

    @Override public void onBackPressed() {
        scroll.smoothScrollTo(0, 0);
        if (firstApp != null) firstApp.requestFocus();
        else if (allAppsButton != null) allAppsButton.requestFocus();
    }

    @Override protected void onDestroy() {
        destroyed = true;
        loader.shutdownNow();
        artwork.evictAll();
        super.onDestroy();
    }

    private Button button(String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setTextColor(new android.content.res.ColorStateList(
                new int[][]{new int[]{android.R.attr.state_focused}, new int[]{}}, new int[]{NAVY, WHITE}));
        button.setPadding(dp(14), 0, dp(14), 0);
        button.setMinimumHeight(0);
        button.setMinHeight(0);
        button.setMinimumWidth(0);
        button.setMinWidth(0);
        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_focused}, panel(GOLD, WHITE));
        background.addState(new int[]{}, panel(0xA61A2940, 0));
        button.setBackground(background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(34));
        params.leftMargin = dp(8);
        button.setLayoutParams(params);
        button.setOnClickListener(view -> action.run());
        return button;
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private GradientDrawable panel(int fill, int border) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(7));
        if (border != 0) drawable.setStroke(dp(2), border);
        return drawable;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }

    private static final class TvApp {
        final ResolveInfo info;
        final String packageName, label;
        final ComponentName component;
        TvApp(ResolveInfo info, String label) {
            this.info = info;
            this.label = label;
            packageName = info.activityInfo.packageName;
            component = new ComponentName(packageName, info.activityInfo.name);
        }
    }
}
