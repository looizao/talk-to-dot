package local.zipshortcut;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.ArrayList;
import java.util.List;

final class IconSettings {
    static final class Choice {
        final String id, label, component;
        final int drawable;
        Choice(String id, String label, String component, int drawable) {
            this.id=id; this.label=label; this.component=component; this.drawable=drawable;
        }
    }
    static final Choice[] CHOICES = {
        new Choice("frog", "Frog", "MainActivity", R.drawable.icon_frog),
        new Choice("cat", "Cat", "IconCat", R.drawable.icon_cat),
        new Choice("fox", "Fox", "IconFox", R.drawable.icon_fox),
        new Choice("bear", "Bear", "IconBear", R.drawable.icon_bear),
        new Choice("owl", "Owl", "IconOwl", R.drawable.icon_owl),
        new Choice("classic", "Classic D", "IconClassic", R.drawable.icon)
    };
    static Choice choice(String id) {
        for (Choice option : CHOICES) if (option.id.equals(id)) return option;
        return CHOICES[0];
    }
    static Choice current(Context context) {
        return choice(context.getSharedPreferences("shortcut",0).getString("iconName","frog"));
    }
    private static ComponentName component(Context context, Choice option) {
        return new ComponentName(context.getPackageName(), context.getPackageName()+"."+option.component);
    }
    static void save(Context context, Choice selected) {
        PackageManager manager=context.getPackageManager();
        if (Build.VERSION.SDK_INT>=33) {
            List<PackageManager.ComponentEnabledSetting> updates=new ArrayList<>();
            for (Choice option : CHOICES) {
                updates.add(new PackageManager.ComponentEnabledSetting(component(context,option),
                        option.id.equals(selected.id) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP));
            }
            manager.setComponentEnabledSettings(updates);
        } else {
            // Keep a working launcher entry available throughout the switch on older Android.
            manager.setComponentEnabledSetting(component(context,selected),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,PackageManager.DONT_KILL_APP);
            for (Choice option : CHOICES) if (!option.id.equals(selected.id)) {
                manager.setComponentEnabledSetting(component(context,option),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP);
            }
        }
        context.getSharedPreferences("shortcut",0).edit().putString("iconName",selected.id).apply();
    }
}
