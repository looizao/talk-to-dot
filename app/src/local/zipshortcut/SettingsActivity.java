package local.zipshortcut;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private EditText name;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(padding, padding * 2, padding, padding);
        TextView title = new TextView(this);
        title.setText("Choose your Dot");
        title.setTextSize(28);
        layout.addView(title);
        TextView help = new TextView(this);
        help.setText("Enter the name exactly as it appears in ChatGPT’s sidebar, including capitalization. The default is zip.\n\nThis changes which Dot the shortcut opens. It does not rename your Dot in ChatGPT.");
        help.setTextSize(17);
        help.setPadding(0, padding, 0, padding);
        layout.addView(help);
        TextView label = new TextView(this);
        label.setText("Dot name");
        layout.addView(label);
        name = new EditText(this);
        name.setContentDescription("Dot name");
        name.setSingleLine(true);
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        name.setText(DotSettings.name(this));
        layout.addView(name);
        Button save = new Button(this);
        save.setText("Save name");
        save.setOnClickListener(v -> { if (saveName()) Toast.makeText(this, "Saved: " + DotSettings.name(this), Toast.LENGTH_SHORT).show(); });
        layout.addView(save);
        Button open = new Button(this);
        open.setText("Save and open Dot");
        open.setOnClickListener(v -> {
            if (saveName()) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        });
        layout.addView(open);
        Button reset = new Button(this);
        reset.setText("Reset to zip");
        reset.setOnClickListener(v -> {
            name.setText(DotSettings.DEFAULT_NAME);
            DotSettings.save(this, DotSettings.DEFAULT_NAME);
            Toast.makeText(this, "Default restored: zip", Toast.LENGTH_SHORT).show();
        });
        layout.addView(reset);
        Button access = new Button(this);
        access.setText("Open Accessibility settings");
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        layout.addView(access);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        setContentView(scroll);
    }

    private boolean saveName() {
        if (name.getText().toString().trim().isEmpty()) {
            name.setError("Enter a Dot name, or use Reset to zip.");
            return false;
        }
        DotSettings.save(this, name.getText().toString());
        return true;
    }
}
