package com.tankecho.zensudoku.tests;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.widget.*;import java.io.*;

/** Drives the app's own replay controls; no artificial tutorial outcomes. */
public final class DemoV110 extends TutorialDeviceTests {
    boolean recording;
    @Override public void onCreate(Bundle args){recording=args!=null&&"true".equals(args.getString("record"));super.onCreate(args);}
    File flag(String name){return new File(getTargetContext().getFilesDir(),name);}
    void mark(String name)throws Exception{flag(name).createNewFile();}
    void awaitFlag(String name){for(int i=0;i<400&&!flag(name).exists();i++)SystemClock.sleep(100);if(!flag(name).exists())throw new AssertionError(name);}
    void play(int n,String scene,long snap,long remaining)throws Exception{
        ui(()->{lesson(n);invoke("showDemo",new Class[]{});});SystemClock.sleep(snap);capture(scene);SystemClock.sleep(remaining);ui(()->ok(((Integer)field(activity,"completed")&(1<<n))!=0,"actual replay completes lesson "+n));
    }
    @Override public void onStart(){Bundle out=new Bundle();try{
        getUiAutomation();Activity main=startActivitySync(new Intent().setClassName("com.tankecho.zensudoku","com.tankecho.zensudoku.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));activity=main;ui(()->{invoke("changeTheme",new Class[]{int.class},0);set(field(main,"store"),"motion",true);invoke("navigate",new Class[]{String.class},"settings");});help(main);
        if(recording){mark("v110-demo-ready");awaitFlag("v110-demo-start");}SystemClock.sleep(1800);capture("learn-overview");
        play(0,"learn-rules",3000,1400);play(1,"learn-classic",2100,1400);play(2,"learn-quick",3650,1900);play(3,"learn-notes",1900,3550);play(4,"learn-hint",2100,4400);play(5,"learn-victory",1850,3850);
        ui(()->activity.finish());activity=main;focus();ui(()->invoke("changeTheme",new Class[]{int.class},1));help(main);play(4,"learn-dark",2300,4200);
        ui(()->activity.finish());activity=main;focus();ui(()->{invoke("changeTheme",new Class[]{int.class},0);invoke("navigate",new Class[]{String.class},"settings");});SystemClock.sleep(400);ui(()->{ScrollView sc=(ScrollView)((View)field(main,"content")).getParent();sc.fullScroll(View.FOCUS_DOWN);});SystemClock.sleep(2200);capture("about-github");
        if(recording){mark("v110-demo-done");awaitFlag("v110-demo-stop");}ui(()->main.finish());out.putString("stream",log+"DEMO_V110_SUCCESS: native tutorial replay controls and about page\n");finish(Activity.RESULT_OK,out);
    }catch(Throwable e){out.putString("stream",log+"FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,out);}}
}
