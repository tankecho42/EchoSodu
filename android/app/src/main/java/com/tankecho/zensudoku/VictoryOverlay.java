package com.tankecho.zensudoku;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.*;
import java.util.Random;

/** Theme-colored paper and ribbons drawn across the activity. Never captures input. */
public final class VictoryOverlay extends View {
    private static final int COUNT=88;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path ribbon=new Path();
    private final float[] speed=new float[COUNT],rise=new float[COUNT],delay=new float[COUNT],size=new float[COUNT],spin=new float[COUNT],phase=new float[COUNT];
    private final Palette theme;private final long started;private boolean stopped;
    public VictoryOverlay(Context context,Palette palette,long start){
        super(context);theme=palette;started=start;setTag("victory-confetti");
        setClickable(false);setFocusable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setLayerType(LAYER_TYPE_HARDWARE,null);
        Random random=new Random(8109);
        for(int i=0;i<COUNT;i++){speed[i]=.15f+random.nextFloat()*.37f;rise[i]=.72f+random.nextFloat()*.45f;delay[i]=420+random.nextFloat()*420;size[i]=2.7f+random.nextFloat()*3.6f;spin[i]=(random.nextFloat()-.5f)*430;phase[i]=random.nextFloat()*6.28f;}
    }
    public void stop(){stopped=true;invalidate();}
    @Override protected void onDetachedFromWindow(){stopped=true;super.onDetachedFromWindow();}
    @Override public boolean onTouchEvent(MotionEvent event){return false;}
    @Override protected void onDraw(Canvas canvas){
        long elapsed=SystemClock.uptimeMillis()-started;
        if(stopped||elapsed>=VictoryMotion.DURATION_MS||getWindowVisibility()!=VISIBLE)return;
        float w=getWidth(),h=getHeight(),density=getResources().getDisplayMetrics().density;
        float fade=1-VictoryMotion.smooth((elapsed-2800)/800f);
        for(int i=0;i<COUNT;i++){
            float t=(elapsed-delay[i])/1000f;if(t<=0)continue;
            boolean fromLeft=i%2==0,shower=i>=72;
            float x=shower?(i-71.5f)/16*w+(float)Math.sin(t*1.7f+phase[i])*w*.04f:(fromLeft?-.035f:1.035f)*w+(fromLeft?1:-1)*speed[i]*w*t+(float)Math.sin(t*2+phase[i])*w*.02f;
            float y=shower?-h*.08f+t*h*.32f:(.76f+(i%5)*.045f)*h-rise[i]*h*t+.29f*h*t*t;
            if(x<-30*density||x>w+30*density||y<-30*density||y>h+30*density)continue;
            int color=i%4==0?theme.accent:i%4==1?theme.fx:i%4==2?theme.fxAlt:(theme.dark?0xffe4c78f:0xffbe995e);
            paint.setColor(color);paint.setAlpha((int)(215*fade*VictoryMotion.smooth(t/.16f)));paint.setStyle(Paint.Style.FILL);paint.setStrokeCap(Paint.Cap.ROUND);
            float s=size[i]*density;canvas.save();canvas.translate(x,y);canvas.rotate(phase[i]*57.3f+spin[i]*t);
            if(i%9==0){
                paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(density*1.35f);ribbon.reset();ribbon.moveTo(-s*1.9f,0);ribbon.cubicTo(-s,-s*(float)Math.sin(t*3+phase[i]),s,s,s*1.9f,0);canvas.drawPath(ribbon,paint);
            }else{
                canvas.scale(.22f+.78f*Math.abs((float)Math.cos(t*4+phase[i])),1);canvas.drawRoundRect(-s*.42f,-s*.85f,s*.42f,s*.85f,density*.6f,density*.6f,paint);
            }
            canvas.restore();
        }
        postInvalidateOnAnimation();
    }
}
