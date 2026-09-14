package com.tankecho.zensudoku.tests;
import android.app.*;import android.content.*;import android.os.*;import android.graphics.*;import android.view.*;import android.widget.*;import java.io.*;import java.time.*;import org.json.*;

/** Short guided recording of the actual release UI with explicit fixture labeling. */
public final class DemoV18 extends V18DeviceTests {
    File flag(String name){return new File(getTargetContext().getFilesDir(),name);}
    void mark(String name)throws Exception{flag(name).createNewFile();}
    void shot(String name)throws Exception{android.view.accessibility.AccessibilityNodeInfo window=null;for(int i=0;i<8;i++){window=getUiAutomation().getRootInActiveWindow();if(window!=null&&"com.tankecho.zensudoku".contentEquals(window.getPackageName()))break;hold(100);}if(window==null||!"com.tankecho.zensudoku".contentEquals(window.getPackageName()))throw new IllegalStateException("Recording foreground guard at "+name+": "+(window==null?"no accessibility root":window.getPackageName()));Bitmap b=getUiAutomation().takeScreenshot();File dir=new File(getTargetContext().getFilesDir(),"v18-proof");dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
    void stamp(String scene){TextView tag=new TextView(activity);tag.setText("功能演示 · 示例题局  /  "+scene);tag.setTextSize(10);tag.setTextColor(0xff7c8792);tag.setPadding(0,4,20,0);tag.setGravity(Gravity.RIGHT);activity.addContentView(tag,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));}
    void hold(long ms){SystemClock.sleep(ms);}
    @Override public void onStart(){Bundle out=new Bundle();try{
        getUiAutomation();
        activity=startActivitySync(new Intent().setClassName("com.tankecho.zensudoku","com.tankecho.zensudoku.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        ui(()->{dismiss();Object store=field(activity,"store");JSONObject records=new JSONObject(),days=new JSONObject();for(int d=0;d<14;d++){if(d==3||d==7)continue;String date=LocalDate.now().minusDays(13-d).toString();long ms=(480+d*17)*1000L;String id="v18-demo-"+d;records.put(id,new JSONObject().put("id",id).put("date",date).put("endDate",date).put("status",d%6==0?"lost":"won").put("level",0).put("ms",ms).put("hints",0).put("mistakes",d%6==0?3:0).put("daily",d%6==0?"":date));days.put(date,new JSONObject().put("ms",ms));}set(store,"records",records);set(store,"days",days);set(activity,"game",null);set(store,"motion",true);invoke("changeTheme",new Class[]{int.class},0);invoke("navigate",new Class[]{String.class},"home");stamp("首页 · 探头");});mark("v18-demo-ready");for(int i=0;i<300&&!flag("v18-demo-start").exists();i++)hold(100);hold(2000);shot("home");
        ui(()->{invoke("navigate",new Class[]{String.class},"profile");stamp("个人中心 · 手账");});hold(1900);shot("profile-white");
        ui(()->{invoke("changeTheme",new Class[]{int.class},1);stamp("深色主题 · 手账");});hold(1800);shot("profile-dark");
        ui(()->{invoke("navigate",new Class[]{String.class},"home");stamp("每日挑战 · 示例记录");invoke("showDailyCalendar",new Class[]{YearMonth.class},YearMonth.now());});hold(2100);shot("daily-dark");ui(()->((AlertDialog)field(activity,"calendarDialog")).dismiss());
        String[] names={"", "行完成 · 从左到右", "列完成 · 从上到下", "宫完成 · 对角扩散", "数字完成 · 九格依次起伏", "多组完成 · 同时起伏"};
        for(int kind=1;kind<=5;kind++){final int k=kind;ui(()->{region(k,k==4,true,k==2||k==5?1:0);stamp(names[k]);select(40);});hold(850);ui(()->place());hold(430);shot("wave-"+k);hold(2100);}
        ui(()->{region(1,false,true,0);stamp("提示 · 一起思考");select(40);click("使用提示");});hold(2400);shot("hint-white");ui(()->((AlertDialog)field(activity,"hintDialog")).getButton(AlertDialog.BUTTON_POSITIVE).performClick());hold(2200);
        ui(()->click("暂停游戏"));hold(1800);shot("pause");
        ui(()->{click("继续游戏");region(1,false,true,1);set(game(),"mistakes",2);invoke("updateGame",new Class[]{});select(40);click("输入数字 "+(values("solution")[40]%9+1));});hold(1600);shot("retry-dark");hold(1400);
        ui(()->{stage(false,true,0);stamp("全盘完成 · 双重波浪与庆祝");finishCell();});hold(750);shot("victory-wave");hold(3500);shot("victory-result");hold(2000);mark("v18-demo-done");for(int i=0;i<300&&!flag("v18-demo-stop").exists();i++)hold(100);ui(()->activity.finish());
        out.putString("stream","DEMO_V18_SUCCESS: seven contextual poses, four regional waves, concurrent waves, whole-board victory\n");finish(Activity.RESULT_OK,out);
    }catch(Throwable e){out.putString("stream","FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,out);}}
}
