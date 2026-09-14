package com.tankecho.zensudoku.tests;
import android.app.*;import android.content.*;import android.os.*;import android.graphics.*;import android.view.*;import java.lang.reflect.*;

/** Checks actual mascot compositing and timer cleanup, outside the release APK. */
public class BlinkDeviceTests extends DemoV14 {
    void set(Object o,String name,Object value)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
    void call(Object o,String method,Class<?>[] types,Object...args)throws Exception{Method m=o.getClass().getDeclaredMethod(method,types);m.setAccessible(true);m.invoke(o,args);}
    View art(){View root=activity.getWindow().getDecorView();View peek=root.findViewWithTag("echo:0");return peek!=null?peek:root.findViewWithTag("echo:3");}
    void motion(View view,boolean enabled)throws Exception{call(view,"setMotionEnabled",new Class[]{boolean.class},enabled);}
    void geometry()throws Exception{
        View a=art();ViewGroup perch=(ViewGroup)a.getParent();View card=perch.getChildAt(1);
        ok(a.getTop()<0,"perched art is lifted above its former origin");ok(Math.abs(a.getTop()+a.getHeight()*.12f)<=2,"lift follows mascot size on both pages");
        ok(Math.abs(card.getTop()-a.getHeight()*.73f)<=2,"card edge stays in its original layout position");
        ok(a.getBottom()-card.getTop()<a.getHeight()*.17f&&a.getBottom()>card.getTop(),"paws overlap only a small strip of the card");
        ok(!((ViewGroup)perch.getParent()).getClipChildren(),"raised ears are not clipped by parent");
    }
    void compareEyes()throws Exception{
        View a=art();motion(a,false);int w=a.getWidth(),h=a.getHeight();Bitmap open=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888),closed=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        a.draw(new Canvas(open));set(a,"blinkAmount",1f);a.draw(new Canvas(closed));int changed=0,outside=0,alphaOutside=0;
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            boolean eye=(x>=w*.340f-1&&x<=w*.432f+1&&y>=h*.316f-1&&y<=h*.398f+1)||(x>=w*.551f-1&&x<=w*.649f+1&&y>=h*.357f-1&&y<=h*.443f+1);
            int before=open.getPixel(x,y),after=closed.getPixel(x,y);if(before!=after){changed++;if(!eye)outside++;}if(!eye&&Color.alpha(before)!=Color.alpha(after))alphaOutside++;
        }
        ok(changed>10,"closed-eye rendering visibly changes both eye regions");ok(outside==0,"blink leaves head, nose, paws, cape and silhouette pixel-identical");ok(alphaOutside==0&&Color.alpha(closed.getPixel(0,0))==0,"original transparency is preserved; no generated backdrop leaks");
        set(a,"blinkAmount",0f);a.invalidate();open.recycle();closed.recycle();motion(a,true);
    }
    @Override public void onStart(){Bundle result=new Bundle();try{
        activity=startActivitySync(new Intent().setClassName("com.tankecho.zensudoku","com.tankecho.zensudoku.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        ui(()->{Object store=field(activity,"store");set(store,"motion",true);invoke("navigate",new Class[]{String.class},"home");});SystemClock.sleep(400);
        final View[] previous={null};ui(()->{geometry();compareEyes();View a=art();ok((Boolean)field(a,"blinkScheduled"),"visible peeking Echo schedules a blink");call(a,"startBlink",new Class[]{});ok(field(a,"blinkAnimator")!=null,"blink animator starts");});
        SystemClock.sleep(105);ui(()->ok((Float)field(art(),"blinkAmount")>.95f,"brief fully closed phase is reached"));SystemClock.sleep(260);
        ui(()->{View a=art();ok((Float)field(a,"blinkAmount")==0&&field(a,"blinkAnimator")==null,"blink returns to open eyes");long remaining=(Long)field(a,"nextBlinkAt")-SystemClock.uptimeMillis();ok((Boolean)field(a,"blinkScheduled")&&remaining>3400&&remaining<=7200,"next blink uses a relaxed variable interval");
            call(a,"startBlink",new Class[]{});motion(a,false);ok(field(a,"blinkAnimator")==null&&!(Boolean)field(a,"blinkScheduled")&&(Float)field(a,"blinkAmount")==0,"motion switch cancels active blink and pending timer immediately");motion(a,true);
            a.onVisibilityAggregated(false);ok(field(a,"animator")==null&&field(a,"blinkAnimator")==null&&!(Boolean)field(a,"blinkScheduled"),"hidden mascot stops idle motion and blink work");a.onVisibilityAggregated(true);ok((Boolean)field(a,"blinkScheduled"),"shown mascot resumes scheduling");
            call(a,"onWindowVisibilityChanged",new Class[]{int.class},View.INVISIBLE);ok(!(Boolean)field(a,"blinkScheduled")&&field(a,"blinkAnimator")==null,"background window cancels blink work");call(a,"onWindowVisibilityChanged",new Class[]{int.class},View.VISIBLE);ok((Boolean)field(a,"blinkScheduled"),"visible window restores blink scheduling");
            callActivityOnPause(activity);ok(field(a,"animator")==null&&!(Boolean)field(a,"blinkScheduled"),"activity pause immediately clears idle and blink work");callActivityOnResume(activity);ok((Boolean)field(a,"blinkScheduled"),"activity resume restores configured motion");
            previous[0]=a;invoke("navigate",new Class[]{String.class},"profile");invoke("changeTheme",new Class[]{int.class},1);});SystemClock.sleep(400);
        ui(()->{ok(!previous[0].isAttachedToWindow()&&field(previous[0],"blinkAnimator")==null&&!(Boolean)field(previous[0],"blinkScheduled"),"leaving page removes old view timers");geometry();ok((Integer)field(art(),"pose")==3,"profile uses its own notebook artwork");ok(field(art(),"blinkEyes")==null,"profile expression does not reuse peeking eye coordinates");
            for(int pose:new int[]{1,2}){View v=(View)art().getClass().getConstructor(Context.class,int.class,boolean.class).newInstance(activity,pose,true);ok(field(v,"blinkEyes")==null&&!(Boolean)field(v,"blinkScheduled"),"already closed-eye pose keeps its expression "+pose);}
            previous[0]=art();activity.finish();});SystemClock.sleep(250);
        ui(()->ok(field(previous[0],"blinkAnimator")==null&&!(Boolean)field(previous[0],"blinkScheduled"),"activity destruction leaves no blink callback"));
        result.putString("stream",log+"PASS "+checks+" blink and perch checks\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){result.putString("stream",log+"FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
