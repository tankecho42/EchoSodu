package com.tankecho.zensudoku.tests;
import android.app.*;import android.content.*;import android.os.*;import android.graphics.*;import android.view.*;import android.animation.ValueAnimator;import java.io.*;

/** Visibility measurements in actual rendered badge pixels, plus light/dark and clipping checks. */
public class SolarDeviceTests extends MedalDeviceTests {
    @Override void shot(String name)throws Exception{Bitmap b=getUiAutomation().takeScreenshot();File folder=new File(getTargetContext().getFilesDir(),"v192-proof");folder.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(folder,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
    void rayVisibility(View art,String name)throws Exception{
        Bitmap b=renderArt(art);int visible=0,total=0,edge=0;
        for(int y=0;y<b.getHeight();y++)for(int x=0;x<b.getWidth();x++){
            int color=b.getPixel(x,y),alpha=Color.alpha(color);float nx=(x-168)/(336f/136),ny=(y-192)/(384f/152)+4;double radius=Math.hypot(nx,ny);
            if(ny<-4&&radius>=54&&radius<=64){total++;int contrast=Math.max(Math.abs(245-Color.red(color)),Math.max(Math.abs(247-Color.green(color)),Math.abs(250-Color.blue(color))));if(alpha>=35&&alpha*contrast/255>=10)visible++;}
            if((x<=1||x>=334||y<=1||y>=382)&&alpha>20)edge++;
        }
        ok(visible>total*.10,name+" clearly colored radiance beyond old rim: "+visible+" / "+total);
        ok(edge==0,name+" luminous art fits transparent bounds");b.recycle();
    }
    void awaitMotion(String id)throws Exception{final boolean[] ready={false};for(int i=0;i<40;i++){ui(()->ready[0]=field(dialogArt(id),"animator")!=null);if(ready[0])return;SystemClock.sleep(50);}}
    @Override public void onStart(){Bundle out=new Bundle();try{
        getUiAutomation();activity=startActivitySync(new Intent().setClassName("com.tankecho.zensudoku","com.tankecho.zensudoku.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        for(int theme=0;theme<2;theme++){final int t=theme;ui(()->gallery(true,true,t));SystemClock.sleep(350);
            for(String[] item:new String[][]{{"journey-50","奖章 熟悉的风景"},{"journey-100","奖章 百次回响"}}){ui(()->choose(item[1]));awaitMotion(item[0]);
                ui(()->{View close=find(((AlertDialog)field(activity,"medalDialog")).getWindow().getDecorView(),"关闭奖章详情");Rect bounds=new Rect();ok(close.getGlobalVisibleRect(bounds)&&bounds.height()>=close.getHeight()-1,"detail close action fully visible without scrolling");View art=dialogArt(item[0]);ValueAnimator animator=(ValueAnimator)field(art,"animator");ok(animator!=null,"solar detail starts "+item[0]+" enabled="+field(art,"motionEnabled")+" focused="+art.hasWindowFocus()+" shown="+art.isShown()+" attached="+art.isAttachedToWindow()+" dimensions="+art.getWidth()+"x"+art.getHeight()+" pref="+field(field(activity,"store"),"motion")+" foreground="+field(activity,"foreground"));for(long time:new long[]{0,1400,4200}){animator.setCurrentPlayTime(time);rayVisibility(art,"theme "+t+" "+item[0]+" time "+time);}motion(art,false);rayVisibility(art,"motion-off "+item[0]);ok(field(art,"animator")==null,"solar effect respects motion-off");motion(art,true);});
                SystemClock.sleep(250);shot("sun-"+item[0]+"-theme-"+t);ui(()->((AlertDialog)field(activity,"medalDialog")).dismiss());SystemClock.sleep(250);
            }
        }
        ui(()->activity.finish());out.putString("stream",log+"PASS "+checks+" solar visibility and bounds checks\n");finish(Activity.RESULT_OK,out);
    }catch(Throwable e){out.putString("stream",log+"FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,out);}}
}
