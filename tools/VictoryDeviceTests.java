package com.tankecho.zensudoku.tests;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import java.lang.reflect.*;import java.util.*;

/** Win input paths and lifecycle, through the actual release activity. */
public class VictoryDeviceTests extends DemoV14 {
    void set(Object o,String name,Object value)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
    void dismiss()throws Exception{AlertDialog d=(AlertDialog)field(activity,"settlementDialog");if(d!=null)d.dismiss();}
    void stage(boolean quick,boolean motion,int theme)throws Exception{
        dismiss();Object store=field(activity,"store");set(store,"motion",motion);invoke("start",new Class[]{int.class,boolean.class},0,false);
        int[] answer=values("solution").clone(),puzzle=answer.clone();puzzle[80]=0;
        Object g=game().getClass().getConstructor(int[].class,int[].class,int.class).newInstance(puzzle,answer,0);set(g,"quickMode",quick);set(activity,"game",g);invoke("changeTheme",new Class[]{int.class},theme);
    }
    void finishCell()throws Exception{int n=values("solution")[80];if((Boolean)field(game(),"quickMode")){click("输入数字 "+n);select(80);}else{select(80);click("输入数字 "+n);}}
    boolean waving()throws Exception{Object b=field(activity,"boardView");return b!=null&&(Boolean)b.getClass().getMethod("isVictoryRunning").invoke(b);}
    boolean dialog()throws Exception{AlertDialog d=(AlertDialog)field(activity,"settlementDialog");return d!=null&&d.isShowing();}
    void verifyStarted(String label)throws Exception{
        ok(status().equals("won")&&waving(),label+" starts whole-board wave");View overlay=(View)field(activity,"victoryOverlay");ok(overlay!=null&&overlay.isAttachedToWindow()&&!overlay.isClickable(),label+" starts nonblocking full-screen overlay");ok(!dialog(),label+" does not show result prematurely");
    }
    @Override public void onStart(){Bundle result=new Bundle();try{
        Intent launch=new Intent().setClassName("com.tankecho.zensudoku","com.tankecho.zensudoku.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);activity=startActivitySync(launch);
        final int[] wins={0};final long[] elapsed={0};
        ui(()->{stage(false,true,0);wins[0]=(Integer)field(activity,"store").getClass().getMethod("wins").invoke(field(activity,"store"));finishCell();verifyStarted("classic");elapsed[0]=(Long)field(game(),"elapsedMs");
            Object t=field(field(activity,"boardView"),"timeline");List<?> events=(List<?>)t.getClass().getMethod("active",long.class).invoke(t,SystemClock.uptimeMillis());int kinds=0;for(Object e:events)kinds|=1<<(Integer)field(e,"kind");ok((kinds&30)==30,"row column box digit effects coexist with victory");
            Object old=field(activity,"victoryOverlay");invoke("completeVictory",new Class[]{});ok(old==field(activity,"victoryOverlay"),"duplicate completion cannot stack overlays");});
        SystemClock.sleep(650);ui(()->{View overlay=(View)field(activity,"victoryOverlay");View host=activity.findViewById(android.R.id.content);ok(overlay.getWidth()==host.getWidth()&&overlay.getHeight()==host.getHeight(),"confetti spans whole application viewport");ok((Long)field(field(activity,"boardView"),"victoryElapsed")>0,"actual board render advances wave clock");ok((Long)field(game(),"elapsedMs")==elapsed[0],"victory does not add playtime");});
        SystemClock.sleep(2200);ui(()->ok(!dialog()&&waving(),"result waits while second wave is active"));SystemClock.sleep(1200);
        ui(()->{ok(dialog()&&!waving()&&field(activity,"victoryOverlay")==null,"result appears after celebration, overlay removed");ok((Integer)field(activity,"store").getClass().getMethod("wins").invoke(field(activity,"store"))==wins[0]+1,"victory is recorded exactly once");
            stage(true,true,1);finishCell();verifyStarted("quick");ok(integer("activeNumber")==0,"final quick number deselects");invoke("navigate",new Class[]{String.class},"home");ok(field(activity,"victoryOverlay")==null&&field(activity,"pendingResult")==null,"navigation cancels overlay and scheduled result");});
        SystemClock.sleep(3900);ui(()->{ok(!dialog()&&field(activity,"victoryOverlay")==null,"no stale result appears on home");stage(false,true,2);select(80);useHint();verifyStarted("hint");ok(integer("hints")==1,"winning hint consumes one hint");callActivityOnPause(activity);ok(field(activity,"victoryOverlay")==null&&!waving()&&field(activity,"pendingResult")==null,"background stops drawing and result callback");});
        SystemClock.sleep(700);ui(()->{ok(!dialog(),"no background dialog");callActivityOnResume(activity);});SystemClock.sleep(600);
        ui(()->{ok(dialog()&&field(activity,"victoryOverlay")==null,"foreground restores settlement without replaying celebration");stage(false,false,0);finishCell();ok(!waving()&&field(activity,"victoryOverlay")==null,"motion off skips wave and confetti");});SystemClock.sleep(600);
        ui(()->{ok(dialog(),"motion off does not delay result");stage(false,true,0);select(80);int wrong=values("solution")[80]%9+1;for(int i=0;i<3;i++)click("输入数字 "+wrong);ok(status().equals("lost")&&!waving()&&field(activity,"victoryOverlay")==null,"loss never starts victory");});SystemClock.sleep(1000);
        ui(()->{ok(dialog(),"loss result still appears");stage(false,true,0);finishCell();invoke("start",new Class[]{int.class,boolean.class},0,false);ok(field(activity,"victoryOverlay")==null,"new game removes previous victory overlay");});SystemClock.sleep(3900);
        ui(()->{ok(status().equals("playing")&&!dialog(),"old result cannot intrude on new game");activity.finish();});
        result.putString("stream",log+"PASS "+checks+" victory device checks\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){result.putString("stream",log+"FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
