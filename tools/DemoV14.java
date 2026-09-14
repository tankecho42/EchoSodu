package com.tankecho.zensudoku.tests;
import android.app.*;import android.content.*;import android.os.*;import android.graphics.*;import android.view.accessibility.*;import java.io.*;import java.lang.reflect.*;

/** Test-only staged screens for recording the actual release app. */
public class DemoV14 extends DeviceTests {
    @Override void ui(Work work){final Throwable[] error={null};runOnMainSync(()->{try{work.run();}catch(Throwable t){error[0]=t;}});if(error[0]!=null)throw new RuntimeException(error[0]);}
    private void set(Object o,String name,Object value)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
    private void screenshot(String name)throws Exception{Bitmap b=getUiAutomation().takeScreenshot();File dir=new File(getTargetContext().getFilesDir(),"v14-proof");dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
    private boolean described(AccessibilityNodeInfo n,String label){if(n==null)return false;if(label.contentEquals(n.getContentDescription()==null?"":n.getContentDescription()))return true;for(int i=0;i<n.getChildCount();i++)if(described(n.getChild(i),label))return true;return false;}
    @Override public void onStart(){Bundle out=new Bundle();try{
        activity=startActivitySync(new Intent().setClassName("com.tankecho.zensudoku","com.tankecho.zensudoku.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        ui(()->invoke("changeTheme",new Class[]{int.class},0));SystemClock.sleep(700);new File(getTargetContext().getFilesDir(),"v14-ready").createNewFile();SystemClock.sleep(1500);screenshot("home-crayon.png");SystemClock.sleep(1600);
        ui(()->invoke("start",new Class[]{int.class,boolean.class},0,false));SystemClock.sleep(700);screenshot("game-crayon.png");
        ui(()->click("暂停游戏"));SystemClock.sleep(700);screenshot("pause-crayon.png");SystemClock.sleep(2000);
        ui(()->{click("继续游戏");int[] answer=values("solution").clone(),puzzle=answer.clone();puzzle[40]=0;Object g=game().getClass().getConstructor(int[].class,int[].class,int.class).newInstance(puzzle,answer,0);set(activity,"game",g);invoke("changeTheme",new Class[]{int.class},0);select(40);click("输入数字 "+answer[40]);});
        SystemClock.sleep(3100);screenshot("celebrate-crayon.png");if(!described(getUiAutomation().getRootInActiveWindow(),"Echo 白熊，举爪庆祝完成"))throw new AssertionError("celebration pose missing from actual result dialog");SystemClock.sleep(7000);
        ui(()->activity.finish());out.putString("stream","DEMO_V14_SUCCESS: thinking, paused-rest and celebration poses shown in actual app.\n");finish(Activity.RESULT_OK,out);
    }catch(Throwable e){out.putString("stream","FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,out);}}
}
