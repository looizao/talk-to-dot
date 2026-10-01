package local.zipshortcut;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        if (isEnabled()) { openDot(); return; }
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(56, 80, 56, 40);
        TextView title = new TextView(this); title.setText("Talk to Zip"); title.setTextSize(28);
        layout.addView(title);
        TextView info = new TextView(this);
        info.setText("This shortcut opens ChatGPT, opens its menu, and selects your saved Dot (zip by default).\n\nAndroid Accessibility access is required to tap those controls. Android grants broad screen access; this app is configured for ChatGPT and acts only for 15 seconds after you launch it.\n\nIt does not send messages, place calls, store chat content, or connect to the internet.\n\nEnable Talk to Zip in Accessibility, then tap its app icon again. If Android blocks the switch, open Talk to Zip’s App info menu and allow restricted settings for this locally built app.");
        info.setTextSize(17); info.setPadding(0,32,0,32); layout.addView(info);
        Button choose = new Button(this); choose.setText("Choose Dot");
        choose.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        layout.addView(choose);
        Button settings = new Button(this); settings.setText("Open Accessibility settings");
        settings.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        layout.addView(settings);
        Button plain = new Button(this); plain.setText("Open ChatGPT without automation");
        plain.setOnClickListener(v -> { Intent i = getPackageManager().getLaunchIntentForPackage("com.openai.chatgpt"); if (i != null) startActivity(i); });
        layout.addView(plain); setContentView(layout);
    }
    private boolean isEnabled() {
        String enabled=Settings.Secure.getString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        String service=new ComponentName(this,ZipService.class).flattenToString();
        return enabled!=null && enabled.contains(service);
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent);
        if (isEnabled()) openDot();
    }
    private void openDot() {
        Intent i = getPackageManager().getLaunchIntentForPackage("com.openai.chatgpt");
        if (i == null) return;
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        getSharedPreferences("shortcut",0).edit().putLong("armedUntil",System.currentTimeMillis()+15000).apply();
        if (ZipService.instance != null) ZipService.instance.begin();
        startActivity(i); finish();
    }
}
