package com.tankecho.zensudoku.tests;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.widget.*;import java.time.*;
public class CaptureV17 extends V17DeviceTests{
 @Override public void onStart(){Bundle out=new Bundle();try{
  activity=startActivitySync(new Intent().setClassName("com.tankecho.zensudoku","com.tankecho.zensudoku.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));boolean small=activity.getResources().getDisplayMetrics().heightPixels<1500;String prefix=small?"small-":"normal-";
  ui(()->{invoke("changeTheme",new Class[]{int.class},0);});shot(prefix+"home.png");
  ui(()->{stage(false);click("全盘候选数");});shot(prefix+"game.png");
  ui(()->{View board=(View)field(activity,"boardView"),key=find(activity.getWindow().getDecorView(),"输入数字 9"),hint=find(activity.getWindow().getDecorView(),"使用提示");android.graphics.Rect r=new android.graphics.Rect();ok(board.getLocalVisibleRect(r)&&r.height()==board.getHeight(),"board fully visible without scrolling");ok(key.getLocalVisibleRect(r)&&r.height()==key.getHeight(),"keypad fully visible without scrolling");ok(hint.getLocalVisibleRect(r)&&r.height()==hint.getHeight(),"hint fully visible without scrolling");click("使用提示");});shot(prefix+"hint.png");
  ui(()->{View positive=hint().getButton(AlertDialog.BUTTON_POSITIVE);android.graphics.Rect r=new android.graphics.Rect();ok(positive.getLocalVisibleRect(r)&&r.height()==positive.getHeight(),"hint apply button fully visible");hint().dismiss();invoke("navigate",new Class[]{String.class},"home");invoke("showDailyCalendar",new Class[]{YearMonth.class},YearMonth.now());});shot(prefix+"daily.png");
  ui(()->{AlertDialog d=(AlertDialog)field(activity,"calendarDialog");View next=find(d.getWindow().getDecorView(),"下个月");ok(!next.isEnabled(),"future calendar month disabled");LocalDate tomorrow=LocalDate.now().plusDays(1);if(YearMonth.from(tomorrow).equals(YearMonth.now()))ok(!find(d.getWindow().getDecorView(),"每日挑战 "+tomorrow+"，尚未开放").isEnabled(),"future calendar day disabled");d.dismiss();invoke("navigate",new Class[]{String.class},"profile");});shot(prefix+"profile.png");
  ui(()->{View section=findText(activity.getWindow().getDecorView(),"最近 30 天 · 同难度用时");ScrollView sc=(ScrollView)((View)field(activity,"content")).getParent();View card=(View)section.getParent();sc.scrollTo(0,card.getTop());});shot(prefix+"trend.png");
  ui(()->{ScrollView sc=(ScrollView)((View)field(activity,"content")).getParent();sc.fullScroll(View.FOCUS_DOWN);});shot(prefix+"settings.png");
  ui(()->{View export=find(activity.getWindow().getDecorView(),"导出存档");android.graphics.Rect r=new android.graphics.Rect();ok(export.getLocalVisibleRect(r)&&r.height()==export.getHeight(),"backup export reachable in settings");invoke("changeTheme",new Class[]{int.class},1);stage(true);});shot(prefix+"dark-game.png");ui(()->activity.finish());
  out.putString("stream",log+"CAPTURE_V17_SUCCESS "+checks+" checks "+prefix);finish(Activity.RESULT_OK,out);
 }catch(Throwable e){out.putString("stream",log+"FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,out);}}
}
