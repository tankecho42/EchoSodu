package com.tankecho.zensudoku;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.*;
import java.util.*;

public final class BoardView extends ViewGroup {
    private final Game game;private final Palette theme;
    private final Paint p=new Paint(3);
    private final Cell[] cells=new Cell[81];
    private final EffectTimeline timeline=new EffectTimeline();
    private List<EffectTimeline.Event> frameEffects=Collections.emptyList();
    private int hintCell=-1;private final boolean[] hintRelated=new boolean[81];
    public void showHint(int cell,int[] related){hintCell=cell;Arrays.fill(hintRelated,false);for(int i:related)hintRelated[i]=true;refresh();}
    public void clearHint(){hintCell=-1;Arrays.fill(hintRelated,false);refresh();}
    private final float[] cellWaves=new float[81];private final int[] drawOrder=new int[81];
    private long frameTime,victoryStart=-1;private long victoryElapsed=-1;
    public interface Select {void onSelect(int index);}
    public BoardView(Context context,Game g,Palette palette,Select select){
        super(context);game=g;theme=palette;setWillNotDraw(false);setClipChildren(false);setClipToPadding(false);setChildrenDrawingOrderEnabled(true);for(int i=0;i<81;i++)drawOrder[i]=i;
        setContentDescription("数独棋盘，9行9列");
        for(int i=0;i<81;i++){final int at=i;Cell cell=new Cell(context,i);cells[i]=cell;cell.setFocusable(true);cell.setClickable(true);cell.setOnClickListener(v->{select.onSelect(at);refresh();});addView(cell);}
        refresh();
    }
    public void refresh(){
        for(int i=0;i<81;i++){
            String d="第"+(i/9+1)+"行第"+(i%9+1)+"列，"+(game.board[i]==0?"空格":game.board[i]+(game.givens[i]>0?"，题目数字":""));
            if(game.showCandidates(i)){d+="，候选数";int m=Game.mask(game.board,i);for(int n=1;n<=9;n++)if((m&(1<<(n-1)))!=0)d+=" "+n;}
            cells[i].setContentDescription(d);cells[i].setSelected(i==game.selected);cells[i].invalidate();
        }invalidate();
    }
    public void celebrate(boolean[] before,boolean motion){
        if(!motion||!android.animation.ValueAnimator.areAnimatorsEnabled()){stopEffects();return;}
        long now=SystemClock.uptimeMillis();
        timeline.clearWrong(game.selected);
        timeline.add(EffectTimeline.CELL,game.selected,game.board[game.selected],new int[]{game.selected},now);
        boolean[] after=game.completedUnits();
        for(int unit=0;unit<after.length;unit++)if(!before[unit]&&after[unit]){
            int kind=unit<9?EffectTimeline.ROW:unit<18?EffectTimeline.COLUMN:unit<27?EffectTimeline.BOX:EffectTimeline.DIGIT;
            int[] targets=new int[9];int k=0;
            for(int i=0;i<81;i++)if(unit<9?i/9==unit:unit<18?i%9==unit-9:unit<27?i/27*3+(i%9)/3==unit-18:game.board[i]==unit-26)targets[k++]=i;
            timeline.add(kind,unit,unit>=27?unit-26:0,targets,now);
        }
        refresh();
    }
    public void wrong(int n,boolean motion){if(!motion||!android.animation.ValueAnimator.areAnimatorsEnabled()){refresh();return;}timeline.add(EffectTimeline.WRONG,game.selected,n,new int[]{game.selected},SystemClock.uptimeMillis());refresh();}
    public void startVictory(long now){if(game.status.equals("won")&&victoryStart<0){victoryStart=now;refresh();}}
    public void stopEffects(){timeline.clear();frameEffects=Collections.emptyList();victoryStart=-1;victoryElapsed=-1;Arrays.fill(cellWaves,0);refresh();}
    public boolean isVictoryRunning(){return victoryStart>=0&&SystemClock.uptimeMillis()-victoryStart<VictoryMotion.DURATION_MS;}
    @Override public void onVisibilityAggregated(boolean visible){super.onVisibilityAggregated(visible);if(!visible)stopEffects();}
    @Override protected int getChildDrawingOrder(int count,int position){return drawOrder[position];}
    @Override protected void onDetachedFromWindow(){stopEffects();super.onDetachedFromWindow();}
    @Override protected void onMeasure(int w,int h){int size=MeasureSpec.getSize(w);setMeasuredDimension(size,size);for(Cell c:cells)c.measure(MeasureSpec.makeMeasureSpec((size+8)/9,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec((size+8)/9,MeasureSpec.EXACTLY));}
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){float s=getWidth()/9f;for(int i=0;i<81;i++){int x=Math.round(i%9*s),y=Math.round(i/9*s);cells[i].layout(x,y,Math.round((i%9+1)*s),Math.round((i/9+1)*s));}}
    @Override protected void dispatchDraw(Canvas c){
        boolean hadEffects=!frameEffects.isEmpty()||victoryElapsed>=0;frameTime=SystemClock.uptimeMillis();frameEffects=timeline.active(frameTime);
        victoryElapsed=victoryStart<0?-1:frameTime-victoryStart;
        if(victoryElapsed>=VictoryMotion.DURATION_MS){victoryStart=-1;victoryElapsed=-1;}
        if(!android.animation.ValueAnimator.areAnimatorsEnabled()){timeline.clear();frameEffects=Collections.emptyList();victoryStart=-1;victoryElapsed=-1;}
        RegionMotion.sample(frameEffects,frameTime,cellWaves);
        if(victoryElapsed>=0)for(int i=0;i<81;i++)cellWaves[i]=VictoryMotion.wave(i,victoryElapsed);
        for(int i=0;i<81;i++)drawOrder[i]=i;
        // Lifted faces are drawn last so adjacent resting cells cannot cut off their edges.
        for(int i=1;i<81;i++){int value=drawOrder[i],j=i-1;while(j>=0&&Math.abs(cellWaves[drawOrder[j]])>Math.abs(cellWaves[value])){drawOrder[j+1]=drawOrder[j];j--;}drawOrder[j+1]=value;}
        reset();p.setColor(theme.line);c.drawRect(0,0,getWidth(),getHeight(),p);
        for(Cell cell:cells)if(hadEffects||!frameEffects.isEmpty()||victoryElapsed>=0)cell.invalidate();
        super.dispatchDraw(c);float s=getWidth()/9f;
        reset();p.setStyle(Paint.Style.STROKE);
        for(int i=0;i<=9;i++)for(int segment=0;segment<9;segment++){
            float v=Math.min(getWidth()-dp(.8f),Math.max(dp(.8f),i*s));p.setStrokeWidth(i%3==0?dp(1.4f):dp(.6f));
            float vertical=Math.max(i>0?Math.abs(cellWaves[segment*9+i-1]):0,i<9?Math.abs(cellWaves[segment*9+i]):0);
            float horizontal=Math.max(i>0?Math.abs(cellWaves[(i-1)*9+segment]):0,i<9?Math.abs(cellWaves[i*9+segment]):0);
            float presence=VictoryMotion.presence(victoryElapsed);
            p.setColor(alpha(i%3==0?theme.grid:theme.line,(int)(255*(1-Math.max(.82f*presence,.94f*vertical)))));c.drawLine(v,segment*s,v,(segment+1)*s,p);
            p.setColor(alpha(i%3==0?theme.grid:theme.line,(int)(255*(1-Math.max(.82f*presence,.94f*horizontal)))));c.drawLine(segment*s,v,(segment+1)*s,v,p);
        }
        c.save();c.clipRect(0,0,getWidth(),getHeight());
        for(EffectTimeline.Event e:frameEffects){float phase=e.progress(frameTime);if(e.kind==EffectTimeline.ROW||e.kind==EffectTimeline.COLUMN)drawBeam(c,e,phase,s);else if(e.kind==EffectTimeline.BOX)drawBox(c,e,phase,s);else if(e.kind==EffectTimeline.DIGIT)drawDigit(c,e,phase,s);}
        c.restore();reset();
        if(!frameEffects.isEmpty()||victoryElapsed>=0)postInvalidateOnAnimation();
    }
    @Override protected boolean drawChild(Canvas canvas,View child,long drawingTime){
        Cell cell=(Cell)child;float wave=cellWaves[cell.index],amount=Math.abs(wave);
        if(amount<.0001f)return super.drawChild(canvas,child,drawingTime);
        float cx=(child.getLeft()+child.getRight())/2f,cy=(child.getTop()+child.getBottom())/2f;
        reset();p.setColor(alpha(theme.ink,(int)(26*amount)));canvas.drawRoundRect(child.getLeft()+dp(2),child.getTop()+dp(3),child.getRight()-dp(2),child.getBottom()+dp(3),dp(3),dp(3),p);
        canvas.save();canvas.translate(0,-dp(12)*wave);canvas.scale(1-.045f*amount,1-.08f*amount,cx,cy);
        boolean result=super.drawChild(canvas,child,drawingTime);canvas.restore();return result;
    }
    private void reset(){p.setShader(null);p.setPathEffect(null);p.setAlpha(255);p.setStyle(Paint.Style.FILL);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);}
    private static int alpha(int color,int a){return (color&0xffffff)|(Math.max(0,Math.min(255,a))<<24);}
    private static float smooth(float x){x=Math.max(0,Math.min(1,x));return x*x*(3-2*x);}
    private static float envelope(float t){return smooth(t/.2f)*(1-smooth((t-.6f)/.4f));}
    private static int mix(int from,int to,float amount){float a=Math.max(0,Math.min(1,amount));return Color.rgb(Math.round(Color.red(from)*(1-a)+Color.red(to)*a),Math.round(Color.green(from)*(1-a)+Color.green(to)*a),Math.round(Color.blue(from)*(1-a)+Color.blue(to)*a));}
    /** A fine reflected edge; the board's numbers always remain unobscured. */
    private void drawBeam(Canvas c,EffectTimeline.Event e,float t,float s){
        boolean vertical=e.kind==EffectTimeline.COLUMN;int index=vertical?e.index-9:e.index;
        float low=index*s+dp(1.8f),high=(index+1)*s-dp(1.8f),length=getWidth(),fade=envelope(t);
        reset();p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(.8f));p.setColor(alpha(theme.accent,(int)(55*fade)));
        RectF area=vertical?new RectF(low,dp(1.8f),high,length-dp(1.8f)):new RectF(dp(1.8f),low,length-dp(1.8f),high);
        c.drawRoundRect(area,dp(3),dp(3),p);
        float head=smooth(t/.82f)*(length+s*2)-s,tail=s*1.7f;
        int[] colors={alpha(theme.accent,0),alpha(theme.accent,(int)(155*fade)),alpha(theme.accent,0)};
        p.setStrokeWidth(dp(1.15f));
        p.setShader(vertical?new LinearGradient(0,head-tail,0,head,colors,new float[]{0,.72f,1},Shader.TileMode.CLAMP):new LinearGradient(head-tail,0,head,0,colors,new float[]{0,.72f,1},Shader.TileMode.CLAMP));
        float start=Math.max(dp(3),head-tail),end=Math.min(length-dp(3),head);
        if(end>start){if(vertical){c.drawLine(low,start,low,end,p);c.drawLine(high,start,high,end,p);}else{c.drawLine(start,low,end,low,p);c.drawLine(start,high,end,high,p);}}
        reset();
    }
    private void drawBox(Canvas c,EffectTimeline.Event e,float t,float s){
        int box=e.index-18;float x=(box%3)*3*s,y=(box/3)*3*s,fade=envelope(t);
        RectF edge=new RectF(x+dp(2),y+dp(2),x+3*s-dp(2),y+3*s-dp(2));
        reset();p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(.85f));p.setColor(alpha(theme.accent,(int)(55*fade)));c.drawRoundRect(edge,dp(4),dp(4),p);
        Path perimeter=new Path();perimeter.addRoundRect(edge,dp(4),dp(4),Path.Direction.CW);
        PathMeasure measure=new PathMeasure(perimeter,false);float length=measure.getLength(),end=smooth(t/.78f)*length;
        Path trace=new Path();measure.getSegment(Math.max(0,end-length*.3f),end,trace,true);
        p.setStrokeWidth(dp(1.25f));p.setColor(alpha(theme.accent,(int)(140*fade)));c.drawPath(trace,p);reset();
    }
    private void drawDigit(Canvas c,EffectTimeline.Event e,float t,float s){
        for(int k=0;k<e.cells.length;k++){
            float local=(t-k*.026f)/.79f;if(local<=0||local>=1)continue;int at=e.cells[k];float x=(at%9+.5f)*s,y=(at/9+.81f)*s,fade=envelope(local),half=s*.13f*smooth(local/.2f);
            reset();p.setStrokeWidth(dp(1.25f));p.setColor(alpha(theme.accent,(int)(145*fade)));c.drawLine(x-half,y,x+half,y,p);
        }reset();
    }
    /** Use the strongest active tint, rather than adding layers at row/column intersections. */
    private float completionWeight(int index){
        float weight=0;
        for(EffectTimeline.Event e:frameEffects){
            if(e.kind<EffectTimeline.ROW||e.kind>EffectTimeline.DIGIT)continue;
            for(int k=0;k<e.cells.length;k++)if(e.cells[k]==index){
                float t=e.progress(frameTime),delay=e.kind==EffectTimeline.DIGIT?k*.026f:e.kind==EffectTimeline.BOX?((index/9)%3+(index%9)%3)*.047f:k*.027f;
                float local=(t-delay)/(1-delay);if(local>0&&local<1)weight=Math.max(weight,envelope(local));break;
            }
        }return weight;
    }
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
    private final class Cell extends View{
        final int index;Cell(Context c,int i){super(c);index=i;}
        @Override protected void onDraw(Canvas canvas){
            reset();float w=getWidth(),h=getHeight();int sel=game.selected;int background=theme.surface;
            if(victoryElapsed<0){if(sel>=0&&Game.peer(sel,index))background=theme.peer;
            if(game.highlightNumber()>0&&game.highlightNumber()==game.board[index])background=theme.match;
            if(game.board[index]==0&&game.highlightNumber()>0&&(Game.mask(game.board,index)&(1<<(game.highlightNumber()-1)))!=0)background=theme.pale;
            if(hintRelated[index])background=theme.peer;
            if(index==sel||index==hintCell)background=theme.selected;}
            float wave=Math.abs(cellWaves[index]);
            float completion=Math.max(completionWeight(index),wave);p.setColor(mix(background,theme.accent,completion*(theme.dark?.19f:.11f)));canvas.drawRect(0,0,w,h,p);
            if(wave>0){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(.7f));p.setColor(alpha(theme.accent,(int)(105*wave)));canvas.drawRect(dp(.5f),dp(.5f),w-dp(.5f),h-dp(.5f),p);p.setStyle(Paint.Style.FILL);}
            float pulse=1,badPhase=1;int badDigit=0;
            for(EffectTimeline.Event e:frameEffects)if(e.index==index){if(e.kind==EffectTimeline.CELL)pulse=e.progress(frameTime);else if(e.kind==EffectTimeline.WRONG){badPhase=e.progress(frameTime);badDigit=e.number;}}
            if(pulse<1){p.setColor(alpha(theme.accent,(int)(22*(1-smooth(pulse)))));canvas.drawRect(0,0,w,h,p);}
            if(badPhase<1){p.setColor(theme.errorBg);canvas.drawRect(0,0,w,h,p);}
            if((index==sel||index==hintCell)&&victoryElapsed<0){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1.6f));p.setColor(theme.accent);canvas.drawRect(dp(1),dp(1),w-dp(1),h-dp(1),p);p.setStyle(Paint.Style.FILL);}
            int val=badPhase<.85f?badDigit:game.board[index];
            if(val>0){
                p.setColor(badPhase<.85f?theme.error:game.givens[index]!=0?theme.ink:theme.accent);
                p.setTypeface(Typeface.create(game.givens[index]!=0?"sans-serif-medium":"sans-serif",Typeface.NORMAL));
                p.setTextSize(w*.53f*(pulse<1?1+.035f*(float)Math.sin(pulse*Math.PI):1));p.setTextAlign(Paint.Align.CENTER);
                float shake=badPhase<1?(float)Math.sin(badPhase*35)*dp(3)*(1-badPhase):0;canvas.drawText(""+val,w/2+shake,h/2-(p.ascent()+p.descent())/2,p);
                if(game.hinted[index]){p.setColor(theme.fxAlt);canvas.drawCircle(w-dp(5),dp(5),dp(1.5f),p);}
            }else{
                int mask=game.showCandidates(index)?Game.mask(game.board,index):game.notes[index];p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setTextSize(w*.235f);p.setTextAlign(Paint.Align.CENTER);p.setColor(game.showCandidates(index)?theme.accent:theme.muted);
                for(int n=1;n<=9;n++)if((mask&(1<<(n-1)))!=0){float x=((n-1)%3+.5f)*w/3,y=((n-1)/3+.5f)*h/3;if(n==game.highlightNumber()){p.setColor(theme.match);canvas.drawCircle(x,y,w*.14f,p);}p.setColor(n==game.highlightNumber()?theme.accent:game.showCandidates(index)?theme.accent:theme.muted);canvas.drawText(""+n,x,y-(p.ascent()+p.descent())/2,p);}
            }
        }
        @Override public CharSequence getAccessibilityClassName(){return "android.widget.Button";}
    }
}
