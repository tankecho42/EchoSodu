package com.tankecho.zensudoku;

import android.animation.ValueAnimator;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import java.util.Random;

/** Contextual crayon illustration, animated only while actually visible. */
public final class EchoArtView extends ImageView {
    public static final int PEEK=0,REST=1,CELEBRATE=2,PROFILE=3,THINK=4,RETRY=5,DAILY=6;
    private final int pose;private boolean motion;
    private ValueAnimator animator;
    private ValueAnimator blinkAnimator;
    private Bitmap blinkEyes;private final Paint eyePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Path eyeMask=new Path();private final RectF imageRect=new RectF();
    private final Random random=new Random();private float blinkAmount;
    private boolean aggregatedVisible,windowVisible=true,blinkScheduled;
    private long nextBlinkAt;private final Runnable blinkTask=()->{blinkScheduled=false;nextBlinkAt=0;startBlink();};
    public EchoArtView(Context context,int pose,boolean motion){
        super(context);this.pose=pose;this.motion=motion;
        int[] assets={R.drawable.echo_peek,R.drawable.echo_rest,R.drawable.echo_celebrate,R.drawable.echo_profile,R.drawable.echo_hint,R.drawable.echo_retry,R.drawable.echo_daily};
        if(pose<0||pose>=assets.length)throw new IllegalArgumentException("Unknown Echo pose");
        setImageResource(assets[pose]);
        setScaleType(ScaleType.FIT_CENTER);setTag("echo:"+pose);
        String[] descriptions={"探头扶着卡片","披着斗篷休息","举爪庆祝完成","翻看专注手账","拿着铅笔思考","伸出爪子鼓励再试一次","举着每日挑战日历"};
        setContentDescription("Echo 白熊，"+descriptions[pose]);
        if(pose==PEEK){
            blinkEyes=BitmapFactory.decodeResource(getResources(),R.drawable.echo_blink_eye_source);
            // Only these eye regions are replaced. The original crayon silhouette never shifts.
            float w=getDrawable().getIntrinsicWidth(),h=getDrawable().getIntrinsicHeight();
            eyeMask.addOval(new RectF(w*.340f,h*.316f,w*.432f,h*.398f),Path.Direction.CW);
            eyeMask.addOval(new RectF(w*.551f,h*.357f,w*.649f,h*.443f),Path.Direction.CW);
            imageRect.set(0,0,w,h);
        }
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);setPivotX(w*.5f);setPivotY(h*(pose==PEEK?.73f:.8f));}
    public void setMotionEnabled(boolean enabled){motion=enabled;if(!enabled)stopMotion();else startMotion();}
    @Override public void onVisibilityAggregated(boolean visible){super.onVisibilityAggregated(visible);aggregatedVisible=visible;if(visible)startMotion();else stopMotion();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);windowVisible=visibility==View.VISIBLE;if(windowVisible)startMotion();else stopMotion();}
    @Override protected void onDetachedFromWindow(){aggregatedVisible=false;stopMotion();super.onDetachedFromWindow();}
    private boolean canAnimate(){return motion&&aggregatedVisible&&windowVisible&&isAttachedToWindow()&&isShown()&&ValueAnimator.areAnimatorsEnabled();}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(pose!=PEEK||blinkAmount<=0||blinkEyes==null)return;
        canvas.save();canvas.translate(getPaddingLeft(),getPaddingTop());canvas.concat(getImageMatrix());canvas.clipPath(eyeMask);
        eyePaint.setAlpha(Math.round(255*blinkAmount));canvas.drawBitmap(blinkEyes,null,imageRect,eyePaint);canvas.restore();
    }
    private void scheduleBlink(boolean first){
        if(pose!=PEEK||!canAnimate()||blinkScheduled||blinkAnimator!=null)return;
        long delay=first?2800+random.nextInt(1000):3800+random.nextInt(3400);
        blinkScheduled=true;nextBlinkAt=android.os.SystemClock.uptimeMillis()+delay;postDelayed(blinkTask,delay);
    }
    private void startBlink(){
        if(pose!=PEEK||!canAnimate()||blinkAnimator!=null)return;
        removeCallbacks(blinkTask);blinkScheduled=false;nextBlinkAt=0;
        ValueAnimator blink=ValueAnimator.ofFloat(0,1);blinkAnimator=blink;blink.setDuration(280);blink.setInterpolator(new LinearInterpolator());
        blink.addUpdateListener(a->{if(!canAnimate()){stopMotion();return;}float elapsed=(Float)a.getAnimatedValue()*280;
            blinkAmount=elapsed<65?smooth(elapsed/65):elapsed<160?1:1-smooth((elapsed-160)/120);invalidate();});
        blink.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator ended){if(blinkAnimator!=ended)return;blinkAnimator=null;blinkAmount=0;invalidate();scheduleBlink(false);}});blink.start();
    }
    private static float smooth(float v){v=Math.max(0,Math.min(1,v));return v*v*(3-2*v);}
    private void startMotion(){
        if(!canAnimate())return;
        scheduleBlink(true);if(animator!=null)return;
        animator=ValueAnimator.ofFloat(0,1);animator.setDuration(pose==PEEK||pose==PROFILE||pose==THINK?3600:pose==REST?2800:2400);animator.setRepeatCount(ValueAnimator.INFINITE);animator.setRepeatMode(ValueAnimator.REVERSE);animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(a->{if(!canAnimate()){stopMotion();return;}float v=(float)a.getAnimatedValue(),density=getResources().getDisplayMetrics().density;
            if(pose==REST){setScaleX(1+v*.012f);setScaleY(1+v*.012f);}else if(pose==PEEK||pose==PROFILE){setRotation((v-.5f)*.8f);}else{setTranslationY(-v*density*2);setRotation((v-.5f)*.8f);}});animator.start();
    }
    private void stopMotion(){
        removeCallbacks(blinkTask);blinkScheduled=false;nextBlinkAt=0;
        ValueAnimator old=blinkAnimator;blinkAnimator=null;if(old!=null)old.cancel();blinkAmount=0;
        if(animator!=null){ValueAnimator previous=animator;animator=null;previous.cancel();}
        setScaleX(1);setScaleY(1);setTranslationY(0);setRotation(0);invalidate();
    }
}
