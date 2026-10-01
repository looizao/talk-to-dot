package local.zipshortcut;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

public class ZipService extends AccessibilityService {
    static ZipService instance;
    final Handler handler = new Handler(Looper.getMainLooper());
    boolean running;
    long lastClick;
    final Runnable tick = () -> step();
    @Override protected void onServiceConnected() { instance = this; if (armed()) begin(); }
    @Override public void onDestroy() { instance = null; handler.removeCallbacks(tick); super.onDestroy(); }
    @Override public void onInterrupt() { stop(); }
    @Override public void onAccessibilityEvent(AccessibilityEvent e) {
        if (!running && armed()) begin();
    }
    boolean armed() { return getSharedPreferences("shortcut",0).getLong("armedUntil",0)>System.currentTimeMillis(); }
    void begin() { if (!running) { running=true; lastClick=0; handler.postDelayed(tick,500); } }
    void stop() { running=false; handler.removeCallbacks(tick); getSharedPreferences("shortcut",0).edit().remove("armedUntil").apply(); }
    void step() {
        if (!armed()) { if (running) Toast.makeText(this,"Could not select zip. Open ChatGPT’s menu and check its name.",Toast.LENGTH_LONG).show(); stop(); return; }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null && "com.openai.chatgpt".contentEquals(root.getPackageName())) {
            if (find(root,"Message zip",false) != null && find(root,"Scheduled",true)==null) { stop(); return; }
            if (System.currentTimeMillis()-lastClick>1200) {
                if (find(root,"Scheduled",true)!=null) {
                    AccessibilityNodeInfo zip=find(root,"zip",true);
                    if (zip!=null) click(zip);
                } else {
                    AccessibilityNodeInfo menu=find(root,"Menu",true);
                    if (menu!=null) click(menu);
                    else {
                        AccessibilityNodeInfo up=find(root,"Navegar para cima",true);
                        if (up==null) up=find(root,"Navigate up",true);
                        if (up!=null) click(up);
                    }
                }
            }
        }
        handler.postDelayed(tick,400);
    }
    AccessibilityNodeInfo find(AccessibilityNodeInfo n, String wanted, boolean exact) {
        if (n==null) return null;
        CharSequence[] labels={n.getText(),n.getContentDescription(),n.getHintText()};
        if (n.isVisibleToUser()) for (CharSequence s:labels) if (s!=null && (exact ? wanted.equals(s.toString()) : s.toString().contains(wanted))) return n;
        for (int i=0;i<n.getChildCount();i++) { AccessibilityNodeInfo found=find(n.getChild(i),wanted,exact); if(found!=null)return found; }
        return null;
    }
    void click(AccessibilityNodeInfo n) {
        lastClick=System.currentTimeMillis();
        AccessibilityNodeInfo parent=n;
        for(int i=0;i<6 && parent!=null;i++,parent=parent.getParent())
            if(parent.isClickable() && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK))return;
        Rect bounds=new Rect(); n.getBoundsInScreen(bounds);
        if(bounds.isEmpty())return;
        Path path=new Path();path.moveTo(bounds.centerX(),bounds.centerY());
        dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path,0,80)).build(),null,null);
    }
}
