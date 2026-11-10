package com.bettertv.launcher;

public final class BroadcastRulesCheck {
    public static void main(String[] args) {
        assert BroadcastRules.isAntenna("TYPE_ATSC_T", "SERVICE_TYPE_AUDIO_VIDEO", true, false);
        assert BroadcastRules.isAntenna("TYPE_ATSC3_T", "SERVICE_TYPE_AUDIO_VIDEO", true, false);
        assert !BroadcastRules.isAntenna("TYPE_ATSC_C", "SERVICE_TYPE_AUDIO_VIDEO", true, false);
        assert !BroadcastRules.isAntenna("TYPE_PREVIEW", "SERVICE_TYPE_AUDIO_VIDEO", true, false);
        assert !BroadcastRules.isAntenna("TYPE_OTHER", "SERVICE_TYPE_AUDIO_VIDEO", true, false);
        assert !BroadcastRules.isAntenna("TYPE_ATSC_T", "SERVICE_TYPE_AUDIO", true, false);
        assert !BroadcastRules.isAntenna("TYPE_ATSC_T", "SERVICE_TYPE_AUDIO_VIDEO", false, false);
        assert !BroadcastRules.isAntenna("TYPE_ATSC_T", "SERVICE_TYPE_AUDIO_VIDEO", true, true);
        assert BroadcastRules.matches("  BEARS  ", "Chicago Bears at Minnesota Vikings", null);
        assert BroadcastRules.matches("a new  hope", "Star Wars", "A New Hope");
        assert !BroadcastRules.matches("", "Anything", "Anything");
        assert !BroadcastRules.matches("prime", null, null);
        assert !BroadcastRules.matches("Dune", "Foundation", "The Emperor's Peace");
        System.out.println("Broadcast rules checks passed: terrestrial TV only, visibility/lock limits, title matching.");
    }
}
