package com.tankecho.zensudoku.tests;
import android.app.*;import android.content.*;import android.os.*;import android.graphics.*;import java.io.*;

/** Actual release UI with test-only near-complete puzzles, for visual delivery. */
public final class DemoV16 extends VictoryDeviceTests {
    private void shot(String name)throws Exception{Bitmap b=getUiAutomation().takeScreenshot();File dir=new File(getTargetContext().getFilesDir(),"v16-proof");dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
    @Override public void onStart(){Bundle out=new Bundle();try{
        activity=startActivitySync(new Intent().setClassName("com.tankecho.zensudoku","com.tankecho.zensudoku.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        ui(()->stage(false,true,0));SystemClock.sleep(600);new File(getTargetContext().getFilesDir(),"v16-ready").createNewFile();SystemClock.sleep(900);
        ui(()->finishCell());SystemClock.sleep(720);shot("victory-wave-white.png");SystemClock.sleep(820);shot("victory-confetti-white.png");SystemClock.sleep(2500);shot("victory-result-white.png");SystemClock.sleep(800);
        ui(()->stage(true,true,1));SystemClock.sleep(850);ui(()->finishCell());SystemClock.sleep(720);shot("victory-wave-dark.png");SystemClock.sleep(820);shot("victory-confetti-dark.png");SystemClock.sleep(2500);shot("victory-result-dark.png");SystemClock.sleep(6000);
        ui(()->{ok(dialog()&&field(activity,"victoryOverlay")==null,"demo ends with settled board and one result dialog");activity.finish();});out.putString("stream",log+"DEMO_V16_SUCCESS\n");finish(Activity.RESULT_OK,out);
    }catch(Throwable e){out.putString("stream",log+"FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,out);}}
}
