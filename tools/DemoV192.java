package com.tankecho.zensudoku.tests;
import android.app.*;import android.content.*;import android.os.*;import java.io.*;

public final class DemoV192 extends SolarDeviceTests {
    boolean recording;
    @Override public void onCreate(Bundle args){recording=args!=null&&"true".equals(args.getString("record"));super.onCreate(args);}
    File flag(String n){return new File(getTargetContext().getFilesDir(),n);}
    void mark(String n)throws Exception{flag(n).createNewFile();}
    void waitFor(String n){for(int i=0;i<350&&!flag(n).exists();i++)SystemClock.sleep(100);if(!flag(n).exists())throw new AssertionError("Missing "+n);}
    void scene(String name,long duration)throws Exception{SystemClock.sleep(recording?duration:500);android.view.accessibility.AccessibilityNodeInfo r=getUiAutomation().getRootInActiveWindow();if(r==null||!"com.tankecho.zensudoku".contentEquals(r.getPackageName()))throw new AssertionError("Unexpected foreground "+name);shot(name);}
    @Override public void onStart(){Bundle out=new Bundle();try{
        getUiAutomation();activity=startActivitySync(new Intent().setClassName("com.tankecho.zensudoku","com.tankecho.zensudoku.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        ui(()->{gallery(true,true,0);stamp();});SystemClock.sleep(350);ui(()->scrollTo(art("journey-50")));
        if(recording){mark("v192-demo-ready");waitFor("v192-demo-start");}scene("sun-gallery-white",2600);
        ui(()->choose("奖章 熟悉的风景"));scene("sun-gold-white",5800);ui(()->((AlertDialog)field(activity,"medalDialog")).dismiss());SystemClock.sleep(300);
        ui(()->choose("奖章 百次回响"));scene("sun-royal-white",6600);ui(()->((AlertDialog)field(activity,"medalDialog")).dismiss());SystemClock.sleep(300);
        ui(()->{invoke("changeTheme",new Class[]{int.class},1);stamp();});SystemClock.sleep(350);ui(()->scrollTo(art("journey-50")));scene("sun-gallery-dark",2400);
        ui(()->choose("奖章 百次回响"));scene("sun-royal-dark",6600);ui(()->((AlertDialog)field(activity,"medalDialog")).dismiss());SystemClock.sleep(300);
        ui(()->{scrollTo(art("rhythm-30"));choose("奖章 月光守候");});scene("sun-moon-dark",5800);
        if(recording){mark("v192-demo-done");waitFor("v192-demo-stop");}ui(()->activity.finish());out.putString("stream","DEMO_V192_SUCCESS: 6 native solar-motion scenes; labeled fixtures\n");finish(Activity.RESULT_OK,out);
    }catch(Throwable e){out.putString("stream","FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,out);}}
}
