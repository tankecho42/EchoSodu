package com.tankecho.zensudoku.tests;
import android.app.*;import android.content.*;import android.os.*;import android.graphics.*;import java.io.*;

/** Records naturally scheduled blinks on the actual home/profile screens. */
public final class DemoBlink extends BlinkDeviceTests {
    private void shot(String name)throws Exception{Bitmap b=getUiAutomation().takeScreenshot();File dir=new File(getTargetContext().getFilesDir(),"blink-proof");dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
    private void waitForClosed()throws Exception{long deadline=SystemClock.uptimeMillis()+8000;final boolean[] closed={false};while(SystemClock.uptimeMillis()<deadline){ui(()->closed[0]=(Float)field(art(),"blinkAmount")>.98f);if(closed[0])return;SystemClock.sleep(8);}throw new AssertionError("natural blink not observed");}
    @Override public void onStart(){Bundle out=new Bundle();try{
        activity=startActivitySync(new Intent().setClassName("com.tankecho.zensudoku","com.tankecho.zensudoku.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        ui(()->{set(field(activity,"store"),"motion",true);invoke("navigate",new Class[]{String.class},"home");invoke("changeTheme",new Class[]{int.class},0);});SystemClock.sleep(400);new File(getTargetContext().getFilesDir(),"blink-ready").createNewFile();SystemClock.sleep(300);shot("home-open.png");waitForClosed();shot("home-blink.png");SystemClock.sleep(2100);
        ui(()->{invoke("navigate",new Class[]{String.class},"profile");invoke("changeTheme",new Class[]{int.class},1);});SystemClock.sleep(500);shot("profile-open.png");waitForClosed();shot("profile-blink.png");SystemClock.sleep(8000);
        ui(()->activity.finish());out.putString("stream","DEMO_BLINK_SUCCESS: natural home and profile blinks\n");finish(Activity.RESULT_OK,out);
    }catch(Throwable e){out.putString("stream","FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,out);}}
}
