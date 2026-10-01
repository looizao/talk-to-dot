package local.zipshortcut;

import android.content.Context;

final class DotSettings {
    static final String DEFAULT_NAME = "zip";

    static String name(Context context) {
        String name = context.getSharedPreferences("shortcut", 0)
                .getString("dotName", DEFAULT_NAME);
        return name == null || name.trim().isEmpty() ? DEFAULT_NAME : name.trim();
    }

    static void save(Context context, String name) {
        context.getSharedPreferences("shortcut", 0).edit()
                .putString("dotName", name.trim()).apply();
    }
}
