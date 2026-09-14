package com.tankecho.zensudoku;

import java.time.LocalDate;
import java.util.*;

/** One immutable aggregation shared by the statistics UI and medal rules. */
public final class ProgressStats {
    public static final class Entry {
        public final String id,date,status,daily;
        public final int level,hints,mistakes;
        public final long ms;
        public final boolean practice;
        public Entry(String id,String date,String status,String daily,int level,int hints,int mistakes,long ms,boolean practice){
            this.id=id;this.date=date;this.status=status;this.daily=daily;this.level=level;this.hints=hints;this.mistakes=mistakes;this.ms=ms;this.practice=practice;
        }
    }
    public final List<Entry> records;
    public final int challengeWins,challengeEnded,challengeLost,challengeAbandoned,practiceWins,practiceEnded,noHintWins,perfectWins,dailyWins,longestStreak,activeDays;
    public final long totalTime;
    public final int[] levelWins=new int[4];
    public final long[] levelBest={-1,-1,-1,-1},levelTotal=new long[4];
    public ProgressStats(List<Entry> input,Map<String,Long> days,LocalDate today){
        List<Entry> ordered=new ArrayList<>(input);ordered.sort((a,b)->{int d=b.date.compareTo(a.date);return d!=0?d:b.id.compareTo(a.id);});records=Collections.unmodifiableList(ordered);
        int wins=0,ended=0,lost=0,abandoned=0,practice=0,practiceEnds=0,independent=0,perfect=0;Set<String> daily=new HashSet<>();
        for(Entry r:records){
            boolean won=r.status.equals("won"),terminal=won||r.status.equals("lost")||r.status.equals("abandoned");
            if(r.practice){if(won)practice++;if(terminal)practiceEnds++;continue;}
            if(terminal)ended++;
            if(r.status.equals("lost"))lost++;
            if(r.status.equals("abandoned"))abandoned++;
            if(!won)continue;
            wins++;if(r.hints==0)independent++;if(r.hints==0&&r.mistakes==0)perfect++;
            if(!r.daily.isEmpty())daily.add(r.daily);
            if(r.level>=0&&r.level<4){levelWins[r.level]++;levelTotal[r.level]+=r.ms;levelBest[r.level]=levelBest[r.level]<0?r.ms:Math.min(levelBest[r.level],r.ms);}
        }
        challengeWins=wins;challengeEnded=ended;challengeLost=lost;challengeAbandoned=abandoned;practiceWins=practice;practiceEnded=practiceEnds;noHintWins=independent;perfectWins=perfect;dailyWins=daily.size();
        TreeSet<LocalDate> active=new TreeSet<>();long time=0;
        for(Map.Entry<String,Long> d:days.entrySet()){
            long ms=Math.max(0,d.getValue());time+=ms;
            try{LocalDate date=LocalDate.parse(d.getKey());if(ms>=1000&&!date.isAfter(today))active.add(date);}catch(RuntimeException ignored){}
        }
        int longest=0,run=0;LocalDate previous=null;
        for(LocalDate d:active){run=previous!=null&&previous.plusDays(1).equals(d)?run+1:1;longest=Math.max(longest,run);previous=d;}
        totalTime=time;longestStreak=longest;activeDays=active.size();
    }
    public int completedDifficulties(){int n=0;for(int count:levelWins)if(count>0)n++;return n;}
    public long best(){long best=-1;for(long v:levelBest)if(v>=0)best=best<0?v:Math.min(best,v);return best;}
}
