package com.tankecho.zensudoku;
import java.util.*;

/** Independent event clocks. Adding a new effect never restarts or removes an older one. */
public final class EffectTimeline {
    public static final int CELL=0,ROW=1,COLUMN=2,BOX=3,DIGIT=4,WRONG=5;
    public static final class Event {
        public final int kind,index,number;public final int[] cells;
        public final long start,duration;
        Event(int kind,int index,int number,int[] cells,long start,long duration){this.kind=kind;this.index=index;this.number=number;this.cells=cells.clone();this.start=start;this.duration=duration;}
        public float progress(long now){return Math.max(0,Math.min(1,(now-start)/(float)duration));}
    }
    private final List<Event> events=new ArrayList<>();
    public void add(int kind,int index,int number,int[] cells,long now){prune(now);events.add(new Event(kind,index,number,cells,now,kind==CELL?680:kind==WRONG?650:RegionMotion.DURATION_MS));}
    public List<Event> active(long now){prune(now);return new ArrayList<>(events);}
    public void prune(long now){Iterator<Event> it=events.iterator();while(it.hasNext()){Event e=it.next();if(now-e.start>=e.duration)it.remove();}}
    public int size(long now){prune(now);return events.size();}
    public void clearWrong(int cell){events.removeIf(e->e.kind==WRONG&&e.index==cell);}
    public void clear(){events.clear();}
}
