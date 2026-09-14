package com.tankecho.zensudoku;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.view.animation.LinearInterpolator;

/** Layered native enamel, individual crests and progressively richer living finishes. */
final class MedalView extends View {
    private static final Bitmap[] FACES=new Bitmap[4];
    private static final int[] RES={R.drawable.medal_journey,R.drawable.medal_insight,R.drawable.medal_rhythm,R.drawable.medal_focus};
    private final Medal medal;final MedalStyle style;private final Palette palette;
    private final Paint p=new Paint(3);private final Path path=new Path();private final Rect visible=new Rect();
    private final RectF oval=new RectF(),face=new RectF(21,17,91,87);private final Matrix matrix=new Matrix();
    private final Shader enamel,rim,shine;private final MedalRadiance radiance;private final ColorMatrixColorFilter muted;
    private ValueAnimator animator;private boolean motionEnabled,detail;private float phase;
    private final ViewTreeObserver.OnScrollChangedListener scrollListener=this::updateMotion;
    private final ViewTreeObserver.OnGlobalLayoutListener layoutListener=this::updateMotion;
    MedalView(Context c,Medal medal,Palette palette){this(c,medal,palette,false,false);}
    MedalView(Context c,Medal medal,Palette palette,boolean motion,boolean detail){super(c);this.medal=medal;this.palette=palette;this.style=MedalStyle.of(medal);this.motionEnabled=motion;this.detail=detail;
        if(FACES[medal.family]==null){BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=2;FACES[medal.family]=BitmapFactory.decodeResource(getResources(),RES[medal.family],o);}
        ColorMatrix gray=new ColorMatrix();gray.setSaturation(.13f);muted=new ColorMatrixColorFilter(gray);
        enamel=new LinearGradient(12,0,98,112,new int[]{style.light,style.base,style.base},null,Shader.TileMode.CLAMP);
        rim=new SweepGradient(56,52,new int[]{style.metal,0xfffcf4da,style.metal,style.base,style.light,style.metal},null);
        radiance=new MedalRadiance(style,palette);
        shine=new LinearGradient(-11,0,11,0,new int[]{0x00ffffff,0x75ffffff,0x00ffffff},null,Shader.TileMode.CLAMP);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setTag("medal-art:"+medal.id);
    }
    void setMotionEnabled(boolean enabled){motionEnabled=enabled;updateMotion();}
    private boolean shouldAnimate(){return medal!=null&&motionEnabled&&medal.unlocked()&&ValueAnimator.areAnimatorsEnabled()&&isAttachedToWindow()&&getWindowVisibility()==VISIBLE&&isShown()&&hasWindowFocus()&&getGlobalVisibleRect(visible)&&visible.width()>0&&visible.height()>0;}
    private void updateMotion(){
        if(!shouldAnimate()){stopMotion();return;}if(animator!=null)return;
        animator=ValueAnimator.ofFloat(0,1);animator.setDuration(detail?5600:7600);animator.setRepeatCount(ValueAnimator.INFINITE);animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a->{if(!shouldAnimate()){stopMotion();return;}phase=(Float)a.getAnimatedValue();invalidate();});animator.start();
    }
    private void stopMotion(){if(animator!=null){ValueAnimator old=animator;animator=null;old.cancel();old.removeAllUpdateListeners();}phase=0;invalidate();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();getViewTreeObserver().addOnScrollChangedListener(scrollListener);getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);post(this::updateMotion);}
    @Override protected void onDetachedFromWindow(){if(getViewTreeObserver().isAlive()){getViewTreeObserver().removeOnScrollChangedListener(scrollListener);getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);}stopMotion();super.onDetachedFromWindow();}
    @Override protected void onVisibilityChanged(View changed,int visibility){super.onVisibilityChanged(changed,visibility);if(medal!=null)updateMotion();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(medal!=null)updateMotion();}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);updateMotion();}
    private static int alpha(int color,int a){return (color&0xffffff)|(Math.max(0,Math.min(255,a))<<24);}
    private void fill(int color){p.setShader(null);p.setColorFilter(null);p.setStyle(Paint.Style.FILL);p.setColor(color);}
    private void stroke(int color,float width){fill(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);}
    private void line(Canvas c,float x,float y,float ex,float ey){c.drawLine(x,y,ex,ey,p);}
    private void diamond(Canvas c,float x,float y,float rx,float ry){path.reset();path.moveTo(x,y-ry);path.lineTo(x+rx,y);path.lineTo(x,y+ry);path.lineTo(x-rx,y);path.close();c.drawPath(path,p);}
    private void star(Canvas c,float x,float y,float radius){path.reset();for(int i=0;i<8;i++){double a=i*Math.PI/4;float r=i%2==0?radius:radius*.25f;float sx=x+(float)Math.cos(a)*r,sy=y+(float)Math.sin(a)*r;if(i==0)path.moveTo(sx,sy);else path.lineTo(sx,sy);}path.close();c.drawPath(path,p);}
    private void arc(Canvas c,float radius,float from,float sweep){oval.set(56-radius,52-radius,56+radius,52+radius);c.drawArc(oval,from,sweep,false,p);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float scale=Math.min(getWidth()/136f,getHeight()/152f);int saved=c.save();c.translate((getWidth()-136*scale)/2,(getHeight()-152*scale)/2);c.scale(scale,scale);c.translate(12,20);
        boolean unlocked=medal.unlocked();int layer=-1;if(!unlocked){p.reset();p.setAntiAlias(true);p.setColorFilter(muted);p.setAlpha(175);layer=c.saveLayer(-12,-20,124,132,p);p.setColorFilter(null);p.setAlpha(255);}
        float wave=animator==null?0:(float)Math.sin(phase*Math.PI*2);int rank=style.rank;
        if(unlocked&&rank>=2)radiance.draw(c,phase,animator!=null,detail);
        // Ribbon silhouettes and folds differ in each grade, keeping their identity when motion is off.
        for(int side=-1;side<=1;side+=2){fill(style.base);path.reset();path.moveTo(56+side*6,81);path.lineTo(56+side*(26+rank*2),82);path.lineTo(56+side*(31+rank*3),119);path.lineTo(56+side*22,112);path.lineTo(56+side*13,125);path.close();c.drawPath(path,p);
            stroke(alpha(style.light,150),rank>=2?1.5f:.7f);line(c,56+side*23,93,56+side*(29+rank*2),112);if(rank==3){line(c,56+side*18,96,56+side*21,114);}}
        // Tiered hardware: engraved bronze, star-cut silver, laurel gold, a jeweled coronet.
        fill(palette.dark?0x60000000:0x160f2030);c.drawCircle(56,55,46,p);
        if(rank>=1){fill(style.metal);for(int i=0;i<(rank==3?16:12);i++){double a=i*Math.PI*2/(rank==3?16:12);float x=56+(float)Math.cos(a)*45,y=52+(float)Math.sin(a)*45;diamond(c,x,y,rank==3?4:2,rank==3?4:2);}}
        matrix.reset();if(rank>=2&&animator!=null)matrix.setRotate(phase*(rank==3?360:180),56,52);rim.setLocalMatrix(matrix);fill(-1);p.setShader(rim);c.drawCircle(56,52,44,p);p.setShader(null);
        stroke(alpha(0xffffffff,190),.8f);arc(c,42.7f,0,360);stroke(style.base,.6f);arc(c,40.7f,0,360);
        fill(-1);p.setShader(enamel);c.drawCircle(56,52,40,p);p.setShader(null);
        drawPattern(c);
        // Art remains original crayon Echo; colored enamel, engraving and symbolic seals carry identity.
        int clip=c.save();path.reset();path.addCircle(56,52,34,Path.Direction.CW);c.clipPath(path);fill(-1);c.drawBitmap(FACES[medal.family],null,face,p);fill(alpha(style.base,38));c.drawCircle(56,52,35,p);c.restoreToCount(clip);
        stroke(alpha(style.light,210),.8f);arc(c,34.8f,0,360);
        if(rank>=2)drawLaurel(c,rank==3);
        if(rank==3)drawCrown(c);
        // A unique semantic crest, large enough to read in the collection's compact cards.
        fill(style.metal);c.drawCircle(56,86,14,p);fill(style.base);c.drawCircle(56,86,12,p);stroke(style.light,1.4f);drawSymbol(c,style.motif,56,85,9);
        fill(style.base);c.drawRoundRect(39,102,73,115,5,5,p);stroke(alpha(style.metal,200),.7f);c.drawRoundRect(39,102,73,115,5,5,p);
        fill(style.light);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(8);c.drawText(style.mark,56,111.5f,p);
        if(unlocked&&animator!=null)drawFinish(c,rank,wave);
        if(layer!=-1)c.restoreToCount(layer);c.restoreToCount(saved);
    }
    private void drawPattern(Canvas c){
        int clip=c.save();path.reset();path.addCircle(56,52,39.5f,Path.Direction.CW);c.clipPath(path);stroke(alpha(style.light,100),.55f);
        int type=style.motif%4;
        if(type==0){for(int i=-5;i<=5;i++){line(c,0,i*9,112,i*9+112);line(c,0,i*9+80,112,i*9-32);}}
        else if(type==1){for(int i=1;i<=8;i++){oval.set(56-i*7,52-i*7,56+i*7,52+i*7);c.drawOval(oval,p);}line(c,17,52,95,52);line(c,56,12,56,92);}
        else if(type==2){for(int i=0;i<12;i++){double a=i*Math.PI/6;line(c,56,52,56+50*(float)Math.cos(a),52+50*(float)Math.sin(a));}}
        else {for(int y=18;y<90;y+=10)for(int x=21;x<96;x+=10){fill(alpha(style.light,130));diamond(c,x+(y%20==8?4:0),y,.65f,1.1f);}}
        c.restoreToCount(clip);stroke(alpha(style.light,190),.65f);
        for(int i=0;i<16+style.motif;i++){float angle=360f*i/(16+style.motif);int s=c.save();c.rotate(angle,56,52);line(c,56,13.4f,56,15.3f);c.restoreToCount(s);}
    }
    private void drawLaurel(Canvas c,boolean royal){
        for(int side=-1;side<=1;side+=2){stroke(style.metal,.8f);path.reset();path.moveTo(56+side*24,89);path.quadTo(56+side*58,70,56+side*40,28);c.drawPath(path,p);
            for(int i=0;i<6;i++){float y=35+i*8,x=56+side*(43.5f+(float)Math.sin(i*.56)*3);int save=c.save();c.rotate(side*(22+i*9),x,y);fill(i%2==0?style.light:style.metal);c.drawOval(x-2,y-4,x+2,y+4,p);c.restoreToCount(save);}}
        if(royal){fill(style.metal);diamond(c,9,52,3,5);diamond(c,103,52,3,5);}
    }
    private void drawCrown(Canvas c){fill(style.metal);path.reset();path.moveTo(43,15);path.lineTo(40,5);path.lineTo(49,9);path.lineTo(56,1);path.lineTo(63,9);path.lineTo(72,5);path.lineTo(69,15);path.close();c.drawPath(path,p);fill(style.base);diamond(c,56,10,2,3);stroke(style.light,.8f);line(c,45,17,67,17);}
    private void drawFinish(Canvas c,int rank,float wave){
        // A single slow specular pass for bronze/silver, layered orbital light for the upper ranks.
        float offset=(phase*1.6f-.3f)*150;int saved=c.save();path.reset();path.addCircle(56,52,44,Path.Direction.CW);c.clipPath(path);matrix.reset();matrix.setRotate(-25);matrix.postTranslate(offset,0);shine.setLocalMatrix(matrix);fill(-1);p.setShader(shine);p.setAlpha(detail?135:85);c.drawRect(8,7,104,98,p);p.setShader(null);c.restoreToCount(saved);
        if(rank>=1){stroke(alpha(style.light,rank==1?130:200),rank==1?1:1.5f);arc(c,43,phase*360-90,rank==1?24:48);}
        if(rank>=2){stroke(alpha(style.light,(int)(90+40*wave)),.8f);arc(c,50,-phase*270+60,54);arc(c,50,-phase*270+240,54);
            int count=rank==3?6:3;for(int i=0;i<count;i++){double angle=i*Math.PI*2/count+phase*(rank==3?.7:.3);float r=rank==3?60:56;float pulse=.7f+.3f*(float)Math.pow(Math.sin(phase*Math.PI*2+i*1.7),2);float x=56+(float)Math.cos(angle)*r,y=52+(float)Math.sin(angle)*r,size=(rank==3?4.7f:3.5f)*pulse;fill(alpha(palette.dark?style.light:style.base,(int)(pulse*220)));star(c,x,y,size+1);fill(alpha(0xffffecb2,255));star(c,x,y,size*.74f);}}
        if(rank==3){stroke(alpha(style.light,120),.7f);arc(c,47,phase*180+25,80);arc(c,47,phase*180+205,80);fill(alpha(0xfffff4c8,(int)(170+65*wave)));star(c,56,4,4f+wave*.7f);}
    }
    private void drawSymbol(Canvas c,int type,float x,float y,float r){
        int saved=c.save();c.translate(x,y);c.scale(r/9,r/9);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.35f);
        switch(type){
        case 0: diamond(c,0,0,5,8);line(c,0,-8,0,8);line(c,-5,0,5,0);break;
        case 1: path.reset();path.moveTo(-7,5);path.cubicTo(7,8,-8,-5,5,-5);c.drawPath(path,p);c.drawCircle(-7,5,1.5f,p);star(c,5,-5,3);break;
        case 2: path.reset();path.moveTo(-9,6);path.lineTo(-3,-6);path.lineTo(2,2);path.lineTo(5,-4);path.lineTo(9,6);path.close();c.drawPath(path,p);line(c,-5,-2,-1,-2);break;
        case 3: path.reset();path.moveTo(-7,6);path.lineTo(-9,-4);path.lineTo(-3,0);path.lineTo(0,-8);path.lineTo(3,0);path.lineTo(9,-4);path.lineTo(7,6);path.close();c.drawPath(path,p);line(c,-6,8,6,8);break;
        case 4: path.reset();path.moveTo(-6,8);path.quadTo(-3,-9,7,-8);path.quadTo(9,5,-6,8);c.drawPath(path,p);line(c,-7,9,5,-6);line(c,-2,1,4,1);break;
        case 5: path.reset();path.moveTo(-8,-3);path.lineTo(-4,-7);path.lineTo(4,-7);path.lineTo(8,-3);path.lineTo(0,8);path.close();c.drawPath(path,p);line(c,-8,-3,8,-3);line(c,-3,-3,0,8);line(c,3,-3,0,8);break;
        case 6: path.reset();path.moveTo(-9,7);path.lineTo(0,-7);path.lineTo(9,7);path.close();c.drawPath(path,p);line(c,-3,-2,0,0);line(c,0,0,3,-2);line(c,0,-7,0,-10);line(c,0,-10,5,-8);break;
        case 7: diamond(c,0,0,8,8);diamond(c,0,0,3,3);for(int i=0;i<4;i++){c.rotate(90);line(c,0,-4,0,-9);}break;
        case 8: line(c,0,7,0,-2);for(int i=0;i<3;i++){int a=c.save();c.rotate(i*120,0,-2);c.drawOval(-2,-9,2,-2,p);c.restoreToCount(a);}break;
        case 9: c.drawCircle(0,0,5,p);for(int i=0;i<7;i++){int a=c.save();c.rotate(i*360f/7);c.drawOval(-1.5f,-9,1.5f,-5,p);c.restoreToCount(a);}break;
        case 10: path.reset();path.moveTo(4,-8);path.cubicTo(-10,-8,-10,10,5,7);path.cubicTo(-3,4,-4,-3,4,-8);c.drawPath(path,p);star(c,5,-1,2);break;
        case 11: c.drawRoundRect(-7,-6,7,8,2,2,p);line(c,-7,-2,7,-2);line(c,-3,-9,-3,-4);line(c,3,-9,3,-4);star(c,0,3,3);break;
        case 12: line(c,0,8,0,-2);path.reset();path.moveTo(0,1);path.quadTo(-10,1,-7,-6);path.quadTo(0,-6,0,1);path.moveTo(0,-2);path.quadTo(1,-10,8,-7);path.quadTo(8,0,0,-2);c.drawPath(path,p);break;
        case 13: for(int i=0;i<5;i++){int a=c.save();c.rotate(i*72);c.drawOval(-2.5f,-8,2.5f,-1,p);c.restoreToCount(a);}c.drawCircle(0,0,2,p);break;
        case 14: line(c,-6,-8,6,-8);line(c,-6,8,6,8);path.reset();path.moveTo(-5,-7);path.cubicTo(-5,-1,5,1,5,7);path.lineTo(-5,7);path.cubicTo(-5,1,5,-1,5,-7);path.close();c.drawPath(path,p);break;
        case 15: c.drawCircle(0,0,7,p);line(c,0,0,0,-4);line(c,0,0,4,2);for(int i=0;i<8;i++){int a=c.save();c.rotate(i*45);line(c,0,-8,0,-9);c.restoreToCount(a);}star(c,6,-6,2);break;
        }
        c.restoreToCount(saved);p.setStyle(Paint.Style.FILL);
    }
}
