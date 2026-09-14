import com.tankecho.zensudoku.*;
import java.util.*;

/** Traveling motion, membership and concurrent clock contracts; no Android dependency. */
public final class RegionMotionTest {
    static int checks;
    static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    static int[] targets(int kind){int[] cells=new int[9];for(int i=0;i<9;i++)cells[i]=kind==1?36+i:kind==2?4+i*9:kind==3?30+i/3*9+i%3:(i*30+i/3)%81;return cells;}
    public static void main(String[] args){
        for(int kind=1;kind<=4;kind++){
            EffectTimeline timeline=new EffectTimeline();int[] cells=targets(kind);timeline.add(kind,0,1,cells,1000);
            EffectTimeline.Event event=timeline.active(1000).get(0);
            for(int at:cells){float low=0,high=0;long first=-1;
                check(RegionMotion.wave(event,at,900)==0,"future event is still");
                check(RegionMotion.wave(event,at,1000)==0,"starts at rest");
                for(long now=1000;now<3300;now+=10){float w=RegionMotion.wave(event,at,now);low=Math.min(low,w);high=Math.max(high,w);if(w!=0&&first<0)first=now;}
                check(high>.7f&&low<-.7f,"every target rises and falls, kind "+kind+" cell "+at);
                check(RegionMotion.wave(event,at,3299)==0&&RegionMotion.wave(event,at,3300)==0,"finishes at rest without jump");
            }
            for(int at=0;at<81;at++){boolean target=false;for(int n:cells)target|=n==at;if(!target)for(long now=1000;now<3300;now+=100)check(RegionMotion.wave(event,at,now)==0,"unrelated cells stay still");}
            check(RegionMotion.wave(event,cells[0],1250)!=RegionMotion.wave(event,cells[8],1250),"wave travels instead of synchronized bobbing");
            float[] sampled=new float[81];RegionMotion.sample(timeline.active(1350),1350,sampled);
            check(sampled[cells[0]]!=0&&sampled[cells[8]]==0,"first crest precedes last target");
        }
        EffectTimeline timeline=new EffectTimeline();timeline.add(1,4,0,targets(1),1000);EffectTimeline.Event first=timeline.active(1000).get(0);
        float before=RegionMotion.wave(first,36,1400);
        timeline.add(2,13,0,targets(2),1200);timeline.add(3,22,0,targets(3),1250);timeline.add(4,27,1,targets(4),1300);
        timeline.add(0,36,1,new int[]{36},1350);timeline.add(5,80,2,new int[]{80},1400);
        check(timeline.active(1400).get(0)==first&&RegionMotion.wave(first,36,1400)==before,"new feedback never restarts regional clock");
        float[] samples=new float[81];
        for(long now=1400;now<3700;now+=10){RegionMotion.sample(timeline.active(now),now,samples);for(float v:samples)check(Float.isFinite(v)&&Math.abs(v)<1.25f,"intersections remain bounded");}
        check(timeline.size(3700)==0,"all independent clocks retire");
        RegionMotion.sample(Collections.emptyList(),4000,samples);for(float v:samples)check(v==0,"no residual displacement");
        System.out.println("PASS "+checks+" regional wave checks");
    }
}
