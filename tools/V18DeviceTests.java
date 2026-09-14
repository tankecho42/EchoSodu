package com.tankecho.zensudoku.tests;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.graphics.*;import java.util.*;import java.time.*;

/** Real release input and rendering; all fixtures live in a separate test APK. */
public class V18DeviceTests extends VictoryDeviceTests {
    int[] answer(){int[] a=new int[81];for(int r=0;r<9;r++)for(int c=0;c<9;c++)a[r*9+c]=(r*3+r/3+c)%9+1;return a;}
    boolean target(int kind,int at,int[] s){return kind==1?at/9==4:kind==2?at%9==4:kind==3?at/27==1&&at%9/3==1:s[at]==s[40];}
    void region(int kind,boolean quick,boolean motion,int theme)throws Exception{
        dismiss();AlertDialog hint=(AlertDialog)field(activity,"hintDialog");if(hint!=null)hint.dismiss();
        set(field(activity,"store"),"motion",motion);int[] s=answer(),puzzle=new int[81];
        for(int i=0;i<81;i++){boolean chosen=kind==5?(target(1,i,s)||target(2,i,s)||target(3,i,s)||target(4,i,s)):target(kind,i,s);if(chosen||i%3==0)puzzle[i]=s[i];}puzzle[40]=0;
        Object g=Class.forName("com.tankecho.zensudoku.Game").getConstructor(int[].class,int[].class,int.class).newInstance(puzzle,s,0);set(g,"quickMode",quick);set(g,"elapsedMs",4*60000L+37000);set(activity,"game",g);set(activity,"paused",false);invoke("changeTheme",new Class[]{int.class},theme);invoke("navigate",new Class[]{String.class},"game");
    }
    void place()throws Exception{int n=values("solution")[40];if((Boolean)field(game(),"quickMode")){click("输入数字 "+n);select(40);}else{select(40);click("输入数字 "+n);}}
    List<?> events()throws Exception{Object t=field(field(activity,"boardView"),"timeline");return (List<?>)t.getClass().getMethod("active",long.class).invoke(t,SystemClock.uptimeMillis());}
    View pose(View root,int which){return root.findViewWithTag("echo:"+which);}
    void assertPose(View root,int which)throws Exception{View art=pose(root,which);ok(art!=null&&art.isShown(),"visible contextual pose "+which);ok((Integer)field(art,"pose")==which,"context has independent art binding "+which);}
    @Override public void onStart(){Bundle out=new Bundle();try{
        activity=startActivitySync(new Intent().setClassName("com.tankecho.zensudoku","com.tankecho.zensudoku.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        for(int kind=1;kind<=5;kind++){final int k=kind;final Object[] original={null};final long[] start={0};final int[][] bounds={null};
            ui(()->region(k,k==4,true,k%2));SystemClock.sleep(180);
            ui(()->{ViewGroup b=(ViewGroup)field(activity,"boardView");bounds[0]=new int[]{b.getChildAt(40).getLeft(),b.getChildAt(40).getTop()};place();List<?> es=events();int mask=0;for(Object e:es){int type=(Integer)field(e,"kind");if(type>=1&&type<=4){mask|=1<<type;if(original[0]==null){original[0]=e;start[0]=(Long)field(e,"start");}}}
                ok(k==5?(mask&30)==30:(mask&(1<<k))!=0,"actual input emits correct regional events "+k);ok(status().equals("playing"),"partial region does not trigger whole-board win "+k);
                if(k==4){ok(integer("activeNumber")==0,"ninth quick digit force-deselects");View key=find(activity.getWindow().getDecorView(),"输入数字 "+values("solution")[40]+"，已完成");ok(key!=null&&!key.isEnabled(),"finished quick digit key disabled");select(40);ok(integer("selected")==40,"finished digit cells still selectable");}
            });SystemClock.sleep(410);
            ui(()->{Object b=field(activity,"boardView");float[] samples=(float[])field(b,"cellWaves");int moved=0;boolean[] affected=new boolean[81];for(Object e:events())if((Integer)field(e,"kind")>=1&&(Integer)field(e,"kind")<=4)for(int cell:(int[])field(e,"cells"))affected[cell]=true;
                for(int i=0;i<81;i++){if(samples[i]!=0)moved++;ok(affected[i]||samples[i]==0,"nonmember remains stationary "+k+":"+i);ok(Math.abs(samples[i])<1.25,"intersection height bounded "+k+":"+i);}ok(moved>0,"actual rendered cells float "+k);
                View cell=((ViewGroup)b).getChildAt(40);ok(cell.getLeft()==bounds[0][0]&&cell.getTop()==bounds[0][1],"input geometry unchanged during floating "+k);
                invoke("setQuickMode",new Class[]{boolean.class},false);int blank=empty();select(blank);click("输入数字 "+values("solution")[blank]);ok(events().contains(original[0])&&(Long)field(original[0],"start")==start[0],"next move preserves regional clock "+k);
            });SystemClock.sleep(2450);ui(()->{float[] wave=(float[])field(field(activity,"boardView"),"cellWaves");for(float v:wave)ok(v==0,"settled cell returns to exact origin");ok(events().isEmpty(),"expired events stop drawing");});
        }
        ui(()->{region(1,false,true,0);select(40);click("使用提示");assertPose(((AlertDialog)field(activity,"hintDialog")).getWindow().getDecorView(),4);});SystemClock.sleep(300);
        ui(()->((AlertDialog)field(activity,"hintDialog")).getButton(AlertDialog.BUTTON_POSITIVE).performClick());SystemClock.sleep(120);
        ui(()->{ok(integer("hints")==1&&values("board")[40]>0,"hint fill charged once");ok(!events().isEmpty(),"hint fill emits regional wave");});SystemClock.sleep(400);
        ui(()->{float[] wave=(float[])field(field(activity,"boardView"),"cellWaves");boolean moved=false;for(float v:wave)moved|=v!=0;ok(moved,"hint regional wave renders after dialog closes");click("暂停游戏");ok(events().isEmpty(),"pause clears active regional clocks");assertPose(activity.getWindow().getDecorView(),1);});SystemClock.sleep(300);
        ui(()->{click("继续游戏");ok(events().isEmpty(),"resume does not replay regional wave");region(2,false,false,0);place();ok(events().isEmpty(),"motion preference disables regional effects");region(2,false,true,0);place();callActivityOnPause(activity);ok(events().isEmpty(),"background cancels regional work");callActivityOnResume(activity);});
        ui(()->{invoke("navigate",new Class[]{String.class},"home");assertPose(activity.getWindow().getDecorView(),0);invoke("showDailyCalendar",new Class[]{YearMonth.class},YearMonth.now());assertPose(((AlertDialog)field(activity,"calendarDialog")).getWindow().getDecorView(),6);((AlertDialog)field(activity,"calendarDialog")).dismiss();invoke("navigate",new Class[]{String.class},"profile");});SystemClock.sleep(300);
        ui(()->{assertPose(activity.getWindow().getDecorView(),3);ok(pose(activity.getWindow().getDecorView(),0)==null,"profile no longer duplicates home art");
            for(String name:new String[]{"echo_profile","echo_hint","echo_retry","echo_daily"}){int id=activity.getResources().getIdentifier(name,"drawable",activity.getPackageName());Bitmap b=BitmapFactory.decodeResource(activity.getResources(),id);ok(b.hasAlpha()&&Color.alpha(b.getPixel(0,0))==0,"real transparent PNG "+name);ok(Color.alpha(b.getPixel(b.getWidth()/2,b.getHeight()/2))>=250,"opaque foreground fur "+name);b.recycle();}
            region(1,false,true,0);select(40);for(int i=0;i<3;i++)click("输入数字 "+(values("solution")[40]%9+1));});SystemClock.sleep(900);
        ui(()->{assertPose(((AlertDialog)field(activity,"settlementDialog")).getWindow().getDecorView(),5);stage(false,true,0);finishCell();});SystemClock.sleep(4100);
        ui(()->{assertPose(((AlertDialog)field(activity,"settlementDialog")).getWindow().getDecorView(),2);activity.finish();});
        out.putString("stream",log+"PASS "+checks+" contextual art and regional wave device checks\n");finish(Activity.RESULT_OK,out);
    }catch(Throwable e){out.putString("stream",log+"FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,out);}}
}
