import com.tankecho.zensudoku.VictoryMotion;
public final class VictoryMotionTest {
    static int checks;static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
    public static void main(String[] args){
        for(int cell=0;cell<81;cell++){
            float min=0,max=0;boolean bounded=true,continuous=true;float previous=0;
            for(long t=0;t<=VictoryMotion.DURATION_MS;t+=4){float v=VictoryMotion.wave(cell,t);min=Math.min(min,v);max=Math.max(max,v);bounded&=Float.isFinite(v)&&Math.abs(v)<1;continuous&=Math.abs(v-previous)<.05;previous=v;}
            check(max>.5f&&min<-.5f,"every cell rises and falls, including givens: "+cell);
            check(bounded&&continuous,"bounded smooth trajectory: "+cell);
            check(VictoryMotion.wave(cell,-1)==0&&VictoryMotion.wave(cell,VictoryMotion.DURATION_MS)==0,"each tile rests before and after victory: "+cell);
        }
        check(VictoryMotion.wave(0,450)>.5f&&VictoryMotion.wave(80,450)==0,"wave travels rather than bouncing all cells together");
        check(VictoryMotion.wave(80,1490)>.5f,"wave reaches final diagonal");
        check(VictoryMotion.wave(0,1750)>.2f,"lighter second crest follows");
        check(VictoryMotion.RESULT_DELAY_MS>VictoryMotion.DURATION_MS,"settlement follows all wave motion");
        check(VictoryMotion.presence(0)==0&&VictoryMotion.presence(VictoryMotion.DURATION_MS)==0,"grid emphasis has no start/end jump");
        System.out.println("PASS "+checks+" whole-board wave checks");
    }
}
