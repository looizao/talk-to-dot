package local.zipshortcut;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

public class ZipService extends AccessibilityService {
    static ZipService instance;
    private static final long WATCHDOG_MS = 120;
    private static final long RETRY_MS = 700;
    private static final long DOT_SETTLE_MS = 4000;
    final Handler handler = new Handler(Looper.getMainLooper());
    boolean running;
    long lastClick;
    long startedAt;
    long composerSeenAt;
    int composerWindow = -1;
    String lastAction = "";
    String targetName = DotSettings.DEFAULT_NAME;
    final Runnable tick = () -> step();

    @Override protected void onServiceConnected() {
        instance = this;
        if (running) schedule(0);
        else if (armed()) begin();
    }
    @Override public void onDestroy() { instance = null; handler.removeCallbacks(tick); super.onDestroy(); }
    @Override public void onInterrupt() {
        // Android interrupts Accessibility feedback, which this service does not produce.
        // Opening a media app must not cancel the explicitly armed navigation request.
        android.util.Log.d("TalkToZip", "Feedback interrupted; navigation remains armed");
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent e) {
        if (!running && armed()) begin();
        else if (running) schedule(0);
    }
    boolean armed() { return getSharedPreferences("shortcut",0).getLong("armedUntil",0)>System.currentTimeMillis(); }
    void begin() {
        targetName = DotSettings.name(this);
        startedAt = getSharedPreferences("shortcut",0).getLong("startedAtElapsed",SystemClock.elapsedRealtime());
        running=true; lastClick=0; lastAction=""; composerSeenAt=0; composerWindow=-1;
        android.util.Log.d("TalkToZip", "Navigation armed");
        schedule(0);
    }
    void schedule(long delay) {
        handler.removeCallbacks(tick);
        handler.postDelayed(tick,delay);
    }
    void stop() {
        running=false; handler.removeCallbacks(tick);
        getSharedPreferences("shortcut",0).edit().remove("armedUntil").remove("startedAtElapsed").apply();
    }
    void step() {
        if (!armed()) {
            android.util.Log.d("TalkToZip", "Navigation deadline expired");
            if (running) Toast.makeText(this,"Could not select " + targetName + ". Open ChatGPT’s menu and check its name.",Toast.LENGTH_LONG).show();
            stop(); return;
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null && root.getPackageName()!=null && "com.openai.chatgpt".contentEquals(root.getPackageName()) && root.refresh()) {
            boolean sidebar = find(root,"Scheduled",true)!=null;
            if (!sidebar && find(root,"Message " + targetName,true)!=null) {
                long now=SystemClock.elapsedRealtime();
                if (composerWindow!=root.getWindowId() || composerSeenAt==0) {
                    composerWindow=root.getWindowId(); composerSeenAt=now;
                }
                // Confirm the destination survived a screen update instead of accepting a stale tree.
                if (now-composerSeenAt>=150) {
                    android.util.Log.d("TalkToZip", "Navigation completed in " + (now-startedAt) + " ms");
                    stop(); return;
                }
                schedule(150-(now-composerSeenAt)); return;
            }
            composerSeenAt=0; composerWindow=-1;
            // The old chat controls can remain visible while the selected Dot loads.
            // Do not reopen the menu during that transition; recover if it stalls.
            if (!sidebar && "dot".equals(lastAction)
                    && SystemClock.elapsedRealtime()-lastClick<DOT_SETTLE_MS) {
                schedule(WATCHDOG_MS); return;
            }
            AccessibilityNodeInfo target;
            String action;
            if (sidebar) { target=find(root,targetName,true); action="dot"; }
            else {
                target=find(root,"Menu",true); action="menu";
                if (target==null) {
                    action="up";
                    target=find(root,"Navegar para cima",true);
                    if (target==null) target=find(root,"Navigate up",true);
                }
            }
            long sinceClick=SystemClock.elapsedRealtime()-lastClick;
            // Advance immediately when the next screen exposes a different action.
            // Retry an unchanged screen slowly to avoid toggling its menu twice.
            if (target!=null && (!action.equals(lastAction) || sinceClick>=RETRY_MS)) {
                if (click(target)) { lastAction=action; android.util.Log.d("TalkToZip", "Navigation action: " + action); }
            }
        }
        schedule(WATCHDOG_MS);
    }
    AccessibilityNodeInfo find(AccessibilityNodeInfo n, String wanted, boolean exact) {
        if (n==null) return null;
        CharSequence[] labels={n.getText(),n.getContentDescription(),n.getHintText()};
        if (n.isVisibleToUser()) for (CharSequence s:labels) if (s!=null && (exact ? wanted.equals(s.toString()) : s.toString().contains(wanted))) return n;
        for (int i=0;i<n.getChildCount();i++) { AccessibilityNodeInfo found=find(n.getChild(i),wanted,exact); if(found!=null)return found; }
        return null;
    }
    boolean isCurrentChatGptWindow(AccessibilityNodeInfo n) {
        AccessibilityNodeInfo current=getRootInActiveWindow();
        return current!=null && current.getWindowId()==n.getWindowId()
                && current.getPackageName()!=null
                && "com.openai.chatgpt".contentEquals(current.getPackageName());
    }
    boolean click(AccessibilityNodeInfo n) {
        if (!isCurrentChatGptWindow(n) || !n.refresh() || !n.isVisibleToUser()) return false;
        AccessibilityNodeInfo parent=n;
        for(int i=0;i<6 && parent!=null;i++,parent=parent.getParent())
            if(parent.isClickable() && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                lastClick=SystemClock.elapsedRealtime();
                return true;
            }
        Rect bounds=new Rect(); n.getBoundsInScreen(bounds);
        if(bounds.isEmpty() || !isCurrentChatGptWindow(n))return false;
        Path path=new Path();path.moveTo(bounds.centerX(),bounds.centerY());
        boolean dispatched=dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path,0,80)).build(),null,null);
        if (dispatched) lastClick=SystemClock.elapsedRealtime();
        return dispatched;
    }
}
