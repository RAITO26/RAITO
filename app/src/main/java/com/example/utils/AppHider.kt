package com.example.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

object AppHider {
    fun setAppIconVisible(context: Context, visible: Boolean) {
        val pm = context.packageManager
        val state = if (visible) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        val targets = mutableListOf<ComponentName>()
        
        // Add hardcoded candidates (excluding InfoAlias which must stay enabled)
        targets.add(ComponentName(context, "com.example.LauncherAlias"))
        targets.add(ComponentName(context, "com.example.MainActivity\$LauncherAlias"))
        targets.add(ComponentName(context, context.packageName + ".LauncherAlias"))

        // Dynamically query all defined activities/aliases in manifest containing "Launcher" or "Alias" (excluding InfoAlias)
        try {
            val flags = PackageManager.GET_ACTIVITIES or PackageManager.GET_DISABLED_COMPONENTS
            val packageInfo = pm.getPackageInfo(context.packageName, flags)
            packageInfo.activities?.forEach { activityInfo ->
                if ((activityInfo.name.contains("LauncherAlias") || activityInfo.name.contains("Alias")) 
                    && !activityInfo.name.contains("InfoAlias")) {
                    targets.add(ComponentName(context.packageName, activityInfo.name))
                }
            }
        } catch (e: Exception) {
            Log.e("AppHider", "Error querying own package manifest components: ${e.message}")
        }

        val uniqueTargets = targets.distinct()
        Log.d("AppHider", "Found targets to toggle: $uniqueTargets, targeted state: $state")

        for (target in uniqueTargets) {
            // First try flag 0 (kills application or triggers instant launcher refresh broadcast)
            try {
                pm.setComponentEnabledSetting(
                    target,
                    state,
                    0
                )
                Log.d("AppHider", "Force-set (flag 0) component $target state to $state successfully")
            } catch (e: Exception) {
                Log.w("AppHider", "Force-set (flag 0) failed for $target: ${e.message}. Retrying with DONT_KILL_APP...")
                try {
                    pm.setComponentEnabledSetting(
                        target,
                        state,
                        PackageManager.DONT_KILL_APP
                    )
                    Log.d("AppHider", "Soft-set (DONT_KILL_APP) component $target state to $state successfully")
                } catch (e2: Exception) {
                    Log.e("AppHider", "Failed completely to toggle component $target: ${e2.message}")
                }
            }
        }

        // Trick to force Samsung One UI Launcher (Android 14+) to clear cache and update icon list immediately:
        // By toggling a totally unrelated dummy broadcast receiver component back and forth, 
        // we trigger multiple packages-changed system broadcasts which shakes and invalidates the launcher's icon cache.
        try {
            val dummyComponent = ComponentName(context, context.packageName + ".services.DummyReceiver")
            val currentSetting = pm.getComponentEnabledSetting(dummyComponent)
            val tempState = if (currentSetting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            }
            // First component state toggle with flag 0 to force system broadcast cascade
            pm.setComponentEnabledSetting(dummyComponent, tempState, 0)
            // Revert back immediately to restore its state safely with DONT_KILL_APP
            pm.setComponentEnabledSetting(dummyComponent, currentSetting, PackageManager.DONT_KILL_APP)
            Log.d("AppHider", "Successfully completed dummy receiver toggling to force launcher refresh")
        } catch (e: Exception) {
            Log.e("AppHider", "Failed to toggle dummy receiver for refresh: ${e.message}")
        }
    }

    fun isAppIconVisible(context: Context): Boolean {
        val pm = context.packageManager
        val targets = mutableListOf<ComponentName>()
        targets.add(ComponentName(context, "com.example.LauncherAlias"))
        targets.add(ComponentName(context, context.packageName + ".LauncherAlias"))

        try {
            val flags = PackageManager.GET_ACTIVITIES or PackageManager.GET_DISABLED_COMPONENTS
            val packageInfo = pm.getPackageInfo(context.packageName, flags)
            packageInfo.activities?.forEach { activityInfo ->
                if ((activityInfo.name.contains("LauncherAlias") || activityInfo.name.contains("Alias"))
                    && !activityInfo.name.contains("InfoAlias")) {
                    targets.add(ComponentName(context.packageName, activityInfo.name))
                }
            }
        } catch (e: Exception) {
            // ignore
        }

        val uniqueTargets = targets.distinct()
        for (target in uniqueTargets) {
            try {
                val setting = pm.getComponentEnabledSetting(target)
                if (setting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    return false
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        return true
    }
}
