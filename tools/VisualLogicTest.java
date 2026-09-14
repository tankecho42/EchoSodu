import com.tankecho.zensudoku.*;
import java.util.*;
public class VisualLogicTest {
    static int checks;static void check(boolean v,String name){checks++;if(!v)throw new AssertionError(name);}
    static double luminance(int c){double[] v={(c>>16&255)/255.,(c>>8&255)/255.,(c&255)/255.};for(int i=0;i<3;i++)v[i]=v[i]<=.04045?v[i]/12.92:Math.pow((v[i]+.055)/1.055,2.4);return .2126*v[0]+.7152*v[1]+.0722*v[2];}
    static double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
    public static void main(String[] args){
        EffectTimeline t=new EffectTimeline();int[] cells={0,1,2,3,4,5,6,7,8};
        t.add(EffectTimeline.ROW,0,0,cells,100);Object first=t.active(100).get(0);
        t.add(EffectTimeline.COLUMN,9,0,cells,300);t.add(EffectTimeline.BOX,18,0,cells,320);t.add(EffectTimeline.DIGIT,27,1,cells,350);t.add(EffectTimeline.WRONG,60,4,new int[]{60},400);
        check(t.size(500)==5,"row/column/box/digit/error coexist");check(t.active(500).get(0)==first,"new effects keep old event identity");
        check(t.active(500).get(0).start==100,"old event never restarts");check(t.active(500).get(0).progress(500)>t.active(500).get(1).progress(500),"independent progress");
        t.add(EffectTimeline.CELL,70,2,new int[]{70},600);check(t.size(600)==6,"ordinary cell pulse does not cancel celebrations");
        cells[0]=80;check(t.active(700).get(0).cells[0]==0,"event owns immutable target snapshot");
        check(t.size(2400)==3,"only independently expired events removed");check(t.size(2630)==1,"digit effect outlives earlier units");check(t.size(2700)==0,"idle timeline cleans itself");
        for(int i=0;i<5;i++){Palette p=Palette.of(i);check(p.id==i,"palette identity");check(contrast(p.ink,p.surface)>=7,"main text contrast "+i);check(contrast(p.muted,p.bg)>=4.5,"secondary text contrast "+i);check(contrast(p.onAccent,p.accent)>=4.5,"selected button contrast "+i);check(contrast(p.accent,p.pale)>=4.5,"button text contrast "+i);check(contrast(p.ink,p.match)>=4.5,"highlighted number contrast "+i);}
        System.out.println("PASS "+checks+" parallel timeline and palette contrast checks");
    }
}
