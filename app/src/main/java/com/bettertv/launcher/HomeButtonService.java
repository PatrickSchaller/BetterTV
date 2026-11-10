package com.bettertv.launcher;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.os.UserManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import java.util.HashSet;
import java.util.Set;

/** Enabled only by the user in Android accessibility settings. */
public final class HomeButtonService extends AccessibilityService {
    private final Set<ComponentName> stockHomes = new HashSet<>();

    @Override protected void onServiceConnected() {
        // shortcut: startup depends on Home window events; test target firmware before adding boot handling.
        Set<String> packages = new HashSet<>();
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        stockHomes.clear();
        for (ResolveInfo entry : getPackageManager().queryIntentActivities(home, 0)) {
            if (entry.priority < 0 || !entry.activityInfo.exported
                    || getPackageName().equals(entry.activityInfo.packageName)
                    || (entry.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) continue;
            stockHomes.add(new ComponentName(entry.activityInfo.packageName, entry.activityInfo.name));
            packages.add(entry.activityInfo.packageName);
        }
        if (packages.isEmpty()) packages.add(getPackageName());
        AccessibilityServiceInfo info = getServiceInfo();
        info.packageNames = packages.toArray(new String[0]);
        setServiceInfo(info);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || event.getPackageName() == null || event.getClassName() == null) return;
        ComponentName window = new ComponentName(event.getPackageName().toString(), event.getClassName().toString());
        UserManager users = getSystemService(UserManager.class);
        if (stockHomes.contains(window) && users != null && users.isUserUnlocked()) {
            try {
                startActivity(new Intent(this, MainActivity.class)
                        .setAction(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            } catch (ActivityNotFoundException | SecurityException unavailable) {
                Log.w("BetterTV", "Home-button launch unavailable", unavailable);
            }
        }
    }
    @Override public void onInterrupt() { }
}
