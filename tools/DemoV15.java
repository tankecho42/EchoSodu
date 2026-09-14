package com.tankecho.zensudoku.tests;
import android.app.*;import android.content.*;import android.os.*;import android.graphics.*;import java.io.*;import java.lang.reflect.*;import java.util.*;
/** Test-only staged game, played by release controls; no fixture is included in release. */
public class DemoV15 extends DemoV14 {
    private void set(Object o,String name,Object value)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
    private void shot(String name)throws Exception{Bitmap b=getUiAutomation().takeScreenshot();File dir=new File(getTargetContext().getFilesDir(),"v15-proof");dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
    private void stage(int themeId)throws Exception{
        invoke("start",new Class[]{int.class,boolean.class},0,false);int[] answer=values("solution").clone(),puzzle=answer.clone();for(int at:new int[]{0,1,2,9,10,11,18,19,20,40,80})puzzle[at]=0;
        Object g=game().getClass().getConstructor(int[].class,int[].class,int.class).newInstance(puzzle,answer,0);set(g,"quickMode",true);set(activity,"game",g);invoke("changeTheme",new Class[]{int.class},themeId);
    }
    private void fill(int at){ui(()->{int n=values("solution")[at];if(integer("activeNumber")!=n)click("输入数字 "+n);select(at);});}
    @Override public void onStart(){Bundle out=new Bundle();try{
        activity=startActivitySync(new Intent().setClassName("com.tankecho.zensudoku","com.tankecho.zensudoku.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        ui(()->{Object s=field(activity,"store");set(s,"motion",true);invoke("changeTheme",new Class[]{int.class},0);});SystemClock.sleep(650);new File(getTargetContext().getFilesDir(),"v15-ready").createNewFile();SystemClock.sleep(700);shot("home-integrated.png");SystemClock.sleep(900);
        ui(()->invoke("navigate",new Class[]{String.class},"profile"));SystemClock.sleep(700);shot("profile-integrated.png");SystemClock.sleep(900);
        ui(()->stage(0));SystemClock.sleep(650);
        for(int at:new int[]{0,1,2,9,10,11,18,19}){fill(at);SystemClock.sleep(180);}fill(20);SystemClock.sleep(520);shot("completion-white.png");SystemClock.sleep(1100);
        ui(()->click("暂停游戏"));SystemClock.sleep(650);shot("rest-transparent.png");SystemClock.sleep(1000);
        ui(()->{click("继续游戏");invoke("changeTheme",new Class[]{int.class},1);});SystemClock.sleep(500);fill(40);SystemClock.sleep(450);fill(80);
        ui(()->{Object t=field(field(activity,"boardView"),"timeline");List<?> list=(List<?>)t.getClass().getMethod("active",long.class).invoke(t,SystemClock.uptimeMillis());int kinds=0;for(Object e:list)kinds|=1<<(Integer)field(e,"kind");ok((kinds&30)==30,"row column box and digit complete concurrently in release UI");});
        SystemClock.sleep(580);shot("completion-dark.png");SystemClock.sleep(2300);shot("celebrate-transparent.png");SystemClock.sleep(10000);
        ui(()->activity.finish());out.putString("stream",log+"DEMO_V15_SUCCESS\n");finish(Activity.RESULT_OK,out);
    }catch(Throwable e){out.putString("stream","FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,out);}}
}
