package com.bettertv.launcher;

import java.util.Arrays;
import java.util.List;

/** Local service order only; no viewing history or catalog is collected. */
public final class AppOrder {
    private static final List<String> SERVICES = Arrays.asList(
            "com.netflix.ninja", "com.amazon.amazonvideo.livingroom",
            "com.apple.atve.androidtv.appletv", "com.google.android.youtube.tv",
            "com.hulu.livingroomplus", "com.disney.disneyplus", "com.espn.score_center",
            "com.espn.espnapp", "com.cbs.ott", "com.cbs.app");

    private AppOrder() { }

    public static int rank(String packageName) {
        int index = SERVICES.indexOf(packageName);
        return index < 0 ? SERVICES.size() : index;
    }

    public static boolean isSuggestedFavorite(String packageName) {
        return SERVICES.contains(packageName);
    }

    public static String description(String packageName) {
        if ("com.google.android.youtube.tv".equals(packageName))
            return "Videos, creators, and live streams.";
        if (packageName.startsWith("com.espn"))
            return "Sports, highlights, and live events.";
        if ("com.apple.atve.androidtv.appletv".equals(packageName))
            return "Browse movies, series, and Apple Originals.";
        return "Open the app to browse and watch.";
    }
}
