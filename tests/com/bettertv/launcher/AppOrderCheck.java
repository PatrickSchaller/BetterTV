package com.bettertv.launcher;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/** Run with java -ea; covers the service presets independently of an Android device. */
public final class AppOrderCheck {
    public static void main(String[] args) {
        List<String> services = Arrays.asList(
                "com.netflix.ninja", "com.amazon.amazonvideo.livingroom",
                "com.apple.atve.androidtv.appletv", "com.google.android.youtube.tv",
                "com.hulu.livingroomplus", "com.disney.disneyplus",
                "com.espn.score_center", "com.espn.espnapp", "com.cbs.ott", "com.cbs.app");
        HashSet<Integer> ranks = new HashSet<>();
        for (String service : services) {
            assert AppOrder.isSuggestedFavorite(service) : "Missing user's service: " + service;
            assert ranks.add(AppOrder.rank(service)) : "Ambiguous service order: " + service;
            assert !AppOrder.description(service).trim().isEmpty() : service;
        }
        for (String other : Arrays.asList("org.jellyfin.androidtv", "tv.plex.app", "com.netflix", "")) {
            assert !AppOrder.isSuggestedFavorite(other) : "Unknown app was mistaken for a preset";
            assert ranks.stream().allMatch(rank -> rank < AppOrder.rank(other))
                    : "Unknown apps must remain available after the service presets";
        }
        assert AppOrder.rank("com.netflix.ninja") < AppOrder.rank("com.amazon.amazonvideo.livingroom");
        System.out.println("AppOrder checks passed: service presets, unique ordering, unknown-app fallback.");
    }
}
