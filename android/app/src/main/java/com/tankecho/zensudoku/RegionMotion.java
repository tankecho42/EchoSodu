package com.tankecho.zensudoku;

import java.util.Arrays;
import java.util.List;

/** Independent traveling crests for completed regions. Positions are visual only. */
public final class RegionMotion {
    public static final long DURATION_MS=2300;
    private RegionMotion(){}
    private static float crest(float t){return t<=0||t>=1?0:(float)(Math.sin(t*Math.PI*2)*Math.sin(t*Math.PI));}
    public static float wave(EffectTimeline.Event event,int cell,long now){
        if(event.kind<EffectTimeline.ROW||event.kind>EffectTimeline.DIGIT||cell<0||cell>=81)return 0;
        long elapsed=now-event.start;if(elapsed<0||elapsed>=DURATION_MS)return 0;
        int order=-1;for(int i=0;i<event.cells.length;i++)if(event.cells[i]==cell){order=i;break;}
        if(order<0)return 0;
        // Rows travel left-to-right, columns top-to-bottom; boxes spread diagonally.
        // Dispersed occurrences of one digit travel in their stable board order.
        int delay=event.kind==EffectTimeline.BOX?((cell/9)%3+cell%3)*160:order*80;
        return crest((elapsed-80-delay)/900f)+.24f*crest((elapsed-980-delay)/620f);
    }
    public static void sample(List<EffectTimeline.Event> events,long now,float[] output){
        Arrays.fill(output,0);
        for(EffectTimeline.Event event:events){
            if(event.kind<EffectTimeline.ROW||event.kind>EffectTimeline.DIGIT)continue;
            for(int cell:event.cells)if(cell>=0&&cell<output.length)output[cell]+=wave(event,cell,now);
        }
        // Smoothly bound intersections; no hard clipping or replacing an older clock.
        for(int i=0;i<output.length;i++){float value=output[i];output[i]=value/(float)Math.sqrt(1+.65f*value*value);}
    }
}
