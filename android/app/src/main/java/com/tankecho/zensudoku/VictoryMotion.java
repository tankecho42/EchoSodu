package com.tankecho.zensudoku;

/** Shared clock for the board wave, confetti and settlement. No per-cell animators. */
public final class VictoryMotion {
    public static final long DURATION_MS=3600;
    public static final long RESULT_DELAY_MS=DURATION_MS+120;
    private VictoryMotion(){}
    public static float smooth(float x){x=Math.max(0,Math.min(1,x));return x*x*(3-2*x);}
    private static float crest(float t){return t<=0||t>=1?0:(float)(Math.sin(t*Math.PI*2)*Math.sin(t*Math.PI));}
    public static float wave(int cell,long elapsed){
        if(cell<0||cell>=81||elapsed<0||elapsed>=DURATION_MS)return 0;
        float delay=(cell/9+cell%9)*65;
        return crest((elapsed-120-delay)/1050f)+.42f*crest((elapsed-1420-delay)/1050f);
    }
    public static float presence(long elapsed){
        if(elapsed<0||elapsed>=DURATION_MS)return 0;
        return smooth(elapsed/240f)*(1-smooth((elapsed-3100)/500f));
    }
}
