package com.tankecho.zensudoku;

import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.animation.ValueAnimator;
import java.util.function.BooleanSupplier;
import java.util.*;

/** Short native feedback; never consumes clicks or moves hit bounds. */
final class UiMotion {
    private static final Map<View,Integer> OWNED=new WeakHashMap<>();
    static boolean enabled(boolean preference){return preference&&ValueAnimator.areAnimatorsEnabled();}
    static void reset(View v){Integer flags=OWNED.get(v);if(flags==null)return;v.animate().cancel();if((flags&1)!=0){v.setScaleX(1);v.setScaleY(1);}if((flags&2)!=0){v.setAlpha(1);v.setTranslationY(0);}}
    static void enter(View v,boolean preference){if(!enabled(preference))return;OWNED.put(v,OWNED.getOrDefault(v,0)|2);v.setAlpha(0);v.setTranslationY(9*v.getResources().getDisplayMetrics().density);v.animate().alpha(1).translationY(0).setDuration(260).setInterpolator(new DecelerateInterpolator(1.6f)).start();}
    static void press(View v,BooleanSupplier preference){
        OWNED.put(v,OWNED.getOrDefault(v,0)|1);
        v.setOnTouchListener((view,e)->{if(!view.isEnabled()||!enabled(preference.getAsBoolean()))return false;
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN)view.animate().scaleX(.98f).scaleY(.98f).setDuration(90).start();
            else if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL)view.animate().scaleX(1).scaleY(1).setDuration(170).setInterpolator(new DecelerateInterpolator()).start();return false;});
        v.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){}public void onViewDetachedFromWindow(View v){reset(v);}});
    }
    static void clearTree(View v){if(v==null)return;reset(v);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)clearTree(((ViewGroup)v).getChildAt(i));}
}
