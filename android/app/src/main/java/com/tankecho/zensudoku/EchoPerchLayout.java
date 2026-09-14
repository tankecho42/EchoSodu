package com.tankecho.zensudoku;

import android.content.Context;
import android.graphics.Canvas;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

/** The face sits behind a real card; the paws and cape naturally cross its front edge. */
public final class EchoPerchLayout extends FrameLayout {
    private final EchoArtView art;
    private final int edge,lift,pose;
    public EchoPerchLayout(Context context,View card,boolean motion,int sizeDp){
        this(context,card,motion,sizeDp,EchoArtView.PEEK);
    }
    public EchoPerchLayout(Context context,View card,boolean motion,int sizeDp,int pose){
        super(context);this.pose=pose;float density=getResources().getDisplayMetrics().density;
        int size=Math.round(sizeDp*density);edge=Math.round(size*.73f);lift=Math.round(size*.12f);
        setClipChildren(false);setClipToPadding(false);
        art=new EchoArtView(context,pose,motion);
        LayoutParams a=new LayoutParams(size,size,Gravity.TOP|Gravity.RIGHT);a.rightMargin=Math.round(18*density);a.topMargin=-lift;addView(art,a);
        LayoutParams panel=new LayoutParams(-1,-2);panel.topMargin=edge;addView(card,panel);
        setTag("echo-perch");
    }
    @Override protected void dispatchDraw(Canvas canvas){
        super.dispatchDraw(canvas);
        canvas.save();
        // The notebook pose has one hanging paw; keep its waist behind the real panel.
        float frontRight=pose==EchoArtView.PROFILE?art.getLeft()+art.getWidth()*.39f:art.getRight();
        canvas.clipRect(art.getLeft(),edge,frontRight,art.getBottom());
        drawChild(canvas,art,getDrawingTime());canvas.restore();
    }
}
