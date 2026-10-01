package local.zipshortcut;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends Activity {
    private static final int BACKGROUND=0xff101311, PANEL=0xff202621, TEXT=0xfff3f6ef;
    private static final int MUTED=0xffa6b0a4, ACCENT=0xffc2ed75;
    private EditText name;
    private ImageView preview;
    private TextView previewName;
    private IconSettings.Choice selected;
    private final List<LinearLayout> tiles=new ArrayList<>();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        selected=state==null ? IconSettings.current(this) : IconSettings.choice(state.getString("icon"));
        getWindow().getDecorView().setBackgroundColor(BACKGROUND);
        ScrollView scroll=new ScrollView(this);
        scroll.setBackgroundColor(BACKGROUND);
        scroll.setFitsSystemWindows(true);
        LinearLayout content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24),dp(24),dp(24),dp(32));
        content.addView(text("Your Dot",30,TEXT,true));
        content.addView(text("Make your shortcut feel like your Dot.",16,MUTED,false));

        LinearLayout summary=new LinearLayout(this);
        summary.setGravity(Gravity.CENTER_VERTICAL);
        summary.setPadding(dp(16),dp(12),dp(16),dp(12));
        summary.setBackground(surface(PANEL,0));
        preview=new ImageView(this);
        preview.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        summary.addView(preview,new LinearLayout.LayoutParams(dp(76),dp(76)));
        LinearLayout summaryText=new LinearLayout(this);
        summaryText.setOrientation(LinearLayout.VERTICAL);
        summaryText.setPadding(dp(16),0,0,0);
        previewName=text("",22,TEXT,true);
        summaryText.addView(previewName);
        summaryText.addView(text("Talk to Dot",14,MUTED,false));
        summary.addView(summaryText);
        content.addView(summary,spaced(dp(24)));

        content.addView(text("Dot name",17,TEXT,true),spaced(dp(24)));
        name=new EditText(this);
        name.setContentDescription("Dot name");
        name.setSingleLine(true);
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        name.setTextColor(TEXT);
        name.setHintTextColor(MUTED);
        name.setHint("Name in ChatGPT");
        name.setBackground(surface(PANEL,0));
        name.setBackgroundTintList(ColorStateList.valueOf(PANEL));
        name.setPadding(dp(16),dp(12),dp(16),dp(12));
        name.setText(state==null ? DotSettings.name(this) : state.getString("name",DotSettings.name(this)));
        content.addView(name,spaced(dp(8)));
        content.addView(text("Match the name in ChatGPT, including capitalization.",14,MUTED,false),spaced(dp(8)));
        content.addView(text("Home screen icon",17,TEXT,true),spaced(dp(24)));
        content.addView(text("Pick a Dot-style character for your shortcut.",14,MUTED,false),spaced(dp(4)));

        for (int row=0;row<2;row++) {
            LinearLayout line=new LinearLayout(this);
            for (int col=0;col<3;col++) {
                IconSettings.Choice option=IconSettings.CHOICES[row*3+col];
                LinearLayout tile=new LinearLayout(this);
                tile.setOrientation(LinearLayout.VERTICAL);
                tile.setGravity(Gravity.CENTER);
                tile.setPadding(dp(6),dp(12),dp(6),dp(10));
                tile.setClickable(true); tile.setFocusable(true);
                ImageView image=new ImageView(this);
                image.setImageResource(option.drawable);
                image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                tile.addView(image,new LinearLayout.LayoutParams(dp(72),dp(72)));
                TextView label=text(option.label,14,TEXT,true);
                label.setGravity(Gravity.CENTER);
                label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                tile.addView(label);
                tile.setOnClickListener(v -> { selected=option; updatePreview(); });
                LinearLayout.LayoutParams cell=new LinearLayout.LayoutParams(0,dp(124),1);
                if (col>0) cell.leftMargin=dp(8);
                line.addView(tile,cell);
                tiles.add(tile);
            }
            content.addView(line,spaced(dp(10)));
        }
        content.addView(text("Your launcher may take a moment to show the new icon.",14,MUTED,false),spaced(dp(12)));
        Button save=button("Save changes",true);
        save.setOnClickListener(v -> { if (saveChanges()) Toast.makeText(this,"Shortcut saved",Toast.LENGTH_SHORT).show(); });
        content.addView(save,spaced(dp(24)));
        Button open=button("Save and open Dot",false);
        open.setOnClickListener(v -> {
            if (saveChanges()) {
                startActivity(new Intent(this,LauncherActivity.class));
                finish();
            }
        });
        content.addView(open,spaced(dp(8)));
        Button reset=button("Reset name to zip",false);
        reset.setOnClickListener(v -> {
            name.setText(DotSettings.DEFAULT_NAME);
            DotSettings.save(this,DotSettings.DEFAULT_NAME);
            Toast.makeText(this,"Default name restored: zip",Toast.LENGTH_SHORT).show();
        });
        content.addView(reset,spaced(dp(8)));
        Button access=button("Open Accessibility settings",false);
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        content.addView(access,spaced(dp(16)));
        name.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            @Override public void onTextChanged(CharSequence s,int start,int before,int count) { updatePreview(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        updatePreview();
        scroll.addView(content);
        setContentView(scroll);
    }
    private void updatePreview() {
        preview.setImageResource(selected.drawable);
        String value=name.getText().toString().trim();
        previewName.setText(value.isEmpty() ? "Your Dot" : value);
        for (int i=0;i<tiles.size();i++) {
            IconSettings.Choice option=IconSettings.CHOICES[i];
            LinearLayout tile=tiles.get(i);
            boolean checked=selected.id.equals(option.id);
            tile.setSelected(checked);
            tile.setBackground(surface(checked ? 0xff293820 : PANEL,checked ? ACCENT : 0));
            tile.setContentDescription(option.label+" icon"+(checked ? ", selected" : ""));
            ((TextView)tile.getChildAt(1)).setText(option.label+(checked ? " ✓" : ""));
        }
    }
    private boolean saveChanges() {
        if (name.getText().toString().trim().isEmpty()) {
            name.setError("Enter a Dot name, or reset the name to zip.");
            name.requestFocus(); return false;
        }
        try {
            IconSettings.save(this,selected);
        } catch (IllegalArgumentException | SecurityException error) {
            Toast.makeText(this,"Could not change the icon. Please try again.",Toast.LENGTH_LONG).show();
            return false;
        }
        DotSettings.save(this,name.getText().toString());
        return true;
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("icon",selected.id);
        state.putString("name",name.getText().toString());
        super.onSaveInstanceState(state);
    }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    private TextView text(String value,int size,int color,boolean bold) {
        TextView view=new TextView(this);
        view.setText(value); view.setTextSize(size); view.setTextColor(color);
        if (bold) view.setTypeface(null,Typeface.BOLD);
        return view;
    }
    private GradientDrawable surface(int color,int outline) {
        GradientDrawable drawable=new GradientDrawable();
        drawable.setColor(color); drawable.setCornerRadius(dp(20));
        if (outline!=0) drawable.setStroke(dp(2),outline);
        return drawable;
    }
    private LinearLayout.LayoutParams spaced(int top) {
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);
        params.topMargin=top; return params;
    }
    private Button button(String label,boolean primary) {
        Button button=new Button(this);
        button.setText(label); button.setAllCaps(false); button.setTextSize(16);
        button.setTextColor(primary ? BACKGROUND : TEXT);
        button.setBackground(surface(primary ? ACCENT : PANEL,0));
        button.setMinHeight(dp(52));
        return button;
    }
}
