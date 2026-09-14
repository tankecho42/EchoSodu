package com.tankecho.zensudoku;

import android.graphics.*;

/** A visible solar corona outside the hardware, drawn inside the art view's padded bounds. */
final class MedalRadiance {
    private final int rank,rayColor,coreColor;
    private final Paint paint=new Paint(3);private final Path beam=new Path();
    private final Shader corona,rays;private final RectF ring=new RectF();
    MedalRadiance(MedalStyle style,Palette palette){
        rank=style.rank;
        rayColor=palette.dark?(rank==3?0xffffce68:style.light):(rank==3?0xffd28a21:blend(style.base,style.metal,.48f));
        coreColor=palette.dark?0xfffff2bb:(rank==3?0xffffd46a:style.metal);
        corona=new RadialGradient(56,52,66,new int[]{alpha(rayColor,0),alpha(rayColor,0),alpha(rayColor,rank==3?190:155),alpha(rayColor,55),alpha(rayColor,0)},new float[]{0,.57f,.72f,.87f,1},Shader.TileMode.CLAMP);
        rays=new RadialGradient(56,52,66,new int[]{alpha(rayColor,0),alpha(rayColor,220),alpha(rayColor,200),alpha(rayColor,65),alpha(rayColor,0)},new float[]{0,.64f,.77f,.96f,1},Shader.TileMode.CLAMP);
    }
    private static int alpha(int c,int a){return (c&0xffffff)|(a<<24);}
    private static int blend(int a,int b,float t){return Color.rgb((int)(Color.red(a)*(1-t)+Color.red(b)*t),(int)(Color.green(a)*(1-t)+Color.green(b)*t),(int)(Color.blue(a)*(1-t)+Color.blue(b)*t));}
    void draw(Canvas canvas,float phase,boolean animated,boolean detail){
        if(rank<2)return;float breath=animated?(float)Math.sin(phase*Math.PI*2):0;float intensity=animated?.84f+.16f*breath:.48f;
        paint.setStyle(Paint.Style.FILL);paint.setShader(corona);paint.setAlpha((int)(255*intensity));canvas.drawCircle(56,52,66,paint);paint.setShader(null);
        int count=rank==3?24:16;float rotation=animated?phase*(720f/count):0;
        // Long/short rays travel slowly; their lengths and opacity never collapse to invisible.
        for(int i=0;i<count;i++){
            float pulse=animated?(float)Math.sin(phase*Math.PI*2+i*Math.PI):0;
            float end=(i%2==0?64:57)+(rank==3?1:0)+pulse*.8f;
            float width=rank==3?2.8f:2.1f;int saved=canvas.save();canvas.rotate(i*360f/count+rotation,56,52);
            beam.reset();beam.moveTo(56-width,52-43);beam.lineTo(56-width*.68f,52-52);beam.lineTo(56,52-end);beam.lineTo(56+width*.68f,52-52);beam.lineTo(56+width,52-43);beam.close();
            paint.setStyle(Paint.Style.FILL);paint.setShader(rays);paint.setAlpha((int)(255*intensity));canvas.drawPath(beam,paint);paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(i%2==0?(rank==3?1.25f:1):.75f);paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(alpha(coreColor,(int)((i%2==0?215:155)*intensity)));canvas.drawLine(56,52-46,56,52-end+2.3f,paint);
            canvas.restoreToCount(saved);
        }
        if(rank==3){
            // A second fan of slender rays breaks up the silhouette into a sunburst.
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(.7f);paint.setColor(alpha(rayColor,(int)(145*intensity)));
            for(int i=0;i<24;i++){int saved=canvas.save();canvas.rotate(i*15+7.5f-rotation,56,52);canvas.drawLine(56,3,56,-8-(i%3)*2,paint);canvas.restoreToCount(saved);}
        }
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(rank==3?1.6f:1.1f);paint.setColor(alpha(coreColor,(int)(180*intensity)));
        ring.set(7,3,105,101);canvas.drawArc(ring,animated?phase*360:15,rank==3?130:75,false,paint);canvas.drawArc(ring,animated?phase*360+180:195,rank==3?130:75,false,paint);
        paint.setShader(null);paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);
    }
}
