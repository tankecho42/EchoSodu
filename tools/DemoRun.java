package com.tankecho.zensudoku.tests;
import android.app.*;import android.content.*;import android.os.*;import android.graphics.*;import java.io.*;import java.lang.reflect.*;import java.util.*;

/** Staged near-complete puzzles, rendered and played by the real app; never shipped. */
public class DemoRun extends DeviceTests {
    @Override void ui(Work work){final Throwable[] error={null};runOnMainSync(()->{try{work.run();}catch(Throwable t){error[0]=t;}});if(error[0]!=null)throw new RuntimeException(error[0]);}
    private void set(Object o,String name,Object value)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
    private void screenshot(String name)throws Exception{Bitmap bitmap=getUiAutomation().takeScreenshot();File dir=new File(getTargetContext().getFilesDir(),"v12-proof");dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}
    private void stage(int themeId,int[] holes)throws Exception{
        invoke("start",new Class[]{int.class,boolean.class},0,false);int[] answer=values("solution").clone(),puzzle=answer.clone();for(int i:holes)puzzle[i]=0;
        Object newGame=game().getClass().getConstructor(int[].class,int[].class,int.class).newInstance(puzzle,answer,0);set(newGame,"quickMode",true);set(activity,"game",newGame);invoke("changeTheme",new Class[]{int.class},themeId);
    }
    private void fill(int at){ui(()->{int n=values("solution")[at];if(integer("activeNumber")!=n)click("输入数字 "+n);select(at);});}
    @Override public void onStart(){Bundle out=new Bundle();try{
        activity=startActivitySync(new Intent().setClassName("com.tankecho.zensudoku","com.tankecho.zensudoku.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));waitForIdleSync();
        ui(()->stage(1,new int[]{0,1,2,9,10,11,18,19,20,40,80}));SystemClock.sleep(1000);
        new File(getTargetContext().getFilesDir(),"demo-ready").createNewFile();
        SystemClock.sleep(2100);
        int[] order={0,1,2,9,10,11,18,19,20};
        for(int at:order){fill(at);SystemClock.sleep(180);}
        screenshot("parallel-dark.png");SystemClock.sleep(1850);
        fill(40);SystemClock.sleep(250);fill(80);SystemClock.sleep(380);screenshot("digit-dark.png");SystemClock.sleep(2600);screenshot("result-dark.png");
        ui(()->activity.finish());out.putString("stream","DEMO_SUCCESS\n");finish(Activity.RESULT_OK,out);
    }catch(Throwable e){out.putString("stream","FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,out);}}
}
