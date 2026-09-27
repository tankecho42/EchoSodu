package com.tankecho.zensudoku.tests;
import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import java.lang.reflect.*;
import java.util.*;
import org.json.*;

/** Separate, same-signer instrumentation. No test entry points in the shipped application. */
public class DeviceTests extends Instrumentation {
    Activity activity;int checks;StringBuilder log=new StringBuilder();
    void ok(boolean value,String name){checks++;if(!value)throw new AssertionError(name);log.append("PASS ").append(name).append('\n');}
    Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    Object game()throws Exception{return field(activity,"game");}
    int integer(String name)throws Exception{return (Integer)field(game(),name);}
    int[] values(String name)throws Exception{return (int[])field(game(),name);}
    String status()throws Exception{return (String)field(game(),"status");}
    View find(View v,String desc){if(v.getContentDescription()!=null&&desc.contentEquals(v.getContentDescription()))return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View found=find(g.getChildAt(i),desc);if(found!=null)return found;}}return null;}
    View findText(View v,String text){if(v instanceof android.widget.TextView&&((android.widget.TextView)v).getText().toString().equals(text))return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View found=findText(g.getChildAt(i),text);if(found!=null)return found;}}return null;}
    void click(String desc){View v=find(activity.getWindow().getDecorView(),desc);if(v==null)throw new AssertionError("missing control "+desc);v.performClick();}
    void useHint()throws Exception{click("使用提示");AlertDialog d=(AlertDialog)field(activity,"hintDialog");if(d!=null){invoke("acceptHint",new Class[]{game().getClass()},game());d.dismiss();}}
    void select(int at)throws Exception{ViewGroup board=(ViewGroup)field(activity,"boardView");board.getChildAt(at).performClick();}
    interface Work{void run()throws Exception;}
    void ui(Work work){final Throwable[] error={null};runOnMainSync(()->{try{work.run();}catch(Throwable t){error[0]=t;}});if(error[0]!=null)throw new RuntimeException(error[0]);waitForIdleSync();}
    void invoke(String method,Class<?>[] types,Object...args)throws Exception{Method m=activity.getClass().getDeclaredMethod(method,types);m.setAccessible(true);m.invoke(activity,args);}
    int empty()throws Exception{int[] b=values("board");for(int i=0;i<81;i++)if(b[i]==0)return i;throw new AssertionError("no empty");}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();try{
        Intent launch=new Intent().setClassName("com.tankecho.zensudoku","com.tankecho.zensudoku.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity=startActivitySync(launch);waitForIdleSync();
        ui(()->{
            Context sandbox=new ContextWrapper(getTargetContext()){@Override public android.content.SharedPreferences getSharedPreferences(String name,int mode){return getBaseContext().getSharedPreferences("v13_migration_test",mode);}};
            android.content.SharedPreferences prefs=sandbox.getSharedPreferences("ignored",0);prefs.edit().clear().putInt("themeId",1).putString("days","{\"2026-09-01\":{\"ms\":1200000}}").commit();
            Class<?> type=Class.forName("com.tankecho.zensudoku.Store");Object local=type.getConstructor(Context.class).newInstance(sandbox);
            ok((Integer)field(local,"themeId")==0,"1.3 appearance migration applies white default");ok(((JSONObject)field(local,"days")).getJSONObject("2026-09-01").getLong("ms")==1200000,"appearance migration preserves daily activity");
            type.getField("themeId").setInt(local,2);type.getMethod("save",Class.forName("com.tankecho.zensudoku.Game")).invoke(local,new Object[]{null});local=type.getConstructor(Context.class).newInstance(sandbox);ok((Integer)field(local,"themeId")==2,"later explicit theme choice survives reload");
            type.getMethod("awaitWrites").invoke(null);Field cache=type.getDeclaredField("CACHE");cache.setAccessible(true);((Map<?,?>)cache.get(null)).remove(prefs);prefs.edit().clear().commit();local=type.getConstructor(Context.class).newInstance(sandbox);ok((Integer)field(local,"themeId")==0,"fresh installation defaults to white");prefs.edit().clear().commit();
            ok(find(activity.getWindow().getDecorView(),"切换主题")==null,"home has no theme shortcut");ok(find(activity.getWindow().getDecorView(),"最近13周游玩热力图")!=null,"home contains contribution heatmap");
            for(String nav:new String[]{"游戏中心","专注对局","个人中心"}){ViewGroup item=(ViewGroup)find(activity.getWindow().getDecorView(),nav);ok(item.getChildCount()==1&&!(item.getChildAt(0) instanceof android.widget.TextView),"icon-only navigation with accessibility name "+nav);}
            ok(find(activity.getWindow().getDecorView(),"游戏中心").isSelected(),"selected navigation icon announced");
            Object store=field(activity,"store");JSONObject original=(JSONObject)field(store,"days");JSONObject fixture=new JSONObject(original.toString());String date=java.time.LocalDate.now().minusDays(1).toString();fixture.put(date,new JSONObject().put("ms",1800000));store.getClass().getField("days").set(store,fixture);invoke("navigate",new Class[]{String.class},"home");
            View day=activity.getWindow().getDecorView().findViewWithTag(date);ok(day!=null&&day.getContentDescription().toString().contains("30"),"heatmap uses actual daily play time");day.performClick();ok(((android.widget.TextView)find(activity.getWindow().getDecorView(),"热力图日期详情")).getText().toString().contains("30"),"heatmap tap shows day details");
            View today=activity.getWindow().getDecorView().findViewWithTag(java.time.LocalDate.now().toString());ok(today!=null,"today has a selectable heatmap cell");ok(activity.getWindow().getDecorView().findViewWithTag(java.time.LocalDate.now().plusDays(1).toString())==null,"future dates cannot be selected");
            store.getClass().getField("days").set(store,original);invoke("navigate",new Class[]{String.class},"profile");View theme=find(activity.getWindow().getDecorView(),"切换主题"),settings=find(activity.getWindow().getDecorView(),"个人中心底部设置");ok(theme!=null&&theme.getParent()==settings,"theme entry lives inside bottom settings");
            ok(activity.getPackageManager().getApplicationIcon(activity.getPackageName()) instanceof android.graphics.drawable.AdaptiveIconDrawable,"launcher uses adaptive Sudoku-primary icon");
            invoke("navigate",new Class[]{String.class},"home");
        });
        ui(()->{ok(find(activity.getWindow().getDecorView(),"选择专家难度")!=null,"home has all difficulty controls");invoke("start",new Class[]{int.class,boolean.class},0,false);invoke("setQuickMode",new Class[]{boolean.class},false);});
        ui(()->ok(find(activity.getWindow().getDecorView(),"切换主题")==null,"game has no theme shortcut"));
        ui(()->{for(String name:new String[]{"echo_peek","echo_rest","echo_celebrate"}){
            int id=activity.getResources().getIdentifier(name,"drawable",activity.getPackageName());android.graphics.Bitmap b=android.graphics.BitmapFactory.decodeResource(activity.getResources(),id);
            ok(b.hasAlpha()&&android.graphics.Color.alpha(b.getPixel(0,0))==0&&android.graphics.Color.alpha(b.getPixel(b.getWidth()/2,b.getHeight()/2))>=250,"actual transparent foreground resource "+name);b.recycle();
        }});
        final Object[] homeMascot={null};
        ui(()->{invoke("navigate",new Class[]{String.class},"home");homeMascot[0]=activity.getWindow().getDecorView().findViewWithTag("echo:0");ok(homeMascot[0]!=null,"daily challenge has peeking crayon Echo");ok(find(activity.getWindow().getDecorView(),"EchoSudoku 数独图标")!=null,"home uses Sudoku-primary app artwork");});
        ui(()->{ok(field(homeMascot[0],"animator")!=null,"visible peeking mascot has gentle motion");View art=(View)homeMascot[0];ViewGroup perch=(ViewGroup)art.getParent();ok(art.getBackground()==null,"mascot has no opaque paper tile background");ok(perch.getClass().getSimpleName().equals("EchoPerchLayout")&&perch.getChildAt(0)==art,"head uses real card depth layout");ok(perch.getChildAt(1).getTop()>art.getTop()&&perch.getChildAt(1).getTop()<art.getBottom(),"card edge overlaps lower portion of mascot");});
        ui(()->invoke("navigate",new Class[]{String.class},"game"));
        ui(()->{ok(!((View)homeMascot[0]).isAttachedToWindow()&&field(homeMascot[0],"animator")==null,"leaving page stops detached mascot animation");View resting=activity.getWindow().getDecorView().findViewWithTag("echo:1");ok(resting!=null&&!resting.isShown()&&field(resting,"animator")==null,"hidden pause mascot consumes no animation");});
        final int[] chosen={-1};
        ui(()->{chosen[0]=empty();select(chosen[0]);click("候选数");ok(((boolean[])field(game(),"candidates"))[chosen[0]],"candidate reveal button");click("候选数");ok(!((boolean[])field(game(),"candidates"))[chosen[0]],"candidate hide button");click("笔记");click("输入数字 1");ok(integer("mistakes")==0&&values("board")[chosen[0]]==0,"pencil input does not count as error");click("笔记");click("输入数字 "+values("solution")[chosen[0]]);ok(values("board")[chosen[0]]==values("solution")[chosen[0]],"keypad inserts solution");click("撤销");ok(values("board")[chosen[0]]==0,"undo control");});
        SystemClock.sleep(1300);
        final long[] timing={0};ui(()->{click("暂停游戏");timing[0]=(Long)field(game(),"elapsedMs");ok((Boolean)field(activity,"paused"),"pause control");});SystemClock.sleep(1200);
        ui(()->{ok((Long)field(game(),"elapsedMs")==timing[0],"paused timer stable");click("继续游戏");});SystemClock.sleep(1200);
        ui(()->{ok((Long)field(game(),"elapsedMs")>timing[0]+800,"resume timer advances");callActivityOnPause(activity);timing[0]=(Long)field(game(),"elapsedMs");});SystemClock.sleep(1100);ui(()->{ok((Long)field(game(),"elapsedMs")==timing[0],"background lifecycle stops timer");callActivityOnResume(activity);});
        ui(()->{for(int i=0;i<3;i++){select(empty());useHint();}ok(integer("hints")==3,"three hints via UI");int at=empty();select(at);useHint();ok(integer("hints")==3&&values("board")[at]==0,"fourth hint blocked via UI");
            int wrong=values("solution")[at]%9+1;for(int i=0;i<3;i++)click("输入数字 "+wrong);ok(integer("mistakes")==3&&status().equals("lost"),"third mistake ends actual activity game");
        });
        SystemClock.sleep(1100);
        ui(()->{ok(status().equals("lost"),"terminal game remains lost");});
        // Relaunch dismisses the result dialog; home then begins a fresh independently tracked game.
        ui(()->activity.finish());activity=startActivitySync(launch);waitForIdleSync();
        ui(()->{invoke("start",new Class[]{int.class,boolean.class},1,true);ok(!((String)field(game(),"dailyDate")).isEmpty(),"daily challenge recorded");ok(integer("mistakes")==0&&integer("hints")==0,"new session resets budgets");});
        final String[] dailyBoard={null};ui(()->{dailyBoard[0]=Arrays.toString(values("givens"));invoke("start",new Class[]{int.class,boolean.class},1,true);ok(dailyBoard[0].equals(Arrays.toString(values("givens"))),"daily puzzle deterministic");
            int[] b=values("board"),s=values("solution");for(int at=0;at<81;at++)if(b[at]==0){select(at);click("输入数字 "+s[at]);}
            ok(status().equals("won"),"whole game completed via actual keypad");Object store=field(activity,"store");ok((Integer)store.getClass().getMethod("wins").invoke(store)>=1,"victory updates local statistics");
            Object board=field(activity,"boardView");Object timeline=field(board,"timeline");ok((Integer)timeline.getClass().getMethod("size",long.class).invoke(timeline,SystemClock.uptimeMillis())>=4,"final cell triggers row column box and digit effects");
        });SystemClock.sleep(1200);
        ui(()->activity.finish());activity=startActivitySync(launch);waitForIdleSync();
        ui(()->{ok(status().equals("won"),"terminal state persists across recreation");invoke("start",new Class[]{int.class,boolean.class},2,false);select(empty());click("候选数");useHint();});
        SystemClock.sleep(1100);final String[] saved={null};ui(()->{invoke("navigate",new Class[]{String.class},"home");saved[0]=Arrays.toString(values("board"));timing[0]=(Long)field(game(),"elapsedMs");activity.finish();});
        activity=startActivitySync(launch);waitForIdleSync();ui(()->{ok(saved[0].equals(Arrays.toString(values("board")))&&integer("hints")==1,"board and hint budget persist across activity recreation");ok((Long)field(game(),"elapsedMs")==timing[0],"home does not add playtime");click("个人中心");ok(findText(activity.getWindow().getDecorView(),"我的专注")!=null,"profile statistics view opens");});
        ui(()->{invoke("navigate",new Class[]{String.class},"game");invoke("start",new Class[]{int.class,boolean.class},0,false);click("快速模式");ok((Boolean)field(game(),"quickMode"),"quick mode switch");
            int at=empty(),n=values("solution")[at];int[] before=values("board").clone();click("输入数字 "+n);
            ok(Arrays.equals(before,values("board"))&&integer("mistakes")==0&&integer("hints")==0,"quick keypad only chooses number");
            ok(integer("activeNumber")==n&&((Integer)game().getClass().getMethod("highlightNumber").invoke(game()))==n,"quick highlight independent of selected cell");
            ok(find(activity.getWindow().getDecorView(),"输入数字 "+n).isSelected(),"active keypad number visibly selected");
            select(at);ok(values("board")[at]==n,"quick blank tap fills chosen number");
            int second=-1;for(int i=0;i<81;i++)if(values("board")[i]==0&&values("solution")[i]==n){second=i;break;}
            ok(second>=0,"fixture has repeated quick number");select(second);ok(values("board")[second]==n&&integer("activeNumber")==((Integer)game().getClass().getMethod("remaining",int.class).invoke(game(),n)>0?n:0),"consecutive quick fill retains selection");
            click("输入数字 "+(n%9+1));select(at);ok(values("board")[at]==n&&integer("mistakes")==0,"quick occupied tap does not overwrite or count error");
            click("输入数字 "+(n%9+1));ok(integer("activeNumber")==0,"tapping active number cancels selection");
            select(empty());click("候选数");ok(((boolean[])field(game(),"candidates"))[integer("selected")],"quick mode can still select for candidates");useHint();ok(integer("hints")==1,"compact hint tool remains functional");
            click("笔记");click("输入数字 2");at=empty();select(at);ok(values("board")[at]==0&&(values("notes")[at]&2)!=0&&integer("mistakes")==0,"quick pencil mode writes note safely");click("笔记");
            int wrong=values("solution")[at]%9+1;if(integer("activeNumber")!=wrong)click("输入数字 "+wrong);select(at);ok(integer("mistakes")==1,"quick wrong tap counts exactly once");
            click("暂停游戏");before=values("board").clone();int oldNumber=integer("activeNumber");click("输入数字 9");select(at);ok(Arrays.equals(before,values("board"))&&integer("activeNumber")==oldNumber&&integer("mistakes")==1,"pause blocks quick number and board inputs");click("继续游戏");
            click("常规模式");ok(!(Boolean)field(game(),"quickMode")&&integer("activeNumber")==0,"classic switch clears quick selection");select(at);click("输入数字 "+values("solution")[at]);ok(values("board")[at]==values("solution")[at],"classic interaction restored");
            click("快速模式");click("输入数字 5");invoke("navigate",new Class[]{String.class},"home");activity.finish();
        });
        activity=startActivitySync(launch);waitForIdleSync();ui(()->{
            ok((Boolean)field(game(),"quickMode")&&integer("activeNumber")==5,"quick mode and chosen number persist across recreation");
            invoke("navigate",new Class[]{String.class},"game");ok(find(activity.getWindow().getDecorView(),"快速模式").isSelected(),"restored quick mode reflected in switch");
            View hint=find(activity.getWindow().getDecorView(),"使用提示");ok(hint.getWidth()<activity.getWindow().getDecorView().getWidth()/4,"hint button is compact toolbar item");
            ok(find(activity.getWindow().getDecorView(),"数独")!=null&&findText(activity.getWindow().getDecorView(),"EchoSudoku")!=null,"Sudoku-focused branding visible in game");
            invoke("start",new Class[]{int.class,boolean.class},0,false);ok((Boolean)field(game(),"quickMode")&&integer("activeNumber")==0,"new game remembers quick preference without stale chosen number");
            int at=empty(),wrong=values("solution")[at]%9+1;click("输入数字 "+wrong);select(at);select(at);select(at);ok(integer("mistakes")==3&&status().equals("lost"),"quick UI third mistake ends game");
        });SystemClock.sleep(1100);
        ui(()->activity.finish());activity=startActivitySync(launch);waitForIdleSync();
        final int[] completed={0,0};final Object[] earlyEvent={null};
        ui(()->{
            invoke("start",new Class[]{int.class,boolean.class},0,false);invoke("setQuickMode",new Class[]{boolean.class},false);int n=values("solution")[empty()];int last=-1;
            for(int i=0;i<81;i++)if(values("board")[i]==0&&values("solution")[i]==n){select(i);click("输入数字 "+n);last=i;}
            completed[0]=n;completed[1]=last;
            click("快速模式");View key=find(activity.getWindow().getDecorView(),"输入数字 "+n+"，已完成");ok(key!=null&&!key.isEnabled(),"completed number key disabled in quick mode");key.performClick();ok(integer("activeNumber")==0,"disabled quick key cannot select via callback");
            select(last);ok(integer("selected")==last,"completed board number remains clickable");click("撤销");ok(find(activity.getWindow().getDecorView(),"输入数字 "+n).isEnabled(),"undo re-enables depleted quick key");
            click("输入数字 "+n);select(last);ok(integer("activeNumber")==0&&!find(activity.getWindow().getDecorView(),"输入数字 "+n+"，已完成").isEnabled(),"ninth occurrence forcibly clears selected number and disables button");
            Object timeline=field(field(activity,"boardView"),"timeline");List<?> events=(List<?>)timeline.getClass().getMethod("active",long.class).invoke(timeline,SystemClock.uptimeMillis());Object digit=null;for(Object e:events)if((Integer)field(e,"kind")==4)digit=e;ok(digit!=null,"ninth occurrence emits digit-completion animation");earlyEvent[0]=digit;
            click("常规模式");int blank=empty();select(blank);click("输入数字 "+values("solution")[blank]);events=(List<?>)timeline.getClass().getMethod("active",long.class).invoke(timeline,SystemClock.uptimeMillis());ok(events.contains(earlyEvent[0]),"next correct move does not interrupt digit animation");
            blank=empty();select(blank);click("输入数字 "+(values("solution")[blank]%9+1));events=(List<?>)timeline.getClass().getMethod("active",long.class).invoke(timeline,SystemClock.uptimeMillis());ok(events.contains(earlyEvent[0]),"wrong feedback does not interrupt completion animation");
        });
        ui(()->{String board=Arrays.toString(values("board"));int hints=integer("hints"),errors=integer("mistakes");
            for(int id=0;id<5;id++){invoke("changeTheme",new Class[]{int.class},id);Object palette=field(activity,"palette");ok((Integer)field(palette,"id")==id,"palette switched "+id);ok(field(field(activity,"boardView"),"theme")==palette,"board uses current palette "+id);ok(Arrays.toString(values("board")).equals(board)&&integer("hints")==hints&&integer("mistakes")==errors,"theme switch preserves game "+id);}
            invoke("changeTheme",new Class[]{int.class},1);ok((activity.getWindow().getDecorView().getSystemUiVisibility()&View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)==0,"dark palette uses light status icons");
            invoke("navigate",new Class[]{String.class},"profile");ok((Integer)field(field(activity,"palette"),"id")==1,"profile inherits dark palette");
            activity.finish();
        });
        activity=startActivitySync(launch);waitForIdleSync();ui(()->{ok((Integer)field(field(activity,"store"),"themeId")==1&&((Boolean)field(field(activity,"palette"),"dark")),"dark theme survives activity recreation");invoke("navigate",new Class[]{String.class},"game");});
        ui(()->invoke("navigate",new Class[]{String.class},"profile"));
        ui(()->{click("完成动效");Object art=activity.getWindow().getDecorView().findViewWithTag("echo:3");ok(field(art,"animator")==null,"actual settings switch immediately stops existing mascot");click("完成动效");ok(field(art,"animator")!=null,"actual settings switch restores existing mascot motion");});
        ui(()->{Object store=field(activity,"store");store.getClass().getField("motion").setBoolean(store,false);invoke("navigate",new Class[]{String.class},"home");});
        ui(()->{Object art=activity.getWindow().getDecorView().findViewWithTag("echo:0");ok(field(art,"animator")==null,"motion preference disables mascot movement");invoke("start",new Class[]{int.class,boolean.class},0,false);invoke("setQuickMode",new Class[]{boolean.class},false);int at=empty();select(at);click("输入数字 "+values("solution")[at]);Object timeline=field(field(activity,"boardView"),"timeline");ok((Integer)timeline.getClass().getMethod("size",long.class).invoke(timeline,SystemClock.uptimeMillis())==0,"motion off creates no completion animations");Object store=field(activity,"store");store.getClass().getField("motion").setBoolean(store,true);invoke("navigate",new Class[]{String.class},"game");});
        ui(()->click("暂停游戏"));
        ui(()->{View resting=activity.getWindow().getDecorView().findViewWithTag("echo:1");ok(resting.isShown()&&field(resting,"animator")!=null,"pausing reveals breathing rest pose");click("继续游戏");});
        ui(()->{Object resting=activity.getWindow().getDecorView().findViewWithTag("echo:1");ok(field(resting,"animator")==null,"resume stops hidden rest animation");});
        result.putString("stream","\n"+log+"PASS "+checks+" device checks\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){result.putString("stream","\n"+log+"FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
