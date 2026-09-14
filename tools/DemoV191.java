package com.tankecho.zensudoku.tests;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.widget.*;import java.io.*;

/** Native showcase of sixteen art identities and four finish grades. */
public final class DemoV191 extends MedalDeviceTests {
    boolean recording;
    @Override public void onCreate(Bundle args){recording=args!=null&&"true".equals(args.getString("record"));super.onCreate(args);}
    File flag(String n){return new File(getTargetContext().getFilesDir(),n);}
    void mark(String n)throws Exception{flag(n).createNewFile();}
    void waitFor(String n){for(int i=0;i<350&&!flag(n).exists();i++)SystemClock.sleep(100);if(!flag(n).exists())throw new AssertionError("Missing "+n);}
    void scene(String name,long duration)throws Exception{SystemClock.sleep(recording?duration:500);android.view.accessibility.AccessibilityNodeInfo r=getUiAutomation().getRootInActiveWindow();if(r==null||!"com.tankecho.zensudoku".contentEquals(r.getPackageName()))throw new AssertionError("Unexpected foreground "+name);shot(name);}
    @Override public void onStart(){Bundle out=new Bundle();try{
        getUiAutomation();activity=startActivitySync(new Intent().setClassName("com.tankecho.zensudoku","com.tankecho.zensudoku.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        ui(()->{gallery(true,true,0);stamp();});
        SystemClock.sleep(350);ui(()->scrollTo(art("journey-1")));if(recording){mark("v191-demo-ready");waitFor("v191-demo-start");}scene("journey-grades",3000);
        ui(()->choose("奖章 初次相遇"));scene("bronze-detail",3300);ui(()->((AlertDialog)field(activity,"medalDialog")).dismiss());
        ui(()->choose("奖章 百次回响"));scene("royal-detail",6800);ui(()->((AlertDialog)field(activity,"medalDialog")).dismiss());
        ui(()->scrollTo(art("insight-independent")));scene("insight-symbols",3200);
        ui(()->{invoke("changeTheme",new Class[]{int.class},1);stamp();});SystemClock.sleep(350);ui(()->scrollTo(art("rhythm-3")));scene("rhythm-dark",3300);
        ui(()->choose("奖章 月光守候"));scene("moon-detail",4200);ui(()->((AlertDialog)field(activity,"medalDialog")).dismiss());
        ui(()->scrollTo(art("focus-first")));scene("focus-dark",3500);ui(()->choose("奖章 时间的朋友"));scene("time-royal",6500);ui(()->((AlertDialog)field(activity,"medalDialog")).dismiss());
        ui(()->{gallery(false,true,1);stamp();});SystemClock.sleep(350);ui(()->scrollTo(art("insight-independent")));scene("locked-collection",2200);
        if(recording){mark("v191-demo-done");waitFor("v191-demo-stop");}ui(()->activity.finish());out.putString("stream","DEMO_V191_SUCCESS: 9 native scenes, labeled fixtures, real ranked motion\n");finish(Activity.RESULT_OK,out);
    }catch(Throwable e){out.putString("stream","FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,out);}}
}
