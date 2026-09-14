package com.tankecho.zensudoku.tests;
import android.app.*;import android.content.*;import android.os.*;import android.graphics.*;import android.view.*;import android.widget.*;import java.io.*;
/** Release-layout evidence only; kept outside shipped APK. */
public class CaptureV14 extends DemoV14 {
    private ScrollView scroll(View v){if(v instanceof ScrollView)return (ScrollView)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){ScrollView s=scroll(((ViewGroup)v).getChildAt(i));if(s!=null)return s;}return null;}
    private void shot(String name)throws Exception{SystemClock.sleep(650);Bitmap b=getUiAutomation().takeScreenshot();File dir=new File(getTargetContext().getFilesDir(),"v14-proof");dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
    @Override public void onStart(){Bundle out=new Bundle();try{
        activity=startActivitySync(new Intent().setClassName("com.tankecho.zensudoku","com.tankecho.zensudoku.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        boolean small=activity.getResources().getDisplayMetrics().widthPixels<800;String prefix=small?"small":"dark";
        ui(()->invoke("changeTheme",new Class[]{int.class},small?0:1));shot(prefix+"-home.png");
        ui(()->{invoke("navigate",new Class[]{String.class},"profile");});shot(prefix+"-profile.png");
        ui(()->scroll(activity.getWindow().getDecorView()).fullScroll(View.FOCUS_DOWN));shot(prefix+"-settings.png");
        ui(()->{View theme=find(activity.getWindow().getDecorView(),"切换主题");int[] xy=new int[2];theme.getLocationOnScreen(xy);ok(theme.isShown()&&xy[1]>=0&&xy[1]+theme.getHeight()<activity.getResources().getDisplayMetrics().heightPixels,"theme control fully reachable after scroll");invoke("start",new Class[]{int.class,boolean.class},0,false);});shot(prefix+"-game.png");
        ui(()->click("暂停游戏"));shot(prefix+"-pause.png");
        ui(()->{View resume=findText(activity.getWindow().getDecorView(),"继续专注");Rect visible=new Rect();ok(resume.getLocalVisibleRect(visible)&&visible.height()==resume.getHeight(),"pause resume button fully visible without clipping");View art=activity.getWindow().getDecorView().findViewWithTag("echo:1");ok(art.getLocalVisibleRect(visible)&&visible.height()>=art.getHeight()-2,"rest artwork fits pause panel");});
        ui(()->{View line=findText(activity.getWindow().getDecorView(),"计时已暂停，进度已保存");int[] xy=new int[2];line.getLocationOnScreen(xy);log.append("pause subtitle x=").append(xy[0]).append(" width=").append(line.getWidth()).append(" gravity=").append(((TextView)line).getGravity()).append('\n');click("继续游戏");scroll(activity.getWindow().getDecorView()).fullScroll(View.FOCUS_DOWN);});shot(prefix+"-game-actions.png");
        ui(()->{View hint=find(activity.getWindow().getDecorView(),"使用提示"),key=find(activity.getWindow().getDecorView(),"输入数字 9");int[] xy=new int[2];key.getLocationOnScreen(xy);ok(key.isShown()&&xy[1]>0&&xy[1]+key.getHeight()<activity.getResources().getDisplayMetrics().heightPixels,"keypad fully reachable on viewport");hint.getLocationOnScreen(xy);ok(hint.isShown()&&xy[1]>=0,"hint reachable on viewport");activity.finish();});
        out.putString("stream",log+"CAPTURE_V14_SUCCESS "+prefix+"\n");finish(Activity.RESULT_OK,out);
    }catch(Throwable e){out.putString("stream","FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,out);}}
}
