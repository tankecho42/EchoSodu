package com.tankecho.zensudoku.tests;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.widget.*;import java.io.*;import org.json.*;

/** Capture-only fixtures never included in the application package. */
public final class DemoV19 extends V19DeviceTests {
    boolean recording;
    @Override public void onCreate(Bundle args){recording=args!=null&&"true".equals(args.getString("record"));super.onCreate(args);}
    File flag(String name){return new File(getTargetContext().getFilesDir(),name);}
    void mark(String name)throws Exception{flag(name).createNewFile();}
    void waitFor(String name){for(int i=0;i<350&&!flag(name).exists();i++)SystemClock.sleep(100);if(!flag(name).exists())throw new AssertionError("Missing recording flag "+name);}
    @Override void shot(String name)throws Exception{android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();if(root==null||!"com.tankecho.zensudoku".contentEquals(root.getPackageName()))throw new AssertionError("Unexpected recording foreground at "+name);super.shot(name);}
    void scene(String name)throws Exception{SystemClock.sleep(recording?1700:400);shot(name);}
    void scrollTo(View target)throws Exception{ScrollView scroll=(ScrollView)((View)field(activity,"content")).getParent();android.graphics.Rect r=new android.graphics.Rect();target.getDrawingRect(r);scroll.offsetDescendantRectToMyCoords(target,r);scroll.smoothScrollTo(0,Math.max(0,r.top-30));}
    void top()throws Exception{((ScrollView)((View)field(activity,"content")).getParent()).scrollTo(0,0);}
    @Override public void onStart(){Bundle out=new Bundle();try{
        getUiAutomation();activity=startActivitySync(new Intent().setClassName("com.tankecho.zensudoku","com.tankecho.zensudoku.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        ui(()->{dismiss();set(activity,"game",null);Object store=field(activity,"store");set(store,"current","");seedRecords();set(store,"motion",true);set(store,"quickMode",false);invoke("changeTheme",new Class[]{int.class},0);invoke("navigate",new Class[]{String.class},"home");stamp();});
        if(recording){mark("v19-demo-ready");waitFor("v19-demo-start");}scene("home-fresh");
        ui(()->{click("开始新游戏");stamp();});scene("setup-mode");ui(()->{choose("不限错练习模式");choose("选择困难难度");stamp();});scene("setup-level");ui(()->{click("确认开始游戏");stamp();});scene("game");
        ui(()->{click("游戏中心");stamp();});scene("home-resume");ui(()->{click("统计");set(activity,"statsTab",0);invoke("render",new Class[]{});stamp();});scene("overview");
        ui(()->scrollTo(findText(decor(),"最近 7 天")));scene("weekly");ui(()->scrollTo(find(decor(),"最近13周游玩热力图")));scene("heatmap");ui(()->scrollTo(findText(decor(),"四种难度，四份成长")));scene("difficulty-records");
        ui(()->{click("统计奖章");stamp();});scene("medals");ui(()->scrollTo(prefix(decor(),"奖章 初次相遇")));scene("medal-journey");
        ui(()->choose("奖章 十格远行"));scene("medal-earned");ui(()->((AlertDialog)field(activity,"medalDialog")).dismiss());
        ui(()->{invoke("changeTheme",new Class[]{int.class},1);stamp();scrollTo(prefix(decor(),"奖章 独立的光"));});scene("medal-insight-dark");
        ui(()->scrollTo(prefix(decor(),"奖章 三日约定")));scene("medal-rhythm-dark");ui(()->choose("奖章 月光守候"));scene("medal-locked-dark");ui(()->((AlertDialog)field(activity,"medalDialog")).dismiss());
        ui(()->scrollTo(prefix(decor(),"奖章 慢慢来也好")));scene("medal-focus-dark");ui(()->{click("统计记录");stamp();});scene("history-dark");
        ui(()->{click("设置");invoke("changeTheme",new Class[]{int.class},0);stamp();});scene("settings");ui(()->scrollTo(findText(decor(),"关于 EchoSudoku")));scene("about");
        if(recording){mark("v19-demo-done");waitFor("v19-demo-stop");}ui(()->activity.finish());out.putString("stream","DEMO_V19_SUCCESS: 19 native scenes; sample statistics labeled; no fixture code in release\n");finish(Activity.RESULT_OK,out);
    }catch(Throwable e){out.putString("stream","FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,out);}}
}
